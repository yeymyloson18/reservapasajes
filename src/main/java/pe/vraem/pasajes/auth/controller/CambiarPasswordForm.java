package pe.vraem.pasajes.auth.controller;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public class CambiarPasswordForm {

    @NotBlank(message = "La nueva contrasena es obligatoria")
    @Pattern(regexp = "^(?=.*[A-Z])(?=.*[0-9]).{8,}$",
            message = "La contrasena debe tener minimo 8 caracteres, al menos una mayuscula y un numero")
    private String nuevaPassword;

    public String getNuevaPassword() {
        return nuevaPassword;
    }

    public void setNuevaPassword(String nuevaPassword) {
        this.nuevaPassword = nuevaPassword;
    }
}
