package com.ecopeques.ecopeques_backend.dto;

import com.ecopeques.ecopeques_backend.model.Rol;

public record AuthResponse(
        Long id,
        String nombre,
        String apellido,
        String email,
        Rol rol
) {
}
