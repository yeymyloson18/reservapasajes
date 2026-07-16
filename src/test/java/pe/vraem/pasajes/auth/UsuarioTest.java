package pe.vraem.pasajes.auth;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import pe.vraem.pasajes.auth.model.Rol;
import pe.vraem.pasajes.auth.model.Usuario;

class UsuarioTest {

    @Test
    void seBloqueaAlLlegarAlNumeroMaximoDeIntentosFallidos() {
        Usuario usuario = new Usuario("45678912", "Ana Quispe", "ana@example.com", "hash", Rol.PASAJERO);

        for (int i = 0; i < Usuario.INTENTOS_MAXIMOS - 1; i++) {
            usuario.registrarIntentoFallido();
        }
        assertThat(usuario.estaBloqueado()).isFalse();

        usuario.registrarIntentoFallido();

        assertThat(usuario.estaBloqueado()).isTrue();
    }

    @Test
    void resetearIntentosFallidosDesbloqueaLaCuenta() {
        Usuario usuario = new Usuario("45678912", "Ana Quispe", "ana@example.com", "hash", Rol.PASAJERO);
        for (int i = 0; i < Usuario.INTENTOS_MAXIMOS; i++) {
            usuario.registrarIntentoFallido();
        }
        assertThat(usuario.estaBloqueado()).isTrue();

        usuario.resetearIntentosFallidos();

        assertThat(usuario.estaBloqueado()).isFalse();
    }
}
