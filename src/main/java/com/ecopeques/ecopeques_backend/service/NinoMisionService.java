package com.ecopeques.ecopeques_backend.service;

import com.ecopeques.ecopeques_backend.dto.AsignarMisionRequest;
import com.ecopeques.ecopeques_backend.dto.MisionAsignadaResponse;
import com.ecopeques.ecopeques_backend.exception.LimiteMisionesAlcanzadoException;
import com.ecopeques.ecopeques_backend.exception.MisionNotFoundException;
import com.ecopeques.ecopeques_backend.exception.MisionYaAsignadaException;
import com.ecopeques.ecopeques_backend.exception.NinoNotFoundException;
import com.ecopeques.ecopeques_backend.model.EstadoMisionAsignada;
import com.ecopeques.ecopeques_backend.model.Mision;
import com.ecopeques.ecopeques_backend.model.Nino;
import com.ecopeques.ecopeques_backend.model.NinoMision;
import com.ecopeques.ecopeques_backend.model.Usuario;
import com.ecopeques.ecopeques_backend.repository.MisionRepository;
import com.ecopeques.ecopeques_backend.repository.NinoMisionRepository;
import com.ecopeques.ecopeques_backend.repository.NinoRepository;
import com.ecopeques.ecopeques_backend.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class NinoMisionService {

    private static final int LIMITE_MISIONES_EN_PROGRESO = 3;

    private final NinoRepository ninoRepository;
    private final MisionRepository misionRepository;
    private final UsuarioRepository usuarioRepository;
    private final NinoMisionRepository ninoMisionRepository;

    @Transactional
    public MisionAsignadaResponse asignarMision(AsignarMisionRequest request, String emailTutorAutenticado) {
        Usuario tutor = buscarTutorPorEmail(emailTutorAutenticado);
        Nino nino = ninoRepository.findById(request.ninoId())
                .orElseThrow(() -> new NinoNotFoundException("Niño no encontrado"));

        if (!nino.getTutor().getId().equals(tutor.getId())) {
            throw new AccessDeniedException("No tienes permisos para asignar misiones a este niño");
        }

        Mision mision = misionRepository.findById(request.misionId())
                .orElseThrow(() -> new MisionNotFoundException("Misión no encontrada"));

        if (!Boolean.TRUE.equals(mision.getActiva())) {
            throw new MisionNotFoundException("Misión no encontrada o inactiva");
        }

        boolean yaAsignada = ninoMisionRepository.existsByNinoIdAndMisionIdAndEstado(
                nino.getId(),
                mision.getId(),
                EstadoMisionAsignada.EN_PROGRESO
        );

        if (yaAsignada) {
            throw new MisionYaAsignadaException("La misión ya está asignada al niño en estado EN_PROGRESO");
        }

        long misionesEnProgreso = ninoMisionRepository.countByNinoIdAndEstado(
                nino.getId(),
                EstadoMisionAsignada.EN_PROGRESO
        );

        if (misionesEnProgreso >= LIMITE_MISIONES_EN_PROGRESO) {
            throw new LimiteMisionesAlcanzadoException("El niño ya tiene 3 misiones activas en progreso");
        }

        NinoMision asignacion = NinoMision.builder()
                .nino(nino)
                .mision(mision)
                .estado(EstadoMisionAsignada.EN_PROGRESO)
                .fechaLimite(request.fechaLimite())
                .build();

        return toResponse(ninoMisionRepository.save(asignacion));
    }

    private Usuario buscarTutorPorEmail(String email) {
        String emailNormalizado = email.trim().toLowerCase();
        return usuarioRepository.findByEmail(emailNormalizado)
                .orElseThrow(() -> new UsernameNotFoundException("Usuario no encontrado"));
    }

    private MisionAsignadaResponse toResponse(NinoMision asignacion) {
        Mision mision = asignacion.getMision();
        return new MisionAsignadaResponse(
                asignacion.getId(),
                asignacion.getNino().getId(),
                mision.getId(),
                mision.getTitulo(),
                mision.getCategoria(),
                mision.getSemillasRecompensa(),
                asignacion.getEstado(),
                asignacion.getFechaAsignacion(),
                asignacion.getFechaCompletado(),
                asignacion.getFechaLimite()
        );
    }
}
