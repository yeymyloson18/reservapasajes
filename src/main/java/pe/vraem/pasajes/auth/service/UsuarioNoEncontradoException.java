package pe.vraem.pasajes.auth.service;

public class UsuarioNoEncontradoException extends RuntimeException {

    public UsuarioNoEncontradoException(String email) {
        super("No existe una cuenta registrada con el email " + email);
    }
}
