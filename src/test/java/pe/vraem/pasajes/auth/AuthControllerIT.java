package pe.vraem.pasajes.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestBuilders.formLogin;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.transaction.annotation.Transactional;

import pe.vraem.pasajes.auth.model.Usuario;
import pe.vraem.pasajes.auth.repository.UsuarioRepository;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class AuthControllerIT {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Test
    void registraUnPasajeroYLuegoIniciaSesion() throws Exception {
        mockMvc.perform(post("/registro")
                        .param("dni", "45678912")
                        .param("nombre", "Ana Quispe")
                        .param("email", "ana@example.com")
                        .param("password", "claveSegura1")
                        .with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login?registrado"));

        Usuario creado = usuarioRepository.findByEmail("ana@example.com").orElseThrow();
        assertThat(passwordEncoder.matches("claveSegura1", creado.getPasswordHash())).isTrue();

        mockMvc.perform(formLogin().user("ana@example.com").password("claveSegura1"))
                .andExpect(SecurityMockMvcResultMatchers.authenticated());
    }

    @Test
    void rechazaRegistroConEmailDuplicado() throws Exception {
        usuarioRepository.save(new Usuario("11111111", "Otro", "duplicado@example.com",
                passwordEncoder.encode("otraClave1"), pe.vraem.pasajes.auth.model.Rol.PASAJERO));

        mockMvc.perform(post("/registro")
                        .param("dni", "22222222")
                        .param("nombre", "Nuevo Usuario")
                        .param("email", "duplicado@example.com")
                        .param("password", "claveSegura1")
                        .with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf()))
                .andExpect(status().isOk());
    }

    @Test
    void bloqueaLaCuentaTrasCincoIntentosFallidosAunqueLuegoUseLaClaveCorrecta() throws Exception {
        usuarioRepository.save(new Usuario("33333333", "Usuario Bloqueable", "bloqueable@example.com",
                passwordEncoder.encode("claveSegura1"), pe.vraem.pasajes.auth.model.Rol.PASAJERO));

        for (int i = 0; i < pe.vraem.pasajes.auth.model.Usuario.INTENTOS_MAXIMOS; i++) {
            mockMvc.perform(formLogin().user("bloqueable@example.com").password("claveMala"))
                    .andExpect(SecurityMockMvcResultMatchers.unauthenticated());
        }

        Usuario bloqueado = usuarioRepository.findByEmail("bloqueable@example.com").orElseThrow();
        assertThat(bloqueado.estaBloqueado()).isTrue();

        mockMvc.perform(formLogin().user("bloqueable@example.com").password("claveSegura1"))
                .andExpect(SecurityMockMvcResultMatchers.unauthenticated());
    }

    @Test
    void unSegundoLoginInvalidaLaSesionAnterior() throws Exception {
        usuarioRepository.save(new Usuario("44445555", "Usuario Sesion", "sesion@example.com",
                passwordEncoder.encode("claveSegura1"), pe.vraem.pasajes.auth.model.Rol.PASAJERO));

        MvcResult primerLogin = mockMvc.perform(formLogin().user("sesion@example.com").password("claveSegura1"))
                .andExpect(SecurityMockMvcResultMatchers.authenticated())
                .andReturn();
        MockHttpSession primeraSesion = (MockHttpSession) primerLogin.getRequest().getSession(false);

        mockMvc.perform(formLogin().user("sesion@example.com").password("claveSegura1"))
                .andExpect(SecurityMockMvcResultMatchers.authenticated());

        MvcResult resultadoConSesionVieja = mockMvc.perform(get("/viajes").session(primeraSesion))
                .andExpect(status().is3xxRedirection())
                .andReturn();
        String location = resultadoConSesionVieja.getResponse().getRedirectedUrl();
        assertThat(location).contains("/login").contains("expired");
    }
}
