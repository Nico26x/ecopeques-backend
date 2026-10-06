package com.ecopeques.ecopeques_backend.repository;

import com.ecopeques.ecopeques_backend.model.Nino;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface NinoRepository extends JpaRepository<Nino, Long> {

    List<Nino> findByTutorIdOrderByIdAsc(Long tutorId);
}
