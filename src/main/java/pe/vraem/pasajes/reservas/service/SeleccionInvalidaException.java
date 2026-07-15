package pe.vraem.pasajes.reservas.service;

/**
 * Lanzada cuando la seleccion de asientos enviada por el pasajero es invalida
 * (sin asientos elegidos, o falta nombre/DNI de algun pasajero).
 */
public class SeleccionInvalidaException extends RuntimeException {

    public SeleccionInvalidaException(String message) {
        super(message);
    }
}
