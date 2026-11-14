package com.ecopeques.ecopeques_backend.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.ecopeques.ecopeques_backend.dto.AsignarMisionRequest;
import com.ecopeques.ecopeques_backend.dto.ConfirmarMisionRequest;
import com.ecopeques.ecopeques_backend.dto.MisionAsignadaResponse;
import com.ecopeques.ecopeques_backend.exception.EvidenciaInvalidaException;
import com.ecopeques.ecopeques_backend.exception.LimiteMisionesAlcanzadoException;
import com.ecopeques.ecopeques_backend.exception.MisionNoConfirmableException;
import com.ecopeques.ecopeques_backend.exception.MisionNotFoundException;
import com.ecopeques.ecopeques_backend.exception.MisionYaAsignadaException;
import com.ecopeques.ecopeques_backend.exception.NinoMisionNotFoundException;
import com.ecopeques.ecopeques_backend.exception.NinoNotFoundException;
import com.ecopeques.ecopeques_backend.model.CategoriaMision;
import com.ecopeques.ecopeques_backend.model.EstadoMisionAsignada;
import com.ecopeques.ecopeques_backend.model.Mision;
import com.ecopeques.ecopeques_backend.model.Nino;
import com.ecopeques.ecopeques_backend.model.NinoMision;
import com.ecopeques.ecopeques_backend.model.Rol;
import com.ecopeques.ecopeques_backend.model.Usuario;
import com.ecopeques.ecopeques_backend.repository.MisionRepository;
import com.ecopeques.ecopeques_backend.repository.NinoMisionRepository;
import com.ecopeques.ecopeques_backend.repository.NinoRepository;
import com.ecopeques.ecopeques_backend.repository.UsuarioRepository;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

@ExtendWith(MockitoExtension.class)
class NinoMisionServiceTest {

    @Mock
    private NinoRepository ninoRepository;

    @Mock
    private MisionRepository misionRepository;

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private NinoMisionRepository ninoMisionRepository;

    private NinoMisionService ninoMisionService;

    @BeforeEach
    void setUp() {
        ninoMisionService = new NinoMisionService(
                ninoRepository,
                misionRepository,
                usuarioRepository,
                ninoMisionRepository
        );
    }

    @Test
    void asignarMision_CuandoDatosValidos_DeberiaCrearAsignacionEnProgreso() {
        Usuario tutor = crearTutor(1L, "tutor@email.com");
        Nino nino = crearNino(10L, tutor);
        Mision mision = crearMision(20L, true);
        LocalDate fechaLimite = LocalDate.now().plusDays(7);
        AsignarMisionRequest request = new AsignarMisionRequest(10L, 20L, fechaLimite);
        LocalDateTime fechaAsignacion = LocalDateTime.now();

        when(usuarioRepository.findByEmail("tutor@email.com")).thenReturn(Optional.of(tutor));
        when(ninoRepository.findById(10L)).thenReturn(Optional.of(nino));
        when(misionRepository.findById(20L)).thenReturn(Optional.of(mision));
        when(ninoMisionRepository.existsByNinoIdAndMisionIdAndEstado(10L, 20L, EstadoMisionAsignada.EN_PROGRESO))
                .thenReturn(false);
        when(ninoMisionRepository.countByNinoIdAndEstado(10L, EstadoMisionAsignada.EN_PROGRESO)).thenReturn(2L);
        when(ninoMisionRepository.save(any(NinoMision.class))).thenAnswer(invocation -> {
            NinoMision asignacion = invocation.getArgument(0);
            asignacion.setId(100L);
            asignacion.setFechaAsignacion(fechaAsignacion);
            return asignacion;
        });

        MisionAsignadaResponse response = ninoMisionService.asignarMision(request, " TUTOR@EMAIL.COM ");

        ArgumentCaptor<NinoMision> asignacionCaptor = ArgumentCaptor.forClass(NinoMision.class);
        verify(ninoMisionRepository).save(asignacionCaptor.capture());

        NinoMision asignacionGuardada = asignacionCaptor.getValue();
        assertEquals(nino, asignacionGuardada.getNino());
        assertEquals(mision, asignacionGuardada.getMision());
        assertEquals(EstadoMisionAsignada.EN_PROGRESO, asignacionGuardada.getEstado());
        assertEquals(fechaLimite, asignacionGuardada.getFechaLimite());

        assertEquals(100L, response.id());
        assertEquals(10L, response.ninoId());
        assertEquals(20L, response.misionId());
        assertEquals("Cerrar la llave", response.tituloMision());
        assertEquals(CategoriaMision.AGUA, response.categoria());
        assertEquals(10, response.semillasRecompensa());
        assertEquals(EstadoMisionAsignada.EN_PROGRESO, response.estado());
        assertEquals(fechaAsignacion, response.fechaAsignacion());
        assertEquals(fechaLimite, response.fechaLimite());
        assertNull(response.evidencia());
        assertEquals(false, response.acreditada());
    }

