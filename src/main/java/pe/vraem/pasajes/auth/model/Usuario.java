package pe.vraem.pasajes.auth.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

@Entity
@Table(name = "usuario", uniqueConstraints = {
        @jakarta.persistence.UniqueConstraint(columnNames = "dni"),
        @jakarta.persistence.UniqueConstraint(columnNames = "email")
})
public class Usuario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    @Pattern(regexp = "\\d{8}", message = "El DNI debe tener exactamente 8 digitos numericos")
    @Column(nullable = false, length = 8, unique = true)
    private String dni;

    @NotBlank
    @Column(nullable = false, length = 150)
    private String nombre;

    @NotBlank
    @Email
    @Column(nullable = false, length = 150, unique = true)
    private String email;

    @NotBlank
    @Column(name = "password_hash", nullable = false, length = 255)
    private String passwordHash;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Rol rol;

    protected Usuario() {
        // JPA
    }

    public Usuario(String dni, String nombre, String email, String passwordHash, Rol rol) {
        this.dni = dni;
        this.nombre = nombre;
        this.email = email;
        this.passwordHash = passwordHash;
        this.rol = rol;
    }

    public Long getId() {
        return id;
    }

    public String getDni() {
        return dni;
    }

    public String getNombre() {
        return nombre;
    }

    public String getEmail() {
        return email;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public void actualizarPasswordHash(String nuevoPasswordHash) {
        this.passwordHash = nuevoPasswordHash;
    }

    public Rol getRol() {
        return rol;
    }
}
