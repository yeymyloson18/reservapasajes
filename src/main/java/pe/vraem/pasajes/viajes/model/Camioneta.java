package pe.vraem.pasajes.viajes.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;

@Entity
@Table(name = "camioneta", uniqueConstraints = @jakarta.persistence.UniqueConstraint(columnNames = "placa"))
public class Camioneta {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    @Column(nullable = false, length = 10, unique = true)
    private String placa;

    @NotBlank
    @Column(nullable = false, length = 150)
    private String ruta;

    protected Camioneta() {
        // JPA
    }

    public Camioneta(String placa, String ruta) {
        this.placa = placa;
        this.ruta = ruta;
    }

    public Long getId() {
        return id;
    }

    public String getPlaca() {
        return placa;
    }

    public String getRuta() {
        return ruta;
    }
}
