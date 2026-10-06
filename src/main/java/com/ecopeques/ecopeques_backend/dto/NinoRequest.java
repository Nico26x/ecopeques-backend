package com.ecopeques.ecopeques_backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record NinoRequest(
        @NotBlank(message = "El nombre del niño es obligatorio")
        @Size(max = 100, message = "El nombre no puede superar los 100 caracteres")
        String nombre,

        @Size(max = 255, message = "El avatar no puede superar los 255 caracteres")
        String avatar,

        @Size(max = 100, message = "El grupo no puede superar los 100 caracteres")
        String grupo
) {
}
