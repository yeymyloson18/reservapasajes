package pe.vraem.pasajes.reservas.controller;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/**
 * Datos del pasajero que ocupara el asiento elegido, capturados en la
 * pantalla de confirmacion antes de crear la reserva (FR-006).
 */
public class ConfirmarAsientoForm {

    @NotBlank(message = "El nombre del pasajero es obligatorio")
    private String nombrePasajero;

    @NotBlank(message = "El DNI del pasajero es obligatorio")
    @Pattern(regexp = "\\d{8}", message = "El DNI debe tener exactamente 8 digitos numericos")
    private String dniPasajero;

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
}
