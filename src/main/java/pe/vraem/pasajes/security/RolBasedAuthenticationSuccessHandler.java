package pe.vraem.pasajes.security;

import java.io.IOException;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

/**
 * Tras un login exitoso, envia al ADMIN a su panel y al PASAJERO al listado
 * de viajes, en vez de una unica pagina de bienvenida para todos los roles.
 */
@Component
public class RolBasedAuthenticationSuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
            Authentication authentication) throws IOException, ServletException {
        boolean esAdmin = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch("ROLE_ADMIN"::equals);

        setDefaultTargetUrl(esAdmin ? "/admin" : "/viajes");
        super.onAuthenticationSuccess(request, response, authentication);
    }
}
