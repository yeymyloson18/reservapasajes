package pe.vraem.pasajes.viajes.controller;

import pe.vraem.pasajes.viajes.model.Viaje;

/**
 * Vista de un viaje junto con el numero de asientos libres, para poder
 * mostrar "COMPLETO" en el listado cuando ya no queden disponibles.
 */
public record ViajeConDisponibilidad(Viaje viaje, long asientosLibres) {

    public boolean completo() {
        return asientosLibres <= 0;
    }
}
