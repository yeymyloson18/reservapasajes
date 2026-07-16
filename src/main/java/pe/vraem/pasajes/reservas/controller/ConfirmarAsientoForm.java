package pe.vraem.pasajes.reservas.controller;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

import pe.vraem.pasajes.pagos.model.MetodoPago;

/**
 * Datos del pasajero que ocupara el asiento elegido, capturados en la
 * pantalla de confirmacion antes de crear la reserva (FR-006). El campo
 * metodoPago solo se usa (y se muestra) cuando quien reserva es el ADMIN,
 * para una venta presencial con pago confirmado al instante (FR-036).
 */
public class ConfirmarAsientoForm {

    @NotBlank(message = "El nombre del pasajero es obligatorio")
    private String nombrePasajero;

    @NotBlank(message = "El DNI del pasajero es obligatorio")
    @Pattern(regexp = "\\d{8}", message = "El DNI debe tener exactamente 8 digitos numericos")
    private String dniPasajero;

    private MetodoPago metodoPago;

    public String getNombrePasajero() {
        return nombrePasajero;
    }

    public void setNombrePasajero(String nombrePasajero) {
        this.nombrePasajero = nombrePasajero;
    }

    public String getDniPasajero() {
        return dniPasajero;
    }

    public void setDniPasajero(String dniPasajero) {
        this.dniPasajero = dniPasajero;
    }

    public MetodoPago getMetodoPago() {
        return metodoPago;
    }

    public void setMetodoPago(MetodoPago metodoPago) {
        this.metodoPago = metodoPago;
    }
}
