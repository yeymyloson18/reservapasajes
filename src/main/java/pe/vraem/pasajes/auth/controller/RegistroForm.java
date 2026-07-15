package pe.vraem.pasajes.auth.controller;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public class RegistroForm {

    @NotBlank(message = "El DNI es obligatorio")
    @Pattern(regexp = "\\d{8}", message = "El DNI debe tener exactamente 8 digitos numericos")
    private String dni;

    @NotBlank(message = "El nombre es obligatorio")
    @Pattern(regexp = "[\\p{L} ]{2,}", message = "El nombre debe tener solo letras y espacios, minimo 2 caracteres")
    private String nombre;

    @NotBlank(message = "El email es obligatorio")
    @Email(message = "Ingresa un email valido")
    private String email;

    @NotBlank(message = "La contrasena es obligatoria")
    @Pattern(regexp = "^(?=.*[A-Z])(?=.*[0-9]).{8,}$",
            message = "La contrasena debe tener minimo 8 caracteres, al menos una mayuscula y un numero")
    private String password;

    public String getDni() {
        return dni;
    }

    public void setDni(String dni) {
        this.dni = dni;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }
}
