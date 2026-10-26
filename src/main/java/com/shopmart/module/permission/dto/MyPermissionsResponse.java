package com.shopmart.module.permission.dto;

import java.util.Set;

/**
 * Effective access for the current user: base roles plus every permission code granted via
 * dynamic custom roles, and the distinct screen keys those permissions unlock. The frontend
 * uses {@code screenKeys} to show/hide menu items and routes (screen-level permissions), and
 * {@code permissionCodes} to show/hide individual actions/buttons (API-level permissions).
 */
public record MyPermissionsResponse(
        Set<String> baseRoles,
        Set<String> permissionCodes,
        Set<String> screenKeys
) {}
