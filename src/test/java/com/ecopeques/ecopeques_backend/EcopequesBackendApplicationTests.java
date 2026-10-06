package com.ecopeques.ecopeques_backend;

import com.ecopeques.ecopeques_backend.repository.NinoRepository;
import com.ecopeques.ecopeques_backend.repository.UsuarioRepository;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@SpringBootTest(properties = {
		"spring.autoconfigure.exclude="
				+ "org.springframework.boot.jdbc.autoconfigure.DataSourceAutoConfiguration,"
				+ "org.springframework.boot.hibernate.autoconfigure.HibernateJpaAutoConfiguration,"
				+ "org.springframework.boot.data.jpa.autoconfigure.DataJpaRepositoriesAutoConfiguration",
		"ecopeques.jwt.secret=01234567890123456789012345678901",
		"ecopeques.jwt.expiration-ms=86400000"
})
class EcopequesBackendApplicationTests {

	@MockitoBean
	private UsuarioRepository usuarioRepository;

	@MockitoBean
	private NinoRepository ninoRepository;

	@Test
	void contextLoads() {
	}

}
