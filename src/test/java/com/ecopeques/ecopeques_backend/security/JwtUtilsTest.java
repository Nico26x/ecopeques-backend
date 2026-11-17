package com.ecopeques.ecopeques_backend.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.ecopeques.ecopeques_backend.model.Rol;
import com.ecopeques.ecopeques_backend.model.Usuario;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class JwtUtilsTest {

    private static final String SECRET = "MDEyMzQ1Njc4OTAxMjM0NTY3ODkwMTIzNDU2Nzg5MDE=";
    private static final long EXPIRATION_MS = 60_000L;

    private JwtUtils jwtUtils;
    private Usuario usuario;

    @BeforeEach
    void setUp() {
        jwtUtils = new JwtUtils(SECRET, EXPIRATION_MS);
        usuario = Usuario.builder()
                .id(42L)
                .email("padre@ecopeques.com")
                .rol(Rol.PADRE)
                .build();
    }

    @Test
    void generarToken_IncluyeClaimsDelUsuarioYFechas() {
        long instanteInicial = System.currentTimeMillis();

        String token = jwtUtils.generateToken(usuario);
        Claims claims = parsearClaims(token);

        assertEquals("padre@ecopeques.com", claims.getSubject());
        assertEquals(42L, ((Number) claims.get("id")).longValue());
        assertEquals("PADRE", claims.get("rol"));
        assertTrue(claims.getIssuedAt().getTime() >= instanteInicial - 1_000L);
        assertEquals(EXPIRATION_MS, claims.getExpiration().getTime() - claims.getIssuedAt().getTime());
    }

    @Test
    void obtenerEmailYValidar_CuandoTokenEsValido_RetornaDatosEsperados() {
        String token = jwtUtils.generateToken(usuario);

        assertEquals("padre@ecopeques.com", jwtUtils.getEmailFromToken(token));
        assertTrue(jwtUtils.validateToken(token));
    }

    @Test
    void validarToken_CuandoEstaExpirado_RetornaFalse() {
        JwtUtils jwtExpirado = new JwtUtils(SECRET, -1_000L);
        String token = jwtExpirado.generateToken(usuario);

        assertFalse(jwtUtils.validateToken(token));
    }

    @Test
    void validarToken_CuandoFueAlterado_RetornaFalse() {
        String token = jwtUtils.generateToken(usuario);
        int inicioFirma = token.lastIndexOf('.') + 1;
        char primerCaracterFirma = token.charAt(inicioFirma);
        char reemplazo = primerCaracterFirma == 'A' ? 'B' : 'A';
        String tokenAlterado = token.substring(0, inicioFirma)
                + reemplazo
                + token.substring(inicioFirma + 1);

        assertFalse(jwtUtils.validateToken(tokenAlterado));
    }

    @Test
    void validarToken_CuandoEstaMalformado_RetornaFalse() {
        assertFalse(jwtUtils.validateToken("token-malformado"));
    }

    private Claims parsearClaims(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(Keys.hmacShaKeyFor(Decoders.BASE64.decode(SECRET)))
                .build()
                .parseClaimsJws(token)
                .getBody();
    }
}
