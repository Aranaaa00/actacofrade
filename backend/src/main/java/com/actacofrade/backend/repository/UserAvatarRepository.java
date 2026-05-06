package com.actacofrade.backend.repository;

import com.actacofrade.backend.entity.UserAvatar;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserAvatarRepository extends JpaRepository<UserAvatar, Integer> {

    Optional<UserAvatar> findByUserId(Integer userId);

    boolean existsByUserId(Integer userId);

    void deleteByUserId(Integer userId);

    @org.springframework.data.jpa.repository.Modifying
    @org.springframework.data.jpa.repository.Query("DELETE FROM UserAvatar a WHERE a.user.hermandad.id = :hermandadId")
    int deleteByUserHermandadId(@org.springframework.data.repository.query.Param("hermandadId") Integer hermandadId);
}
