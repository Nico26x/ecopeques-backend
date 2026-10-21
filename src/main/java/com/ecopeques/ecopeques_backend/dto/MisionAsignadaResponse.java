package com.ecopeques.ecopeques_backend.dto;

import com.ecopeques.ecopeques_backend.model.CategoriaMision;
import com.ecopeques.ecopeques_backend.model.EstadoMisionAsignada;
import java.time.LocalDate;
import java.time.LocalDateTime;

public record MisionAsignadaResponse(
        Long id,
        Long ninoId,
        Long misionId,
        String tituloMision,
        CategoriaMision categoria,
        Integer semillasRecompensa,
        EstadoMisionAsignada estado,
        LocalDateTime fechaAsignacion,
        LocalDateTime fechaCompletado,
        LocalDate fechaLimite
) {
}
