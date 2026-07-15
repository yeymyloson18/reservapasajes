package pe.vraem.pasajes.auth.service;

/**
 * Lanzada cuando el email o el DNI indicados en un registro ya pertenecen a otra cuenta (FR-001).
 */
public class RegistroDuplicadoException extends RuntimeException {

    public RegistroDuplicadoException(String message) {
        super(message);
    }
}
