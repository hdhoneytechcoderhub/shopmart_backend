package com.shopmart.module.superadmin.service.impl;

import com.shopmart.common.exception.ConflictException;
import com.shopmart.module.order.repository.OrderRepository;
import com.shopmart.module.product.entity.ProductStatus;
import com.shopmart.module.product.repository.ProductRepository;
import com.shopmart.module.superadmin.dto.AdminCreatedResponse;
import com.shopmart.module.superadmin.dto.CreateAdminRequest;
import com.shopmart.module.superadmin.dto.SuperAdminDashboardResponse;
import com.shopmart.module.superadmin.service.SuperAdminService;
import com.shopmart.module.user.entity.Role;
import com.shopmart.module.user.entity.User;
import com.shopmart.module.user.repository.UserRepository;
import com.shopmart.module.superadmin.dto.AdminResponse;
import com.shopmart.module.superadmin.dto.UpdateAdminRequest;
import com.shopmart.common.dto.PageResponse;
import com.shopmart.common.exception.ResourceNotFoundException;
import com.shopmart.module.audit.service.AuditService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import java.time.Instant;

import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SuperAdminServiceImpl implements SuperAdminService {

    private final UserRepository userRepository;
    private final ProductRepository productRepository;
    private final OrderRepository orderRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuditService auditService;

    @Override
    @Transactional
    public AdminCreatedResponse createAdmin(CreateAdminRequest req) {
        if (userRepository.existsByEmail(req.email())) {
            throw new ConflictException("A user with this email already exists");
        }
        User admin = new User();
        admin.setName(req.name());
        admin.setEmail(req.email());
        admin.setPhone(req.phone());
        admin.setPasswordHash(passwordEncoder.encode(req.password()));
        admin.setEmailVerified(true);
        admin.setEnabled(true);
        admin.addRole(Role.ROLE_ADMIN);
        admin.addRole(Role.ROLE_CUSTOMER);
        admin = userRepository.save(admin);
        auditService.log(null, "ADMIN_CREATED", "User", admin.getId(), admin.getEmail());
        return new AdminCreatedResponse(
                admin.getId(), admin.getName(), admin.getEmail(),
                admin.getRoles().stream().map(Enum::name).collect(Collectors.toSet()));
    }

    @Override
    @Transactional(readOnly = true)
    public SuperAdminDashboardResponse dashboard() {
        BigDecimal revenue = orderRepository.totalRevenue();
        return new SuperAdminDashboardResponse(
                userRepository.countByRole(Role.ROLE_ADMIN),
                productRepository.count(),
                productRepository.countByStatus(ProductStatus.PENDING_APPROVAL),
                orderRepository.count(),
                revenue != null ? revenue : BigDecimal.ZERO,
                orderRepository.countDistinctCustomers()
        );
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<AdminResponse> listAdmins(Pageable pageable) {
        Page<com.shopmart.module.user.entity.User> page = userRepository.findByRole(Role.ROLE_ADMIN, pageable);
        return PageResponse.from(page.map(this::toAdminResponse));
    }

    @Override
    @Transactional(readOnly = true)
    public AdminResponse getAdmin(Long id) {
        return toAdminResponse(findAdmin(id));
    }

    @Override
    @Transactional
    public AdminResponse updateAdmin(Long id, UpdateAdminRequest req) {
        com.shopmart.module.user.entity.User u = findAdmin(id);
        if (req.name() != null) u.setName(req.name());
        if (req.phone() != null) u.setPhone(req.phone());
        if (req.enabled() != null) u.setEnabled(req.enabled());
        if (req.password() != null && !req.password().isBlank())
            u.setPasswordHash(passwordEncoder.encode(req.password()));
        u = userRepository.save(u);
        auditService.log(null, "ADMIN_UPDATED", "User", u.getId(), u.getEmail());
        return toAdminResponse(u);
    }

    @Override
    @Transactional
    public void deleteAdmin(Long id) {
        com.shopmart.module.user.entity.User u = findAdmin(id);
        // Soft delete: disable the account (keeps the record + history in the DB).
        u.setEnabled(false);
        userRepository.save(u);
        auditService.log(null, "ADMIN_DELETED", "User", u.getId(), u.getEmail());
    }

    private com.shopmart.module.user.entity.User findAdmin(Long id) {
        com.shopmart.module.user.entity.User u = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Admin not found: " + id));
        if (!u.getRoles().contains(Role.ROLE_ADMIN))
            throw new ResourceNotFoundException("User " + id + " is not an admin");
        return u;
    }

    private AdminResponse toAdminResponse(com.shopmart.module.user.entity.User u) {
        return new AdminResponse(u.getId(), u.getName(), u.getEmail(), u.getPhone(),
                u.isEnabled(), u.getRoles().stream().map(Enum::name).collect(java.util.stream.Collectors.toSet()),
                u.getCreatedAt());
    }
}
