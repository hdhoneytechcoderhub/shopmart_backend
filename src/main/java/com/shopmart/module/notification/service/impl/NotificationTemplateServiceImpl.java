package com.shopmart.module.notification.service.impl;

import com.shopmart.common.dto.PageResponse;
import com.shopmart.common.exception.BadRequestException;
import com.shopmart.common.exception.ConflictException;
import com.shopmart.common.exception.ResourceNotFoundException;
import com.shopmart.module.notification.dto.NotificationTemplateRequest;
import com.shopmart.module.notification.dto.NotificationTemplateResponse;
import com.shopmart.module.notification.entity.NotificationChannel;
import com.shopmart.module.notification.entity.NotificationTemplate;
import com.shopmart.module.notification.repository.NotificationTemplateRepository;
import com.shopmart.module.notification.service.NotificationTemplateService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class NotificationTemplateServiceImpl implements NotificationTemplateService {

    private final NotificationTemplateRepository repository;

    @Override
    @Transactional
    public NotificationTemplateResponse create(NotificationTemplateRequest request) {
        if (repository.existsByCode(request.code())) {
            throw new ConflictException("A template with code '" + request.code() + "' already exists");
        }
        if (request.channel() == NotificationChannel.EMAIL
                && (request.subject() == null || request.subject().isBlank())) {
            throw new BadRequestException("Email templates require a subject");
        }
        NotificationTemplate t = new NotificationTemplate();
        apply(t, request);
        return toResponse(repository.save(t));
    }

    @Override
    @Transactional
    public NotificationTemplateResponse update(Long id, NotificationTemplateRequest request) {
        NotificationTemplate t = find(id);
        if (!t.getCode().equals(request.code()) && repository.existsByCode(request.code())) {
            throw new ConflictException("A template with code '" + request.code() + "' already exists");
        }
        apply(t, request);
        return toResponse(repository.save(t));
    }

    @Override
    @Transactional
    public void delete(Long id) {
        repository.delete(find(id));
    }

    @Override
    @Transactional(readOnly = true)
    public NotificationTemplateResponse get(Long id) {
        return toResponse(find(id));
    }

    @Override
    @Transactional(readOnly = true)
    public NotificationTemplateResponse getByCode(String code) {
        return repository.findByCode(code).map(this::toResponse)
                .orElseThrow(() -> new ResourceNotFoundException("NotificationTemplate", "code", code));
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<NotificationTemplateResponse> list(NotificationChannel channel, Pageable pageable) {
        Page<NotificationTemplate> page = channel != null
                ? repository.findByChannel(channel, pageable)
                : repository.findAll(pageable);
        return PageResponse.from(page.map(this::toResponse));
    }

    private void apply(NotificationTemplate t, NotificationTemplateRequest request) {
        t.setCode(request.code());
        t.setChannel(request.channel());
        t.setSubject(request.subject());
        t.setBody(request.body());
        t.setDescription(request.description());
        t.setActive(request.active() == null || request.active());
    }

    private NotificationTemplate find(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("NotificationTemplate", "id", id));
    }

    private NotificationTemplateResponse toResponse(NotificationTemplate t) {
        return new NotificationTemplateResponse(t.getId(), t.getCode(), t.getChannel(), t.getSubject(),
                t.getBody(), t.getDescription(), t.isActive(), t.getCreatedAt(), t.getUpdatedAt());
    }
}
