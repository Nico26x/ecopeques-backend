package com.ecopeques.ecopeques_backend.dto;

import com.ecopeques.ecopeques_backend.model.CategoriaMision;

public record MisionResponse(
        Long id,
        String titulo,
        String descripcion,
        CategoriaMision categoria,
        Integer semillasRecompensa
) {
}
