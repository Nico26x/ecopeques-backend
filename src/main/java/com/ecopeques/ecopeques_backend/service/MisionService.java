package com.ecopeques.ecopeques_backend.service;

import com.ecopeques.ecopeques_backend.dto.MisionResponse;
import com.ecopeques.ecopeques_backend.model.CategoriaMision;
import com.ecopeques.ecopeques_backend.model.Mision;
import com.ecopeques.ecopeques_backend.repository.MisionRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class MisionService {

    private final MisionRepository misionRepository;

    @Transactional(readOnly = true)
    public List<MisionResponse> obtenerMisionesActivas(CategoriaMision categoria) {
        List<Mision> misiones = categoria == null
                ? misionRepository.findByActivaTrueOrderByIdAsc()
                : misionRepository.findByCategoriaAndActivaTrueOrderByIdAsc(categoria);

        return misiones.stream()
                .map(this::toResponse)
                .toList();
    }

    private MisionResponse toResponse(Mision mision) {
        return new MisionResponse(
                mision.getId(),
                mision.getTitulo(),
                mision.getDescripcion(),
                mision.getCategoria(),
                mision.getSemillasRecompensa()
        );
    }
}