    @Test
    void asignarMision_CuandoNinoNoExiste_DeberiaLanzarNinoNotFoundException() {
        Usuario tutor = crearTutor(1L, "tutor@email.com");
        AsignarMisionRequest request = new AsignarMisionRequest(10L, 20L, null);

        when(usuarioRepository.findByEmail("tutor@email.com")).thenReturn(Optional.of(tutor));
        when(ninoRepository.findById(10L)).thenReturn(Optional.empty());

        assertThrows(NinoNotFoundException.class, () -> ninoMisionService.asignarMision(request, "tutor@email.com"));

        verify(misionRepository, never()).findById(20L);
        verify(ninoMisionRepository, never()).save(any(NinoMision.class));
    }

    @Test
    void asignarMision_CuandoMisionNoExiste_DeberiaLanzarMisionNotFoundException() {
        Usuario tutor = crearTutor(1L, "tutor@email.com");
        Nino nino = crearNino(10L, tutor);
        AsignarMisionRequest request = new AsignarMisionRequest(10L, 20L, null);

        when(usuarioRepository.findByEmail("tutor@email.com")).thenReturn(Optional.of(tutor));
        when(ninoRepository.findById(10L)).thenReturn(Optional.of(nino));
        when(misionRepository.findById(20L)).thenReturn(Optional.empty());

        assertThrows(MisionNotFoundException.class, () -> ninoMisionService.asignarMision(request, "tutor@email.com"));

        verify(ninoMisionRepository, never()).save(any(NinoMision.class));
    }

    @Test
    void asignarMision_CuandoNinoNoPerteneceAlTutor_DeberiaLanzarAccessDeniedException() {
        Usuario tutor = crearTutor(1L, "tutor@email.com");
        Usuario otroTutor = crearTutor(2L, "otro@email.com");
        Nino nino = crearNino(10L, otroTutor);
        AsignarMisionRequest request = new AsignarMisionRequest(10L, 20L, null);

        when(usuarioRepository.findByEmail("tutor@email.com")).thenReturn(Optional.of(tutor));
        when(ninoRepository.findById(10L)).thenReturn(Optional.of(nino));

        assertThrows(AccessDeniedException.class, () -> ninoMisionService.asignarMision(request, "tutor@email.com"));

        verify(misionRepository, never()).findById(20L);
        verify(ninoMisionRepository, never()).save(any(NinoMision.class));
    }

    @Test
    void asignarMision_CuandoMisionYaEstaEnProgreso_DeberiaLanzarMisionYaAsignadaException() {
        Usuario tutor = crearTutor(1L, "tutor@email.com");
        Nino nino = crearNino(10L, tutor);
        Mision mision = crearMision(20L, true);
        AsignarMisionRequest request = new AsignarMisionRequest(10L, 20L, null);

        when(usuarioRepository.findByEmail("tutor@email.com")).thenReturn(Optional.of(tutor));
        when(ninoRepository.findById(10L)).thenReturn(Optional.of(nino));
        when(misionRepository.findById(20L)).thenReturn(Optional.of(mision));
        when(ninoMisionRepository.existsByNinoIdAndMisionIdAndEstado(10L, 20L, EstadoMisionAsignada.EN_PROGRESO))
                .thenReturn(true);

        assertThrows(MisionYaAsignadaException.class, () -> ninoMisionService.asignarMision(request, "tutor@email.com"));

        verify(ninoMisionRepository, never()).countByNinoIdAndEstado(10L, EstadoMisionAsignada.EN_PROGRESO);
        verify(ninoMisionRepository, never()).save(any(NinoMision.class));
    }

    @Test
    void asignarMision_CuandoNinoTieneTresMisionesActivas_DeberiaLanzarLimiteMisionesAlcanzadoException() {
        Usuario tutor = crearTutor(1L, "tutor@email.com");
        Nino nino = crearNino(10L, tutor);
        Mision mision = crearMision(20L, true);
        AsignarMisionRequest request = new AsignarMisionRequest(10L, 20L, null);

        when(usuarioRepository.findByEmail("tutor@email.com")).thenReturn(Optional.of(tutor));
        when(ninoRepository.findById(10L)).thenReturn(Optional.of(nino));
        when(misionRepository.findById(20L)).thenReturn(Optional.of(mision));
        when(ninoMisionRepository.existsByNinoIdAndMisionIdAndEstado(10L, 20L, EstadoMisionAsignada.EN_PROGRESO))
                .thenReturn(false);
        when(ninoMisionRepository.countByNinoIdAndEstado(10L, EstadoMisionAsignada.EN_PROGRESO)).thenReturn(3L);

        assertThrows(
                LimiteMisionesAlcanzadoException.class,
                () -> ninoMisionService.asignarMision(request, "tutor@email.com")
        );

        verify(ninoMisionRepository, never()).save(any(NinoMision.class));
    }

