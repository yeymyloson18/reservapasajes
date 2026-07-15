package pe.vraem.pasajes.viajes.service;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.NOT_FOUND)
public class ViajeNoEncontradoException extends RuntimeException {

    public ViajeNoEncontradoException(Long id) {
        super("No existe el viaje con id " + id);
    }
}
