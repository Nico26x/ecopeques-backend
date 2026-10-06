package com.ecopeques.ecopeques_backend.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.ecopeques.ecopeques_backend.dto.NinoRequest;
import com.ecopeques.ecopeques_backend.dto.NinoResponse;
import com.ecopeques.ecopeques_backend.model.Nino;
import com.ecopeques.ecopeques_backend.model.Rol;
import com.ecopeques.ecopeques_backend.model.Usuario;
import com.ecopeques.ecopeques_backend.repository.NinoRepository;
import com.ecopeques.ecopeques_backend.repository.UsuarioRepository;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

@ExtendWith(MockitoExtension.class)
class NinoServiceTest {

    @Mock
    private NinoRepository ninoRepository;

    @Mock
    private UsuarioRepository usuarioRepository;

    private NinoService ninoService;

    @BeforeEach
    void setUp() {
        ninoService = new NinoService(ninoRepository, usuarioRepository);
    }

    @Test
    void crearNino_CuandoTutorExiste_DeberiaGuardarConTotalSemillasCeroYRetornarResponse() {
        Usuario tutor = crearTutor(1L, "tutor@email.com");
        NinoRequest request = new NinoRequest(" Lucas ", " avatar-1.png ", " Grupo A ");

        when(usuarioRepository.findByEmail("tutor@email.com")).thenReturn(Optional.of(tutor));
        when(ninoRepository.save(any(Nino.class))).thenAnswer(invocation -> {
            Nino nino = invocation.getArgument(0);
            nino.setId(10L);
            return nino;
        });

        NinoResponse response = ninoService.crearNino(request, " TUTOR@EMAIL.COM ");

        ArgumentCaptor<Nino> ninoCaptor = ArgumentCaptor.forClass(Nino.class);
        verify(ninoRepository).save(ninoCaptor.capture());

        Nino ninoGuardado = ninoCaptor.getValue();
        assertEquals("Lucas", ninoGuardado.getNombre());
        assertEquals("avatar-1.png", ninoGuardado.getAvatar());
        assertEquals("Grupo A", ninoGuardado.getGrupo());
        assertEquals(0, ninoGuardado.getTotalSemillas());
        assertEquals(tutor, ninoGuardado.getTutor());

        assertEquals(10L, response.id());
        assertEquals("Lucas", response.nombre());
        assertEquals(0, response.totalSemillas());
        assertEquals("avatar-1.png", response.avatar());
        assertEquals("Grupo A", response.grupo());
        assertEquals(1L, response.tutorId());
    }

    @Test
    void crearNino_CuandoTutorNoExiste_DeberiaLanzarUsernameNotFoundException() {
        NinoRequest request = new NinoRequest("Lucas", "avatar-1.png", "Grupo A");

        when(usuarioRepository.findByEmail("tutor@email.com")).thenReturn(Optional.empty());

        assertThrows(UsernameNotFoundException.class, () -> ninoService.crearNino(request, "tutor@email.com"));

        verify(ninoRepository, never()).save(any(Nino.class));
    }

    @Test
    void obtenerNinosPorTutor_CuandoTutorCoincide_DeberiaRetornarLista() {
        Usuario tutor = crearTutor(1L, "tutor@email.com");
        Nino lucas = crearNino(10L, "Lucas", "avatar-1.png", "Grupo A", tutor);
        Nino sofia = crearNino(11L, "Sofia", null, null, tutor);

        when(usuarioRepository.findByEmail("tutor@email.com")).thenReturn(Optional.of(tutor));
        when(ninoRepository.findByTutorIdOrderByIdAsc(1L)).thenReturn(List.of(lucas, sofia));

        List<NinoResponse> response = ninoService.obtenerNinosPorTutor(1L, "tutor@email.com");

        assertEquals(2, response.size());
        assertEquals(10L, response.get(0).id());
        assertEquals("Lucas", response.get(0).nombre());
        assertEquals("avatar-1.png", response.get(0).avatar());
        assertEquals("Grupo A", response.get(0).grupo());
        assertEquals(1L, response.get(0).tutorId());
        assertEquals(11L, response.get(1).id());
        assertEquals("Sofia", response.get(1).nombre());
    }

    @Test
    void obtenerNinosPorTutor_CuandoTutorNoCoincide_DeberiaLanzarAccessDeniedException() {
        Usuario tutor = crearTutor(1L, "tutor@email.com");

        when(usuarioRepository.findByEmail("tutor@email.com")).thenReturn(Optional.of(tutor));

        assertThrows(
                AccessDeniedException.class,
                () -> ninoService.obtenerNinosPorTutor(2L, "tutor@email.com")
        );

        verify(ninoRepository, never()).findByTutorIdOrderByIdAsc(2L);
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

    private Nino crearNino(Long id, String nombre, String avatar, String grupo, Usuario tutor) {
        return Nino.builder()
                .id(id)
                .nombre(nombre)
                .avatar(avatar)
                .grupo(grupo)
                .totalSemillas(0)
                .tutor(tutor)
                .build();
    }
}
