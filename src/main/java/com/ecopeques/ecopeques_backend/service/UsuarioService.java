package com.ecopeques.ecopeques_backend.service;

import com.ecopeques.ecopeques_backend.dto.AuthResponse;
import com.ecopeques.ecopeques_backend.dto.LoginRequest;
import com.ecopeques.ecopeques_backend.dto.RegisterRequest;
import com.ecopeques.ecopeques_backend.exception.EmailAlreadyExistsException;
import com.ecopeques.ecopeques_backend.model.Usuario;
import com.ecopeques.ecopeques_backend.repository.UsuarioRepository;
import com.ecopeques.ecopeques_backend.security.JwtUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UsuarioService {

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtUtils jwtUtils;

    @Transactional
    public AuthResponse registrar(RegisterRequest request) {
        String emailNormalizado = request.email().trim().toLowerCase();

        if (usuarioRepository.existsByEmail(emailNormalizado)) {
            throw new EmailAlreadyExistsException("El correo ya está registrado");
        }

        Usuario usuario = Usuario.builder()
                .nombre(request.nombre().trim())
                .apellido(request.apellido().trim())
                .email(emailNormalizado)
                .password(passwordEncoder.encode(request.password()))
                .rol(request.rol())
                .build();

        Usuario usuarioGuardado = usuarioRepository.save(usuario);
        String token = jwtUtils.generateToken(usuarioGuardado);

        return new AuthResponse(
                usuarioGuardado.getId(),
                usuarioGuardado.getNombre(),
                usuarioGuardado.getApellido(),
                usuarioGuardado.getEmail(),
                usuarioGuardado.getRol(),
                token
        );
    }

    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest request) {
        String emailNormalizado = request.email().trim().toLowerCase();

        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(emailNormalizado, request.password())
        );

        Usuario usuario = usuarioRepository.findByEmail(emailNormalizado)
                .orElseThrow(() -> new UsernameNotFoundException("Usuario no encontrado"));
        String token = jwtUtils.generateToken(usuario);

        return new AuthResponse(
                usuario.getId(),
                usuario.getNombre(),
                usuario.getApellido(),
                usuario.getEmail(),
                usuario.getRol(),
                token
        );
    }
}
