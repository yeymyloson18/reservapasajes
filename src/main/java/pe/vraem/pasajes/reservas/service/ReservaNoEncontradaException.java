package pe.vraem.pasajes.reservas.service;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.NOT_FOUND)
public class ReservaNoEncontradaException extends RuntimeException {

    public ReservaNoEncontradaException(Long id) {
        super("No existe la reserva con id " + id);
    }
}
