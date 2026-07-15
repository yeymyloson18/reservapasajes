package pe.vraem.pasajes.auth.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import pe.vraem.pasajes.auth.model.Usuario;

public interface UsuarioRepository extends JpaRepository<Usuario, Long> {

    Optional<Usuario> findByEmail(String email);

    boolean existsByEmail(String email);

    boolean existsByDni(String dni);
}
