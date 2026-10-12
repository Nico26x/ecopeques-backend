package com.ecopeques.ecopeques_backend.controller;

import com.ecopeques.ecopeques_backend.dto.AsignarMisionRequest;
import com.ecopeques.ecopeques_backend.dto.MisionAsignadaResponse;
import com.ecopeques.ecopeques_backend.service.NinoMisionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/misiones")
@RequiredArgsConstructor
public class NinoMisionController {

    private final NinoMisionService ninoMisionService;

    @PostMapping("/asignar")
    public ResponseEntity<MisionAsignadaResponse> asignarMision(
            @Valid @RequestBody AsignarMisionRequest request,
            Authentication authentication
    ) {
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ninoMisionService.asignarMision(request, authentication.getName()));
    }
}
