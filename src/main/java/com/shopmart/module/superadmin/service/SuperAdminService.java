package com.shopmart.module.superadmin.service;

import com.shopmart.module.superadmin.dto.AdminCreatedResponse;
import com.shopmart.module.superadmin.dto.CreateAdminRequest;
import com.shopmart.module.superadmin.dto.SuperAdminDashboardResponse;
import com.shopmart.module.superadmin.dto.AdminResponse;
import com.shopmart.module.superadmin.dto.UpdateAdminRequest;
import com.shopmart.common.dto.PageResponse;
import org.springframework.data.domain.Pageable;

public interface SuperAdminService {
    AdminCreatedResponse createAdmin(CreateAdminRequest request);
    SuperAdminDashboardResponse dashboard();

    // Admin management (full CRUD)
    PageResponse<AdminResponse> listAdmins(Pageable pageable);
    AdminResponse getAdmin(Long id);
    AdminResponse updateAdmin(Long id, UpdateAdminRequest req);
    void deleteAdmin(Long id);
}
