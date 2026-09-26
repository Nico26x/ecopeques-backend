package com.ecopeques.ecopeques_backend.usuario.dto;

import com.ecopeques.ecopeques_backend.usuario.domain.Rol;

public record AuthResponse(
        Long id,
        String nombre,
        String apellido,
        String email,
        Rol rol
) {
}
