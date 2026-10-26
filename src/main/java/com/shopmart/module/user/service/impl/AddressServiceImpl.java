package com.shopmart.module.user.service.impl;

import com.shopmart.common.exception.ResourceNotFoundException;
import com.shopmart.module.user.dto.AddressRequest;
import com.shopmart.module.user.dto.AddressResponse;
import com.shopmart.module.user.entity.Address;
import com.shopmart.module.user.entity.User;
import com.shopmart.module.user.repository.AddressRepository;
import com.shopmart.module.user.repository.UserRepository;
import com.shopmart.module.user.service.AddressService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AddressServiceImpl implements AddressService {

    private final AddressRepository addressRepository;
    private final UserRepository userRepository;

    @Override @Transactional(readOnly = true)
    public List<AddressResponse> list(Long userId) {
        return addressRepository.findByUserId(userId).stream().map(this::toResponse).toList();
    }

    @Override @Transactional
    public AddressResponse create(Long userId, AddressRequest req) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        Address a = new Address();
        a.setUser(user);
        apply(a, req);
        return toResponse(addressRepository.save(a));
    }

    @Override @Transactional
    public AddressResponse update(Long userId, Long id, AddressRequest req) {
        Address a = addressRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Address not found: " + id));
        apply(a, req);
        return toResponse(addressRepository.save(a));
    }

    @Override @Transactional
    public void delete(Long userId, Long id) {
        Address a = addressRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Address not found: " + id));
        addressRepository.delete(a);
    }

    private void apply(Address a, AddressRequest req) {
        a.setLabel(req.label());
        a.setFullName(req.fullName());
        a.setPhone(req.phone());
        a.setLine1(req.line1());
        a.setLine2(req.line2());
        a.setCity(req.city());
        a.setState(req.state());
        a.setPostalCode(req.postalCode());
        a.setCountry(req.country());
        if (req.isDefault() != null) a.setDefault(req.isDefault());
    }

    private AddressResponse toResponse(Address a) {
        return new AddressResponse(a.getId(), a.getLabel(), a.getFullName(), a.getPhone(),
                a.getLine1(), a.getLine2(), a.getCity(), a.getState(),
                a.getPostalCode(), a.getCountry(), a.isDefault());
    }
}
