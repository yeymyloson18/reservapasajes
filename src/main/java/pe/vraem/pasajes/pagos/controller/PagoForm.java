package pe.vraem.pasajes.pagos.controller;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import pe.vraem.pasajes.pagos.model.MetodoPago;

public class PagoForm {

    @NotNull(message = "Selecciona un metodo de pago")
    private MetodoPago metodo;

    @NotBlank(message = "Ingresa la referencia u operacion del pago")
    private String referencia;

    public MetodoPago getMetodo() {
        return metodo;
    }

    public void setMetodo(MetodoPago metodo) {
        this.metodo = metodo;
    }

    public String getReferencia() {
        return referencia;
    }

    public void setReferencia(String referencia) {
        this.referencia = referencia;
    }
}
