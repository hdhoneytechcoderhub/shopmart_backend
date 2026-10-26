package com.shopmart.module.user.service;

import com.shopmart.module.user.dto.AddressRequest;
import com.shopmart.module.user.dto.AddressResponse;

import java.util.List;

public interface AddressService {
    List<AddressResponse> list(Long userId);
    AddressResponse create(Long userId, AddressRequest req);
    AddressResponse update(Long userId, Long id, AddressRequest req);
    void delete(Long userId, Long id);
}
