package com.shopmart.module.settings.service.impl;

import com.shopmart.common.exception.ResourceNotFoundException;
import com.shopmart.module.settings.dto.SettingRequest;
import com.shopmart.module.settings.dto.SettingResponse;
import com.shopmart.module.settings.entity.Setting;
import com.shopmart.module.settings.repository.SettingRepository;
import com.shopmart.module.settings.service.SettingService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class SettingServiceImpl implements SettingService {

    private final SettingRepository repository;

    @Override @Transactional(readOnly = true)
    public List<SettingResponse> listAll() {
        return repository.findAll().stream().map(this::toResponse).toList();
    }

    @Override @Transactional(readOnly = true)
    public List<SettingResponse> listPublic() {
        return repository.findByIsPublicTrue().stream().map(this::toResponse).toList();
    }

    @Override @Transactional
    public SettingResponse upsert(SettingRequest req) {
        Setting s = repository.findById(req.key()).orElseGet(Setting::new);
        s.setKey(req.key());
        s.setValue(req.value());
        if (req.isPublic() != null) s.setPublic(req.isPublic());
        return toResponse(repository.save(s));
    }

    @Override @Transactional
    public void delete(String key) {
        if (!repository.existsById(key))
            throw new ResourceNotFoundException("Setting not found: " + key);
        repository.deleteById(key);
    }

    private SettingResponse toResponse(Setting s) {
        return new SettingResponse(s.getKey(), s.getValue(), s.isPublic());
    }
}
