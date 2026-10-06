package com.ecopeques.ecopeques_backend.service;

import com.ecopeques.ecopeques_backend.dto.NinoRequest;
import com.ecopeques.ecopeques_backend.dto.NinoResponse;
import com.ecopeques.ecopeques_backend.model.Nino;
import com.ecopeques.ecopeques_backend.model.Usuario;
import com.ecopeques.ecopeques_backend.repository.NinoRepository;
import com.ecopeques.ecopeques_backend.repository.UsuarioRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class NinoService {

    private final NinoRepository ninoRepository;
    private final UsuarioRepository usuarioRepository;

    @Transactional
    public NinoResponse crearNino(NinoRequest request, String emailTutor) {
        Usuario tutor = buscarTutorPorEmail(emailTutor);

        Nino nino = Nino.builder()
                .nombre(request.nombre().trim())
                .avatar(limpiarTextoOpcional(request.avatar()))
                .grupo(limpiarTextoOpcional(request.grupo()))
                .totalSemillas(0)
                .tutor(tutor)
                .build();

        return toResponse(ninoRepository.save(nino));
    }

    @Transactional(readOnly = true)
    public List<NinoResponse> obtenerNinosPorTutor(Long tutorId, String emailTutorAutenticado) {
        Usuario tutorAutenticado = buscarTutorPorEmail(emailTutorAutenticado);

        if (!tutorAutenticado.getId().equals(tutorId)) {
            throw new AccessDeniedException("No tienes permisos para acceder a este recurso");
        }

        return ninoRepository.findByTutorIdOrderByIdAsc(tutorId).stream()
                .map(this::toResponse)
                .toList();
    }

    private Usuario buscarTutorPorEmail(String email) {
        String emailNormalizado = email.trim().toLowerCase();
        return usuarioRepository.findByEmail(emailNormalizado)
                .orElseThrow(() -> new UsernameNotFoundException("Usuario no encontrado"));
    }

    private String limpiarTextoOpcional(String valor) {
        if (valor == null || valor.isBlank()) {
            return null;
        }
        return valor.trim();
    }

    private NinoResponse toResponse(Nino nino) {
        return new NinoResponse(
                nino.getId(),
                nino.getNombre(),
                nino.getTotalSemillas(),
                nino.getAvatar(),
                nino.getGrupo(),
                nino.getTutor().getId()
        );
    }
}
