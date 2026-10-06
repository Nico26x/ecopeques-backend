package com.ecopeques.ecopeques_backend.repository;

import com.ecopeques.ecopeques_backend.model.CategoriaMision;
import com.ecopeques.ecopeques_backend.model.Mision;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface MisionRepository extends JpaRepository<Mision, Long> {

    List<Mision> findByActivaTrueOrderByIdAsc();

    List<Mision> findByCategoriaAndActivaTrueOrderByIdAsc(CategoriaMision categoria);
}
