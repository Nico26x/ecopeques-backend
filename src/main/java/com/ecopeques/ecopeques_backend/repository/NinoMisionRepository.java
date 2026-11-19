package com.ecopeques.ecopeques_backend.repository;

import com.ecopeques.ecopeques_backend.model.EstadoMisionAsignada;
import com.ecopeques.ecopeques_backend.model.NinoMision;
import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface NinoMisionRepository extends JpaRepository<NinoMision, Long> {

    long countByNinoIdAndEstado(Long ninoId, EstadoMisionAsignada estado);

    boolean existsByNinoIdAndMisionIdAndEstado(Long ninoId, Long misionId, EstadoMisionAsignada estado);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select nm from NinoMision nm join fetch nm.nino n join fetch n.tutor join fetch nm.mision where nm.id = :id")
    Optional<NinoMision> findByIdForUpdate(@Param("id") Long id);

    List<NinoMision> findByNinoTutorIdAndEstadoOrderByFechaAsignacionDesc(
            Long tutorId,
            EstadoMisionAsignada estado
    );
}
