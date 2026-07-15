package pe.vraem.pasajes.auth.service;

import java.security.SecureRandom;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import pe.vraem.pasajes.auth.model.Rol;
import pe.vraem.pasajes.auth.model.Usuario;
import pe.vraem.pasajes.auth.repository.UsuarioRepository;

@Service
public class UsuarioService {

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;

    public UsuarioService(UsuarioRepository usuarioRepository, PasswordEncoder passwordEncoder) {
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
    }

    /**
     * Registra un nuevo pasajero (FR-001, FR-002). El DNI solo se valida por formato
     * (garantizado por la anotacion @Pattern de la entidad), sin verificacion externa.
     */
    @Transactional
    public Usuario registrar(String dni, String nombre, String email, String passwordPlano) {
        if (usuarioRepository.existsByEmail(email)) {
            throw new RegistroDuplicadoException("Ya existe una cuenta registrada con ese email");
        }
        if (usuarioRepository.existsByDni(dni)) {
            throw new RegistroDuplicadoException("Ya existe una cuenta registrada con ese DNI");
        }

        String passwordHash = passwordEncoder.encode(passwordPlano);
        Usuario usuario = new Usuario(dni, nombre, email, passwordHash, Rol.PASAJERO);
        return usuarioRepository.save(usuario);
    }

    /**
     * Genera una contrasena temporal para el email indicado y la guarda hasheada.
     * No hay servidor de correo configurado, por lo que la contrasena en texto
     * plano se retorna una unica vez para mostrarla en pantalla al usuario.
     */
    @Transactional
    public String recuperarPassword(String email) {
        Usuario usuario = usuarioRepository.findByEmail(email)
                .orElseThrow(() -> new UsuarioNoEncontradoException(email));

        String passwordTemporal = generarPasswordTemporal();
        usuario.actualizarPasswordHash(passwordEncoder.encode(passwordTemporal));
        usuarioRepository.save(usuario);

        return passwordTemporal;
    }

    private String generarPasswordTemporal() {
        SecureRandom random = new SecureRandom();
        int numero = 100000 + random.nextInt(900000);
        return "Vraem" + numero;
    }
}
