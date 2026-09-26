package com.ecopeques.ecopeques_backend.usuario.service;

import com.ecopeques.ecopeques_backend.shared.exception.EmailAlreadyExistsException;
import com.ecopeques.ecopeques_backend.usuario.domain.Usuario;
import com.ecopeques.ecopeques_backend.usuario.dto.AuthResponse;
import com.ecopeques.ecopeques_backend.usuario.dto.RegisterRequest;
import com.ecopeques.ecopeques_backend.usuario.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UsuarioService {

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;

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

        return new AuthResponse(
                usuarioGuardado.getId(),
                usuarioGuardado.getNombre(),
                usuarioGuardado.getApellido(),
                usuarioGuardado.getEmail(),
                usuarioGuardado.getRol()
        );
    }
}
