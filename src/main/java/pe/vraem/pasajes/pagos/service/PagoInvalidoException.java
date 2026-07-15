package pe.vraem.pasajes.pagos.service;

public class PagoInvalidoException extends RuntimeException {

    public PagoInvalidoException(String message) {
        super(message);
    }
}
