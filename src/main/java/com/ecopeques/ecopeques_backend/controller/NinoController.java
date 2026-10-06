package com.ecopeques.ecopeques_backend.controller;

import com.ecopeques.ecopeques_backend.dto.NinoRequest;
import com.ecopeques.ecopeques_backend.dto.NinoResponse;
import com.ecopeques.ecopeques_backend.service.NinoService;
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

@RestController
@RequestMapping("/api/ninos")
@RequiredArgsConstructor
public class NinoController {

    private final NinoService ninoService;

    @PostMapping
    public ResponseEntity<NinoResponse> crearNino(
            @Valid @RequestBody NinoRequest request,
            Authentication authentication
    ) {
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ninoService.crearNino(request, authentication.getName()));
    }

    @GetMapping("/tutor/{tutorId}")
    public ResponseEntity<List<NinoResponse>> obtenerPorTutor(
            @PathVariable Long tutorId,
            Authentication authentication
    ) {
        return ResponseEntity.ok(ninoService.obtenerNinosPorTutor(tutorId, authentication.getName()));
    }
}
