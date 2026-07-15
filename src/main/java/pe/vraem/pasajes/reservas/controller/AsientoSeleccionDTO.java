package pe.vraem.pasajes.reservas.controller;

import jakarta.validation.constraints.Pattern;

/**
 * Una fila del formulario de seleccion de asientos: representa un asiento del
 * viaje, marcado o no para reserva, con los datos del pasajero que lo ocupara.
 */
public class AsientoSeleccionDTO {

    private Long asientoId;

    private boolean seleccionado;

    private String nombrePasajero;

    @Pattern(regexp = "\\d{8}|", message = "El DNI debe tener exactamente 8 digitos numericos")
    private String dniPasajero;

    public AsientoSeleccionDTO() {
        // binding de formulario
    }

    public AsientoSeleccionDTO(Long asientoId) {
        this.asientoId = asientoId;
    }

    public Long getAsientoId() {
        return asientoId;
    }

    public void setAsientoId(Long asientoId) {
        this.asientoId = asientoId;
    }

    public boolean isSeleccionado() {
        return seleccionado;
    }

    public void setSeleccionado(boolean seleccionado) {
        this.seleccionado = seleccionado;
    }

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
