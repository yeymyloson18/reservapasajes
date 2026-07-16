package pe.vraem.pasajes.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import pe.vraem.pasajes.auth.model.Rol;
import pe.vraem.pasajes.auth.model.Usuario;
import pe.vraem.pasajes.auth.repository.UsuarioRepository;
import pe.vraem.pasajes.auth.service.RegistroDuplicadoException;
import pe.vraem.pasajes.auth.service.UsuarioService;

@ExtendWith(MockitoExtension.class)
class UsuarioServiceTest {

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    private UsuarioService usuarioService;

    @BeforeEach
    void setUp() {
        usuarioService = new UsuarioService(usuarioRepository, passwordEncoder);
    }

    @Test
    void registraUnPasajeroConContrasenaHasheada() {
        when(usuarioRepository.existsByEmail("ana@example.com")).thenReturn(false);
        when(usuarioRepository.existsByDni("45678912")).thenReturn(false);
        when(passwordEncoder.encode("claveSegura1")).thenReturn("hash-bcrypt");
        when(usuarioRepository.save(any(Usuario.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Usuario usuario = usuarioService.registrar("45678912", "Ana Quispe", "ana@example.com", "claveSegura1");

        assertThat(usuario.getDni()).isEqualTo("45678912");
        assertThat(usuario.getPasswordHash()).isEqualTo("hash-bcrypt");
        assertThat(usuario.getRol()).isEqualTo(Rol.PASAJERO);
    }

    @Test
    void rechazaRegistroConEmailYaExistente() {
        when(usuarioRepository.existsByEmail("ana@example.com")).thenReturn(true);

        assertThatThrownBy(() -> usuarioService.registrar("45678912", "Ana Quispe", "ana@example.com", "claveSegura1"))
                .isInstanceOf(RegistroDuplicadoException.class);
    }

    @Test
    void rechazaRegistroConDniYaExistente() {
        when(usuarioRepository.existsByEmail("ana@example.com")).thenReturn(false);
        when(usuarioRepository.existsByDni("45678912")).thenReturn(true);

        assertThatThrownBy(() -> usuarioService.registrar("45678912", "Ana Quispe", "ana@example.com", "claveSegura1"))
                .isInstanceOf(RegistroDuplicadoException.class);
    }

    @Test
    void cambiarPasswordActualizaElHashYGuarda() {
        Usuario usuario = new Usuario("45678912", "Ana Quispe", "ana@example.com", "hash-viejo", Rol.PASAJERO);
        when(passwordEncoder.encode("NuevaClave1")).thenReturn("hash-nuevo");

        usuarioService.cambiarPassword(usuario, "NuevaClave1");

        assertThat(usuario.getPasswordHash()).isEqualTo("hash-nuevo");
        org.mockito.Mockito.verify(usuarioRepository).save(usuario);
    }
}
