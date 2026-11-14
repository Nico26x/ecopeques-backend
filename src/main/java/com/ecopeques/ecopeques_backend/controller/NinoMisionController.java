package com.ecopeques.ecopeques_backend.controller;

import com.ecopeques.ecopeques_backend.dto.AsignarMisionRequest;
import com.ecopeques.ecopeques_backend.dto.ConfirmarMisionRequest;
import com.ecopeques.ecopeques_backend.dto.MisionAsignadaResponse;
import com.ecopeques.ecopeques_backend.service.NinoMisionService;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.PutMapping;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class NinoMisionController {

    private final NinoMisionService ninoMisionService;

    @PostMapping("/misiones/asignar")
    public ResponseEntity<MisionAsignadaResponse> asignarMision(
            @Valid @RequestBody AsignarMisionRequest request,
            Authentication authentication
    ) {
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ninoMisionService.asignarMision(request, authentication.getName()));
    }

    @PutMapping("/misiones-ninos/{id}/confirmar")
    public ResponseEntity<MisionAsignadaResponse> confirmarMision(
            @PathVariable Long id,
            @RequestBody(required = false) ConfirmarMisionRequest request,
            Authentication authentication
    ) {
        return ResponseEntity.ok(ninoMisionService.confirmarMision(id, request, authentication.getName()));
    }

    @GetMapping("/misiones-ninos/tutor/pendientes")
    public ResponseEntity<List<MisionAsignadaResponse>> listarPendientes(Authentication authentication) {
        return ResponseEntity.ok(ninoMisionService.listarPendientesPorTutor(authentication.getName()));
    }
}
