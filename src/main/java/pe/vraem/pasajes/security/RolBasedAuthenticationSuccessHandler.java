package pe.vraem.pasajes.security;

import java.io.IOException;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import pe.vraem.pasajes.auth.model.Usuario;
import pe.vraem.pasajes.auth.repository.UsuarioRepository;

/**
 * Tras un login exitoso, envia al ADMIN a su panel y al PASAJERO al listado
 * de viajes, en vez de una unica pagina de bienvenida para todos los roles.
 * Tambien resetea el contador de intentos fallidos del usuario.
 */
@Component
public class RolBasedAuthenticationSuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    private final UsuarioRepository usuarioRepository;

    public RolBasedAuthenticationSuccessHandler(UsuarioRepository usuarioRepository) {
        this.usuarioRepository = usuarioRepository;
    }

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
            Authentication authentication) throws IOException, ServletException {
        boolean esAdmin = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch("ROLE_ADMIN"::equals);

        usuarioRepository.findByEmail(authentication.getName()).ifPresent(usuario -> {
            usuario.resetearIntentosFallidos();
            usuarioRepository.save(usuario);
        });

        setDefaultTargetUrl(esAdmin ? "/admin" : "/viajes");
        super.onAuthenticationSuccess(request, response, authentication);
    }
}
