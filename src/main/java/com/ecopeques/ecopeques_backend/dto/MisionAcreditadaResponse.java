package com.ecopeques.ecopeques_backend.dto;

public record MisionAcreditadaResponse(
        Long id,
        Long ninoId,
        Long misionId,
        Integer semillasAcreditadas,
        Integer totalSemillas,
        Boolean acreditada
) {
}
