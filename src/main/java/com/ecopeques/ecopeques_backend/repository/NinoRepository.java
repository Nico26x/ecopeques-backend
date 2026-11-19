package com.ecopeques.ecopeques_backend.repository;

import com.ecopeques.ecopeques_backend.model.Nino;
import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface NinoRepository extends JpaRepository<Nino, Long> {

    List<Nino> findByTutorIdOrderByIdAsc(Long tutorId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select n from Nino n where n.id = :id")
    Optional<Nino> findByIdForUpdate(@Param("id") Long id);
}
