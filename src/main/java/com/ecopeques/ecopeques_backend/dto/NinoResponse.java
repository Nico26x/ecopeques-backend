package com.ecopeques.ecopeques_backend.dto;

public record NinoResponse(
        Long id,
        String nombre,
        Integer totalSemillas,
        String avatar,
        String grupo,
        Long tutorId
) {
}
