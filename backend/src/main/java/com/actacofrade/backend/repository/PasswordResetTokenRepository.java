package com.actacofrade.backend.repository;

import com.actacofrade.backend.entity.PasswordResetToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Optional;

public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken, Integer> {

    Optional<PasswordResetToken> findByTokenHash(String tokenHash);

    @Modifying
    @Query("UPDATE PasswordResetToken t SET t.consumedAt = :now " +
           "WHERE t.user.id = :userId AND t.consumedAt IS NULL")
    void invalidateActiveForUser(@Param("userId") Integer userId, @Param("now") LocalDateTime now);
}
