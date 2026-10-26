package com.shopmart.module.notification.service.impl;

import com.shopmart.common.dto.PageResponse;
import com.shopmart.common.exception.BadRequestException;
import com.shopmart.common.exception.ResourceNotFoundException;
import com.shopmart.common.notification.EmailSender;
import com.shopmart.common.notification.PushSender;
import com.shopmart.common.notification.SmsSender;
import com.shopmart.module.customer.dto.CustomerSegment;
import com.shopmart.module.customer.service.CustomerIntelligenceService;
import com.shopmart.module.notification.dto.CampaignRequest;
import com.shopmart.module.notification.dto.CampaignResponse;
import com.shopmart.module.notification.dto.CampaignStatsResponse;
import com.shopmart.module.notification.entity.*;
import com.shopmart.module.notification.repository.NotificationCampaignRepository;
import com.shopmart.module.notification.repository.NotificationTemplateRepository;
import com.shopmart.module.notification.service.NotificationCampaignService;
import com.shopmart.module.user.entity.Role;
import com.shopmart.module.user.entity.User;
import com.shopmart.module.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationCampaignServiceImpl implements NotificationCampaignService {

    private final NotificationCampaignRepository repository;
    private final NotificationTemplateRepository templateRepository;
    private final UserRepository userRepository;
    private final CustomerIntelligenceService customerIntelligenceService;
    private final com.shopmart.module.notification.service.UserNotificationService userNotificationService;
    private final EmailSender emailSender;
    private final SmsSender smsSender;
    private final PushSender pushSender;
    private final com.shopmart.config.MetricsConfig.AppMetrics appMetrics;

    @Override
    @Transactional
    public CampaignResponse create(CampaignRequest request, Long adminId) {
        validateTarget(request);
        NotificationCampaign c = new NotificationCampaign();
        apply(c, request);
        c.setCreatedBy(adminId);
        c.setStatus(request.scheduledAt() != null ? CampaignStatus.SCHEDULED : CampaignStatus.DRAFT);
        return toResponse(repository.save(c));
    }

    @Override
    @Transactional
    public CampaignResponse update(Long id, CampaignRequest request) {
        NotificationCampaign c = find(id);
        if (c.getStatus() == CampaignStatus.SENT || c.getStatus() == CampaignStatus.SENDING) {
            throw new BadRequestException("Cannot edit a campaign that has already been sent");
        }
        validateTarget(request);
        apply(c, request);
        c.setStatus(request.scheduledAt() != null ? CampaignStatus.SCHEDULED : CampaignStatus.DRAFT);
        return toResponse(repository.save(c));
    }

    @Override
    @Transactional
    public void delete(Long id) {
        NotificationCampaign c = find(id);
        if (c.getStatus() == CampaignStatus.SENDING) {
            throw new BadRequestException("Cannot delete a campaign while it is sending");
        }
        repository.delete(c);
    }

    @Override
    @Transactional(readOnly = true)
    public CampaignResponse get(Long id) {
        return toResponse(find(id));
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<CampaignResponse> list(CampaignStatus status, Pageable pageable) {
        Page<NotificationCampaign> page = status != null
                ? repository.findByStatus(status, pageable)
                : repository.findAll(pageable);
        return PageResponse.from(page.map(this::toResponse));
    }

    @Override
    @Transactional
    public CampaignResponse sendNow(Long id) {
        NotificationCampaign c = find(id);
        if (c.getStatus() == CampaignStatus.SENT || c.getStatus() == CampaignStatus.SENDING) {
            throw new BadRequestException("Campaign has already been sent");
        }
        send(c);
        return toResponse(c);
    }

    @Override
    @Transactional
    public CampaignResponse cancel(Long id) {
        NotificationCampaign c = find(id);
        if (c.getStatus() == CampaignStatus.SENT) {
            throw new BadRequestException("Cannot cancel a campaign that has already been sent");
        }
        c.setStatus(CampaignStatus.CANCELLED);
        return toResponse(repository.save(c));
    }

    @Override
    @Transactional(readOnly = true)
    public CampaignStatsResponse stats() {
        List<NotificationCampaign> all = repository.findAll();
        long draft = all.stream().filter(c -> c.getStatus() == CampaignStatus.DRAFT).count();
        long scheduled = all.stream().filter(c -> c.getStatus() == CampaignStatus.SCHEDULED).count();
        long sent = all.stream().filter(c -> c.getStatus() == CampaignStatus.SENT).count();
        long cancelled = all.stream().filter(c -> c.getStatus() == CampaignStatus.CANCELLED).count();
        long failed = all.stream().filter(c -> c.getStatus() == CampaignStatus.FAILED).count();
        long totalReached = all.stream().mapToLong(NotificationCampaign::getRecipientCount).sum();
        return new CampaignStatsResponse(all.size(), draft, scheduled, sent, cancelled, failed, totalReached);
    }

    @Override
    @Transactional
    public void processDueCampaigns() {
        List<NotificationCampaign> due = repository.findByStatusAndScheduledAtLessThanEqual(
                CampaignStatus.SCHEDULED, Instant.now());
        for (NotificationCampaign c : due) {
            try {
                send(c);
            } catch (Exception e) {
                log.error("[CAMPAIGN] failed to send scheduled campaign {}: {}", c.getId(), e.getMessage());
                c.setStatus(CampaignStatus.FAILED);
                c.setFailureReason(e.getMessage());
                repository.save(c);
            }
        }
    }

    // ---------------- internal ----------------

    private void send(NotificationCampaign c) {
        c.setStatus(CampaignStatus.SENDING);
        repository.save(c);

        String title = c.getTitle();
        String message = c.getMessage();
        if (c.getTemplateId() != null) {
            NotificationTemplate t = templateRepository.findById(c.getTemplateId()).orElse(null);
            if (t != null && t.isActive()) {
                title = t.getSubject() != null ? t.getSubject() : title;
                message = t.getBody();
            }
        }

        List<Long> recipientIds = resolveRecipients(c);
        String finalTitle = title;
        String finalMessage = message;
        int delivered = 0;
        for (Long userId : recipientIds) {
            try {
                switch (c.getChannel()) {
                    case IN_APP -> userNotificationService.notify(userId, NotificationType.PROMO, finalTitle, finalMessage, c.getLink());
                    case EMAIL -> userRepository.findById(userId).ifPresent(u -> emailSender.send(u.getEmail(), finalTitle, finalMessage));
                    case SMS -> userRepository.findById(userId).ifPresent(u -> {
                        if (u.getPhone() != null) smsSender.send(u.getPhone(), finalMessage);
                    });
                    case PUSH -> pushSender.send(userId, finalTitle, finalMessage, c.getLink());
                }
                delivered++;
            } catch (Exception e) {
                log.warn("[CAMPAIGN] delivery failed campaign={} userId={}: {}", c.getId(), userId, e.getMessage());
                appMetrics.incrementNotificationFailure(c.getChannel().name());
            }
        }

        c.setRecipientCount(delivered);
        c.setStatus(CampaignStatus.SENT);
        c.setSentAt(Instant.now());
        repository.save(c);
        log.info("[CAMPAIGN] sent campaign={} channel={} recipients={}", c.getId(), c.getChannel(), delivered);
    }

    private List<Long> resolveRecipients(NotificationCampaign c) {
        return switch (c.getTargetType()) {
            case ALL -> userRepository.findAll().stream()
                    .filter(u -> u.getRoles().contains(Role.ROLE_CUSTOMER))
                    .map(User::getId).toList();
            case ROLE -> {
                Role role = Role.valueOf(c.getTargetRole());
                yield userRepository.findByRole(role, org.springframework.data.domain.Pageable.unpaged())
                        .stream().map(User::getId).toList();
            }
            case SEGMENT -> customerIntelligenceService.customerIdsInSegment(CustomerSegment.valueOf(c.getTargetSegment()));
        };
    }

    private void validateTarget(CampaignRequest request) {
        if (request.targetType() == CampaignTargetType.ROLE) {
            if (request.targetRole() == null || request.targetRole().isBlank()) {
                throw new BadRequestException("targetRole is required when targetType = ROLE");
            }
            try {
                Role.valueOf(request.targetRole());
            } catch (IllegalArgumentException e) {
                throw new BadRequestException("Unknown role: " + request.targetRole());
            }
        }
        if (request.targetType() == CampaignTargetType.SEGMENT) {
            if (request.targetSegment() == null || request.targetSegment().isBlank()) {
                throw new BadRequestException("targetSegment is required when targetType = SEGMENT");
            }
            try {
                CustomerSegment.valueOf(request.targetSegment());
            } catch (IllegalArgumentException e) {
                throw new BadRequestException("Unknown segment: " + request.targetSegment());
            }
        }
    }

    private void apply(NotificationCampaign c, CampaignRequest request) {
        c.setName(request.name());
        c.setChannel(request.channel() != null ? request.channel() : NotificationChannel.IN_APP);
        c.setTargetType(request.targetType());
        c.setTargetRole(request.targetRole());
        c.setTargetSegment(request.targetSegment());
        c.setTemplateId(request.templateId());
        c.setTitle(request.title());
        c.setMessage(request.message());
        c.setLink(request.link());
        c.setScheduledAt(request.scheduledAt());
    }

    private NotificationCampaign find(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("NotificationCampaign", "id", id));
    }

    private CampaignResponse toResponse(NotificationCampaign c) {
        return new CampaignResponse(c.getId(), c.getName(), c.getChannel(), c.getTargetType(),
                c.getTargetRole(), c.getTargetSegment(), c.getTitle(), c.getMessage(), c.getLink(),
                c.getStatus(), c.getScheduledAt(), c.getSentAt(), c.getRecipientCount(),
                c.getFailureReason(), c.getCreatedAt());
    }
}
