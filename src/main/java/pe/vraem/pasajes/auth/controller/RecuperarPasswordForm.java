package pe.vraem.pasajes.auth.controller;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public class RecuperarPasswordForm {

    @NotBlank(message = "El email es obligatorio")
    @Email(message = "Ingresa un email valido")
    private String email;

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }
}
