package com.ecopeques.ecopeques_backend.dto;

import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;

public record AsignarMisionRequest(
        @NotNull(message = "El id del niño es obligatorio")
        Long ninoId,

        @NotNull(message = "El id de la misión es obligatorio")
        Long misionId,

        LocalDate fechaLimite
) {
}
