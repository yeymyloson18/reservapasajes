package pe.vraem.pasajes.reservas.service;

/**
 * Lanzada cuando, al confirmar una seleccion de asientos, alguno de ellos ya no
 * esta LIBRE (fue tomado por otra reserva concurrente) o no pertenece al viaje (FR-008).
 */
public class AsientoNoDisponibleException extends RuntimeException {

    public AsientoNoDisponibleException(String message) {
        super(message);
    }
}
