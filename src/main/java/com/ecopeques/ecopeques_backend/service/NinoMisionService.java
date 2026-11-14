package com.ecopeques.ecopeques_backend.service;

import com.ecopeques.ecopeques_backend.dto.AsignarMisionRequest;
import com.ecopeques.ecopeques_backend.dto.ConfirmarMisionRequest;
import com.ecopeques.ecopeques_backend.dto.MisionAcreditadaResponse;
import com.ecopeques.ecopeques_backend.dto.MisionAsignadaResponse;
import com.ecopeques.ecopeques_backend.exception.EvidenciaInvalidaException;
import com.ecopeques.ecopeques_backend.exception.LimiteMisionesAlcanzadoException;
import com.ecopeques.ecopeques_backend.exception.MisionNoConfirmableException;
import com.ecopeques.ecopeques_backend.exception.MisionNoAcreditableException;
import com.ecopeques.ecopeques_backend.exception.MisionNotFoundException;
import com.ecopeques.ecopeques_backend.exception.MisionYaAsignadaException;
import com.ecopeques.ecopeques_backend.exception.NinoMisionNotFoundException;
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
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class NinoMisionService {

    private static final int LIMITE_MISIONES_EN_PROGRESO = 3;
    private static final int LONGITUD_MAXIMA_EVIDENCIA = 2_800_000;

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

    @Transactional
    public MisionAsignadaResponse confirmarMision(
            Long id,
            ConfirmarMisionRequest request,
            String emailTutorAutenticado
    ) {
        Usuario tutor = buscarTutorPorEmail(emailTutorAutenticado);
        NinoMision asignacion = ninoMisionRepository.findByIdForUpdate(id)
                .orElseThrow(() -> new NinoMisionNotFoundException("La asignación de misión no existe"));

        if (!asignacion.getNino().getTutor().getId().equals(tutor.getId())) {
            throw new AccessDeniedException("No tienes permiso para confirmar esta misión");
        }

        if (asignacion.getEstado() != EstadoMisionAsignada.EN_PROGRESO) {
            throw new MisionNoConfirmableException(
                    "La misión asignada ya fue completada o no está en progreso"
            );
        }

        String evidenciaNormalizada = validarEvidencia(request);
        asignacion.setEstado(EstadoMisionAsignada.COMPLETADA);
        asignacion.setFechaCompletado(LocalDateTime.now());
        asignacion.setEvidencia(evidenciaNormalizada);
        asignacion.setAcreditada(false);

        return toResponse(ninoMisionRepository.save(asignacion));
    }

    @Transactional
    public MisionAcreditadaResponse acreditarMision(Long id, String emailTutorAutenticado) {
        Usuario tutor = buscarTutorPorEmail(emailTutorAutenticado);
        NinoMision asignacion = ninoMisionRepository.findByIdForUpdate(id)
                .orElseThrow(() -> new NinoMisionNotFoundException("La asignación de misión no existe"));

        if (!asignacion.getNino().getTutor().getId().equals(tutor.getId())) {
            throw new AccessDeniedException("No tienes permiso para acreditar esta misión");
        }

        if (asignacion.getEstado() != EstadoMisionAsignada.COMPLETADA) {
            throw new MisionNoAcreditableException("La misión debe estar completada antes de acreditarla");
        }

        if (Boolean.TRUE.equals(asignacion.getAcreditada())) {
            throw new MisionNoAcreditableException("La misión ya fue acreditada");
        }

        Nino nino = ninoRepository.findByIdForUpdate(asignacion.getNino().getId())
                .orElseThrow(() -> new NinoNotFoundException("Niño no encontrado"));
        int semillasAcreditadas = asignacion.getMision().getSemillasRecompensa();

        nino.setTotalSemillas(nino.getTotalSemillas() + semillasAcreditadas);
        asignacion.setAcreditada(true);

        ninoRepository.save(nino);
        ninoMisionRepository.save(asignacion);

        return new MisionAcreditadaResponse(
                asignacion.getId(),
                nino.getId(),
                asignacion.getMision().getId(),
                semillasAcreditadas,
                nino.getTotalSemillas(),
                asignacion.getAcreditada()
        );
    }

    @Transactional(readOnly = true)
    public List<MisionAsignadaResponse> listarPendientesPorTutor(String emailTutorAutenticado) {
        Usuario tutor = buscarTutorPorEmail(emailTutorAutenticado);
        return ninoMisionRepository.findByNinoTutorIdAndEstadoOrderByFechaAsignacionDesc(
                        tutor.getId(),
                        EstadoMisionAsignada.EN_PROGRESO
                ).stream()
                .map(this::toResponse)
                .toList();
    }

    private Usuario buscarTutorPorEmail(String email) {
        String emailNormalizado = email.trim().toLowerCase();
        return usuarioRepository.findByEmail(emailNormalizado)
                .orElseThrow(() -> new UsernameNotFoundException("Usuario no encontrado"));
    }

    private String validarEvidencia(ConfirmarMisionRequest request) {
        if (request == null || request.evidencia() == null || request.evidencia().isBlank()) {
            return null;
        }

        String evidencia = request.evidencia().trim();
        if (evidencia.length() > LONGITUD_MAXIMA_EVIDENCIA) {
            throw new EvidenciaInvalidaException("La evidencia supera el tamaño máximo permitido (2MB)");
        }

        if (!evidencia.startsWith("http://")
                && !evidencia.startsWith("https://")
                && !evidencia.startsWith("data:image/")) {
            throw new EvidenciaInvalidaException(
                    "Formato de evidencia inválido. Debe ser una URL válida o una imagen en Base64"
            );
        }

        return evidencia;
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
                asignacion.getFechaLimite(),
                asignacion.getEvidencia(),
                asignacion.getAcreditada()
        );
    }
}
