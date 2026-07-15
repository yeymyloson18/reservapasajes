package pe.vraem.pasajes.reservas.service;

/**
 * Entrada de servicio: un asiento elegido por el pasajero junto con los datos
 * de quien lo ocupara (FR-006).
 */
public record SeleccionAsiento(Long asientoId, String nombrePasajero, String dniPasajero) {
}