    @Test
    void confirmarMision_SinEvidencia_DeberiaCompletarSinAcreditarSemillas() {
        Usuario tutor = crearTutor(1L, "tutor@email.com");
        Nino nino = crearNino(10L, tutor);
        nino.setTotalSemillas(25);
        NinoMision asignacion = crearAsignacion(100L, nino, EstadoMisionAsignada.EN_PROGRESO);

        configurarConfirmacion(tutor, asignacion);

        MisionAsignadaResponse response = ninoMisionService.confirmarMision(
                100L,
                null,
                " TUTOR@EMAIL.COM "
        );

        assertEquals(EstadoMisionAsignada.COMPLETADA, asignacion.getEstado());
        assertNotNull(asignacion.getFechaCompletado());
        assertNull(asignacion.getEvidencia());
        assertEquals(false, asignacion.getAcreditada());
        assertEquals(25, nino.getTotalSemillas());
        assertEquals(EstadoMisionAsignada.COMPLETADA, response.estado());
        assertEquals(false, response.acreditada());
        verify(ninoMisionRepository).save(asignacion);
    }

    @Test
    void confirmarMision_ConUrlValida_DeberiaGuardarEvidenciaNormalizada() {
        Usuario tutor = crearTutor(1L, "tutor@email.com");
        NinoMision asignacion = crearAsignacion(
                100L,
                crearNino(10L, tutor),
                EstadoMisionAsignada.EN_PROGRESO
        );
        configurarConfirmacion(tutor, asignacion);

        MisionAsignadaResponse response = ninoMisionService.confirmarMision(
                100L,
                new ConfirmarMisionRequest("  https://example.com/evidencia.jpg  "),
                "tutor@email.com"
        );

        assertEquals("https://example.com/evidencia.jpg", asignacion.getEvidencia());
        assertEquals("https://example.com/evidencia.jpg", response.evidencia());
    }

    @Test
    void confirmarMision_ConImagenBase64Valida_DeberiaGuardarEvidencia() {
        Usuario tutor = crearTutor(1L, "tutor@email.com");
        NinoMision asignacion = crearAsignacion(
                100L,
                crearNino(10L, tutor),
                EstadoMisionAsignada.EN_PROGRESO
        );
        String evidencia = "data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAAEAAAAB";
        configurarConfirmacion(tutor, asignacion);

        MisionAsignadaResponse response = ninoMisionService.confirmarMision(
                100L,
                new ConfirmarMisionRequest(evidencia),
                "tutor@email.com"
        );

        assertEquals(evidencia, asignacion.getEvidencia());
        assertEquals(evidencia, response.evidencia());
    }

    @Test
    void confirmarMision_CuandoAsignacionNoExiste_DeberiaLanzarNinoMisionNotFoundException() {
        Usuario tutor = crearTutor(1L, "tutor@email.com");
        when(usuarioRepository.findByEmail("tutor@email.com")).thenReturn(Optional.of(tutor));
        when(ninoMisionRepository.findByIdForUpdate(100L)).thenReturn(Optional.empty());

        assertThrows(
                NinoMisionNotFoundException.class,
                () -> ninoMisionService.confirmarMision(100L, null, "tutor@email.com")
        );

        verify(ninoMisionRepository, never()).save(any(NinoMision.class));
    }

    @Test
    void confirmarMision_CuandoPerteneceAOtroTutor_DeberiaLanzarAccessDeniedException() {
        Usuario tutor = crearTutor(1L, "tutor@email.com");
        Usuario otroTutor = crearTutor(2L, "otro@email.com");
        NinoMision asignacion = crearAsignacion(
                100L,
                crearNino(10L, otroTutor),
                EstadoMisionAsignada.EN_PROGRESO
        );

        when(usuarioRepository.findByEmail("tutor@email.com")).thenReturn(Optional.of(tutor));
        when(ninoMisionRepository.findByIdForUpdate(100L)).thenReturn(Optional.of(asignacion));

        assertThrows(
                AccessDeniedException.class,
                () -> ninoMisionService.confirmarMision(100L, null, "tutor@email.com")
        );

        verify(ninoMisionRepository, never()).save(any(NinoMision.class));
    }

    @Test
    void confirmarMision_CuandoYaEstaCompletada_DeberiaLanzarMisionNoConfirmableException() {
        Usuario tutor = crearTutor(1L, "tutor@email.com");
        NinoMision asignacion = crearAsignacion(
                100L,
                crearNino(10L, tutor),
                EstadoMisionAsignada.COMPLETADA
        );

        when(usuarioRepository.findByEmail("tutor@email.com")).thenReturn(Optional.of(tutor));
        when(ninoMisionRepository.findByIdForUpdate(100L)).thenReturn(Optional.of(asignacion));

        assertThrows(
                MisionNoConfirmableException.class,
                () -> ninoMisionService.confirmarMision(100L, null, "tutor@email.com")
        );

        verify(ninoMisionRepository, never()).save(any(NinoMision.class));
    }

