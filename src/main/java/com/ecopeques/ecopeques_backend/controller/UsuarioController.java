package com.ecopeques.ecopeques_backend.controller;

import com.ecopeques.ecopeques_backend.dto.AuthResponse;
import com.ecopeques.ecopeques_backend.dto.LoginRequest;
import com.ecopeques.ecopeques_backend.dto.RegisterRequest;
import com.ecopeques.ecopeques_backend.service.UsuarioService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class UsuarioController {

    private final UsuarioService usuarioService;

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> registrar(@Valid @RequestBody RegisterRequest request) {
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(usuarioService.registrar(request));
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(usuarioService.login(request));
    }
}
