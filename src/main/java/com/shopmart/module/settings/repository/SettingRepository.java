package com.shopmart.module.settings.repository;

import com.shopmart.module.settings.entity.Setting;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SettingRepository extends JpaRepository<Setting, String> {
    List<Setting> findByIsPublicTrue();
}
