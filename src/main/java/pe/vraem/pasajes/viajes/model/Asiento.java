package pe.vraem.pasajes.viajes.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import pe.vraem.pasajes.reservas.model.Reserva;

@Entity
@Table(name = "asiento", uniqueConstraints = @UniqueConstraint(columnNames = { "viaje_id", "numero" }))
public class Asiento {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "viaje_id", nullable = false)
    private Viaje viaje;

    @Positive
    @Column(nullable = false)
    private int numero;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EstadoAsiento estado;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reserva_id")
    private Reserva reserva;

    @Column(name = "nombre_pasajero", length = 150)
    private String nombrePasajero;

    @Column(name = "dni_pasajero", length = 8)
    private String dniPasajero;

    protected Asiento() {
        // JPA
    }

    public Asiento(Viaje viaje, int numero) {
        this.viaje = viaje;
        this.numero = numero;
        this.estado = EstadoAsiento.LIBRE;
    }

    public void ocupar(Reserva reserva, String nombrePasajero, String dniPasajero) {
        this.reserva = reserva;
        this.nombrePasajero = nombrePasajero;
        this.dniPasajero = dniPasajero;
        this.estado = EstadoAsiento.RESERVADO;
    }

    public void marcarPagado() {
        this.estado = EstadoAsiento.PAGADO;
    }

    public void liberar() {
        this.reserva = null;
        this.nombrePasajero = null;
        this.dniPasajero = null;
        this.estado = EstadoAsiento.LIBRE;
    }

    public Long getId() {
        return id;
    }

    public Viaje getViaje() {
        return viaje;
    }

    public int getNumero() {
        return numero;
    }

    public EstadoAsiento getEstado() {
        return estado;
    }

    public Reserva getReserva() {
        return reserva;
    }

    public String getNombrePasajero() {
        return nombrePasajero;
    }

    public String getDniPasajero() {
        return dniPasajero;
    }

    /**
     * La camioneta tiene 4 asientos fijos para pasajeros: el asiento 1 va
     * adelante (junto al chofer) y los asientos 2-4 van juntos atras.
     */
    public boolean esAdelante() {
        return numero == 1;
    }
}
