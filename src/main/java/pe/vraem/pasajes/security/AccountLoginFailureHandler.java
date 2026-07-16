package pe.vraem.pasajes.security;

import java.io.IOException;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.security.authentication.LockedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationFailureHandler;
import org.springframework.stereotype.Component;

import pe.vraem.pasajes.auth.repository.UsuarioRepository;

/**
 * Ante credenciales incorrectas, incrementa el contador de intentos fallidos
 * del usuario (bloqueando la cuenta al llegar a {@link pe.vraem.pasajes.auth.model.Usuario#INTENTOS_MAXIMOS}).
 * Distingue el mensaje cuando el rechazo es porque la cuenta ya esta bloqueada.
 */
@Component
public class AccountLoginFailureHandler extends SimpleUrlAuthenticationFailureHandler {

    private final UsuarioRepository usuarioRepository;

    public AccountLoginFailureHandler(UsuarioRepository usuarioRepository) {
        this.usuarioRepository = usuarioRepository;
    }

    @Override
    public void onAuthenticationFailure(HttpServletRequest request, HttpServletResponse response,
            AuthenticationException exception) throws IOException, ServletException {
        if (exception instanceof LockedException) {
            setDefaultFailureUrl("/login?locked");
        } else {
            String email = request.getParameter("username");
            if (email != null) {
                usuarioRepository.findByEmail(email).ifPresent(usuario -> {
                    usuario.registrarIntentoFallido();
                    usuarioRepository.save(usuario);
                });
            }
            setDefaultFailureUrl("/login?error");
        }
        super.onAuthenticationFailure(request, response, exception);
    }
}
