package com.shopmart.module.auth.repository;

import com.shopmart.module.auth.entity.RefreshToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {
    Optional<RefreshToken> findByTokenAndRevokedFalse(String token);

    Optional<RefreshToken> findByIdAndUserId(Long id, Long userId);

    java.util.List<RefreshToken> findByUserIdAndRevokedFalseOrderByLastUsedAtDesc(Long userId);
    Optional<RefreshToken> findTopByUserIdOrderByLastUsedAtDesc(Long userId);
    long countByUserIdAndRevokedFalse(Long userId);
    /** All sessions ever created for this user (including revoked), newest first — used to
     *  reconstruct login history for the customer timeline. Each row = one login event. */
    java.util.List<RefreshToken> findByUserIdOrderByCreatedAtDesc(Long userId);

    @Modifying
    @Transactional
    @Query("update RefreshToken r set r.revoked = true where r.userId = :userId")
    void revokeAllForUser(Long userId);
}
