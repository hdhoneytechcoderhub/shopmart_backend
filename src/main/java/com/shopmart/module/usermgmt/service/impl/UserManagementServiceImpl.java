package com.shopmart.module.usermgmt.service.impl;

import com.shopmart.common.dto.PageResponse;
import com.shopmart.common.exception.ConflictException;
import com.shopmart.common.exception.ResourceNotFoundException;
import com.shopmart.module.audit.service.AuditService;
import com.shopmart.module.superadmin.dto.AdminCreatedResponse;
import com.shopmart.module.superadmin.dto.AdminResponse;
import com.shopmart.module.superadmin.dto.CreateAdminRequest;
import com.shopmart.module.user.entity.Role;
import com.shopmart.module.user.entity.User;
import com.shopmart.module.user.repository.UserRepository;
import com.shopmart.module.usermgmt.service.UserManagementService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserManagementServiceImpl implements UserManagementService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuditService auditService;

    @Override
    @Transactional
    public AdminCreatedResponse createUser(CreateAdminRequest req, Role role) {
        if (userRepository.existsByEmail(req.email()))
            throw new ConflictException("A user with this email already exists");
        User u = new User();
        u.setName(req.name());
        u.setEmail(req.email());
        u.setPhone(req.phone());
        u.setPasswordHash(passwordEncoder.encode(req.password()));
        u.setEmailVerified(true);
        u.setEnabled(true);
        u.addRole(role);
        u.addRole(Role.ROLE_CUSTOMER);
        u = userRepository.save(u);
        auditService.log(null, role.name() + "_CREATED", "User", u.getId(), u.getEmail());
        return new AdminCreatedResponse(u.getId(), u.getName(), u.getEmail(),
                u.getRoles().stream().map(Enum::name).collect(Collectors.toSet()));
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<AdminResponse> listByRole(Role role, Pageable pageable) {
        Page<User> page = userRepository.findByRole(role, pageable);
        return PageResponse.from(page.map(this::toResponse));
    }

    @Override
    @Transactional(readOnly = true)
    public AdminResponse getUser(Long id) {
        return toResponse(find(id));
    }

    @Override
    @Transactional
    public AdminResponse setEnabled(Long id, boolean enabled) {
        User u = find(id);
        u.setEnabled(enabled);
        u = userRepository.save(u);
        auditService.log(null, enabled ? "USER_ENABLED" : "USER_DISABLED", "User", u.getId(), u.getEmail());
        return toResponse(u);
    }

    @Override
    @Transactional
    public void softDelete(Long id) {
        User u = find(id);
        u.setEnabled(false);
        userRepository.save(u);
        auditService.log(null, "USER_DELETED", "User", u.getId(), u.getEmail());
    }

    private User find(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + id));
    }

    private AdminResponse toResponse(User u) {
        return new AdminResponse(u.getId(), u.getName(), u.getEmail(), u.getPhone(),
                u.isEnabled(), u.getRoles().stream().map(Enum::name).collect(Collectors.toSet()),
                u.getCreatedAt());
    }
}
