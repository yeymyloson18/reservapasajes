package pe.vraem.pasajes.pagos.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import pe.vraem.pasajes.reservas.model.Reserva;

@Entity
@Table(name = "pago", uniqueConstraints = @jakarta.persistence.UniqueConstraint(columnNames = "reserva_id"))
public class Pago {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reserva_id", nullable = false, unique = true)
    private Reserva reserva;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private MetodoPago metodo;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EstadoPago estado;

    @NotBlank
    @Column(nullable = false, length = 50)
    private String referencia;

    @Column(name = "motivo_rechazo", length = 255)
    private String motivoRechazo;

    protected Pago() {
        // JPA
    }

    public Pago(Reserva reserva, MetodoPago metodo, String referencia) {
        this.reserva = reserva;
        this.metodo = metodo;
        this.referencia = referencia;
        this.estado = EstadoPago.PENDIENTE;
    }

    public void confirmar() {
        this.estado = EstadoPago.CONFIRMADO;
    }

    public void rechazar(String motivo) {
        this.estado = EstadoPago.RECHAZADO;
        this.motivoRechazo = motivo;
    }

    public Long getId() {
        return id;
    }

    public Reserva getReserva() {
        return reserva;
    }

    public MetodoPago getMetodo() {
        return metodo;
    }

    public EstadoPago getEstado() {
        return estado;
    }

    public String getReferencia() {
        return referencia;
    }

    public String getMotivoRechazo() {
        return motivoRechazo;
    }
}
