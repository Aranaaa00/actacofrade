package com.actacofrade.backend.repository;

import com.actacofrade.backend.entity.Hermandad;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface HermandadRepository extends JpaRepository<Hermandad, Integer> {

    boolean existsByNombreIgnoreCase(String nombre);

    Optional<Hermandad> findByNombreIgnoreCase(String nombre);
}
