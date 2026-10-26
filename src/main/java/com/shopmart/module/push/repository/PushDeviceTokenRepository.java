package com.shopmart.module.push.repository;

import com.shopmart.module.push.entity.DeviceToken;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PushDeviceTokenRepository extends JpaRepository<DeviceToken, Long> {
    List<DeviceToken> findByUserIdAndActiveTrue(Long userId);
    Optional<DeviceToken> findByToken(String token);
    void deleteByToken(String token);
    long countByUserIdAndActiveTrue(Long userId);
}