    @Test
    void confirmarMision_ConFormatoDeEvidenciaInvalido_DeberiaLanzarEvidenciaInvalidaException() {
        Usuario tutor = crearTutor(1L, "tutor@email.com");
        NinoMision asignacion = crearAsignacion(
                100L,
                crearNino(10L, tutor),
                EstadoMisionAsignada.EN_PROGRESO
        );
        configurarAsignacionParaConfirmar(tutor, asignacion);

        assertThrows(
                EvidenciaInvalidaException.class,
                () -> ninoMisionService.confirmarMision(
                        100L,
                        new ConfirmarMisionRequest("archivo-local.jpg"),
                        "tutor@email.com"
                )
        );

        verify(ninoMisionRepository, never()).save(any(NinoMision.class));
    }

    @Test
    void confirmarMision_ConEvidenciaDemasiadoGrande_DeberiaLanzarEvidenciaInvalidaException() {
        Usuario tutor = crearTutor(1L, "tutor@email.com");
        NinoMision asignacion = crearAsignacion(
                100L,
                crearNino(10L, tutor),
                EstadoMisionAsignada.EN_PROGRESO
        );
        String evidencia = "data:image/png;base64," + "A".repeat(2_800_000);
        configurarAsignacionParaConfirmar(tutor, asignacion);

        assertThrows(
                EvidenciaInvalidaException.class,
                () -> ninoMisionService.confirmarMision(
                        100L,
                        new ConfirmarMisionRequest(evidencia),
                        "tutor@email.com"
                )
        );

        verify(ninoMisionRepository, never()).save(any(NinoMision.class));
    }

    @Test
    void listarPendientesPorTutor_DeberiaRetornarAsignacionesEnProgreso() {
        Usuario tutor = crearTutor(1L, "tutor@email.com");
        NinoMision asignacion = crearAsignacion(
                100L,
                crearNino(10L, tutor),
                EstadoMisionAsignada.EN_PROGRESO
        );
        when(usuarioRepository.findByEmail("tutor@email.com")).thenReturn(Optional.of(tutor));
        when(ninoMisionRepository.findByNinoTutorIdAndEstadoOrderByFechaAsignacionDesc(
                1L,
                EstadoMisionAsignada.EN_PROGRESO
        )).thenReturn(List.of(asignacion));

        List<MisionAsignadaResponse> response = ninoMisionService.listarPendientesPorTutor("tutor@email.com");

        assertEquals(1, response.size());
        assertEquals(100L, response.getFirst().id());
        assertEquals(EstadoMisionAsignada.EN_PROGRESO, response.getFirst().estado());
    }

    private void configurarConfirmacion(Usuario tutor, NinoMision asignacion) {
        configurarAsignacionParaConfirmar(tutor, asignacion);
        when(ninoMisionRepository.save(asignacion)).thenReturn(asignacion);
    }

    private void configurarAsignacionParaConfirmar(Usuario tutor, NinoMision asignacion) {
        when(usuarioRepository.findByEmail("tutor@email.com")).thenReturn(Optional.of(tutor));
        when(ninoMisionRepository.findByIdForUpdate(asignacion.getId())).thenReturn(Optional.of(asignacion));
    }

    private NinoMision crearAsignacion(Long id, Nino nino, EstadoMisionAsignada estado) {
        return NinoMision.builder()
                .id(id)
                .nino(nino)
                .mision(crearMision(20L, true))
                .estado(estado)
                .fechaAsignacion(LocalDateTime.now())
                .acreditada(false)
                .build();
    }

    private Usuario crearTutor(Long id, String email) {
        return Usuario.builder()
                .id(id)
                .nombre("Tutor")
                .apellido("EcoPeques")
                .email(email)
                .password("password-encriptado")
                .rol(Rol.PADRE)
                .build();
    }

    private Nino crearNino(Long id, Usuario tutor) {
        return Nino.builder()
                .id(id)
                .nombre("Lucas")
                .avatar("avatar.png")
                .grupo("Grupo A")
                .totalSemillas(0)
                .tutor(tutor)
                .build();
    }

    private Mision crearMision(Long id, boolean activa) {
        return Mision.builder()
                .id(id)
                .titulo("Cerrar la llave")
                .descripcion("Ahorra agua cerrando la llave mientras te lavas los dientes.")
                .categoria(CategoriaMision.AGUA)
                .semillasRecompensa(10)
                .activa(activa)
                .build();
    }
}
