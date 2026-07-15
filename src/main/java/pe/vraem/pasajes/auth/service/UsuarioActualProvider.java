package pe.vraem.pasajes.auth.service;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

import pe.vraem.pasajes.auth.model.Usuario;
import pe.vraem.pasajes.auth.repository.UsuarioRepository;

/**
 * Resuelve la entidad {@link Usuario} autenticada a partir del contexto de
 * seguridad. Usado por los controllers de reservas, pagos y admin para saber
 * quien esta realizando la accion.
 */
@Service
public class UsuarioActualProvider {

    private final UsuarioRepository usuarioRepository;

    public UsuarioActualProvider(UsuarioRepository usuarioRepository) {
        this.usuarioRepository = usuarioRepository;
    }

    public Usuario obtener(Authentication authentication) {
        return usuarioRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new IllegalStateException(
                        "Usuario autenticado no encontrado: " + authentication.getName()));
    }
}
