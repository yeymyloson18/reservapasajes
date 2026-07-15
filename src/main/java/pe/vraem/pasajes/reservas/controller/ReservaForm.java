package pe.vraem.pasajes.reservas.controller;

import java.util.ArrayList;
import java.util.List;

public class ReservaForm {

    private List<AsientoSeleccionDTO> asientos = new ArrayList<>();

    public List<AsientoSeleccionDTO> getAsientos() {
        return asientos;
    }

    public void setAsientos(List<AsientoSeleccionDTO> asientos) {
        this.asientos = asientos;
    }
}
