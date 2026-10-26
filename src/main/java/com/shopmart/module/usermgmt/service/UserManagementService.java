package com.shopmart.module.usermgmt.service;

import com.shopmart.common.dto.PageResponse;
import com.shopmart.module.superadmin.dto.AdminCreatedResponse;
import com.shopmart.module.superadmin.dto.AdminResponse;
import com.shopmart.module.superadmin.dto.CreateAdminRequest;
import com.shopmart.module.user.entity.Role;
import org.springframework.data.domain.Pageable;

public interface UserManagementService {
    AdminCreatedResponse createUser(CreateAdminRequest req, Role role);
    PageResponse<AdminResponse> listByRole(Role role, Pageable pageable);
    AdminResponse getUser(Long id);
    AdminResponse setEnabled(Long id, boolean enabled);
    void softDelete(Long id);
}
