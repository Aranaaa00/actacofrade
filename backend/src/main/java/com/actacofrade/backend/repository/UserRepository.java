package com.actacofrade.backend.repository;

import com.actacofrade.backend.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Integer> {

    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);

    List<User> findByHermandadId(Integer hermandadId);

    Optional<User> findByIdAndHermandadId(Integer id, Integer hermandadId);
}
