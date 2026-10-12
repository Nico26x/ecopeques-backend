package com.ecopeques.ecopeques_backend.repository;

import com.ecopeques.ecopeques_backend.model.EstadoMisionAsignada;
import com.ecopeques.ecopeques_backend.model.NinoMision;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface NinoMisionRepository extends JpaRepository<NinoMision, Long> {

    long countByNinoIdAndEstado(Long ninoId, EstadoMisionAsignada estado);

    boolean existsByNinoIdAndMisionIdAndEstado(Long ninoId, Long misionId, EstadoMisionAsignada estado);
}
