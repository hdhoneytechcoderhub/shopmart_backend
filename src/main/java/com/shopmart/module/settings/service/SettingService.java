package com.shopmart.module.settings.service;

import com.shopmart.module.settings.dto.SettingRequest;
import com.shopmart.module.settings.dto.SettingResponse;

import java.util.List;

public interface SettingService {
    List<SettingResponse> listAll();
    List<SettingResponse> listPublic();
    SettingResponse upsert(SettingRequest req);
    void delete(String key);
}
