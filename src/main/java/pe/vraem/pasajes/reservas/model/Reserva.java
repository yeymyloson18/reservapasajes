package pe.vraem.pasajes.reservas.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

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
import jakarta.validation.constraints.NotNull;

import pe.vraem.pasajes.auth.model.Usuario;
import pe.vraem.pasajes.viajes.model.Viaje;

@Entity
@Table(name = "reserva", uniqueConstraints = @jakarta.persistence.UniqueConstraint(columnNames = "codigo_reserva"))
public class Reserva {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "viaje_id", nullable = false)
    private Viaje viaje;

    @NotNull
    @Column(name = "monto_total", nullable = false, precision = 10, scale = 2)
    private BigDecimal montoTotal;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EstadoReserva estado;

    @NotNull
    @Column(name = "fecha_creacion", nullable = false)
    private LocalDateTime fechaCreacion;

    @Column(name = "codigo_reserva", length = 8, unique = true)
    private String codigoReserva;

    protected Reserva() {
        // JPA
    }

    public Reserva(Usuario usuario, Viaje viaje, BigDecimal montoTotal) {
        this.usuario = usuario;
        this.viaje = viaje;
        this.montoTotal = montoTotal;
        this.estado = EstadoReserva.PENDIENTE;
        this.fechaCreacion = LocalDateTime.now();
    }

    public void asignarCodigoReserva(String codigo) {
        this.codigoReserva = codigo;
    }

    public void marcarPagada() {
        this.estado = EstadoReserva.PAGADO;
    }

    public void marcarExpirada() {
        this.estado = EstadoReserva.EXPIRADA;
    }

    public void marcarRechazada() {
        this.estado = EstadoReserva.RECHAZADA;
    }

    public boolean perteneceA(Usuario usuario) {
        return this.usuario.getId().equals(usuario.getId());
    }

    public Long getId() {
        return id;
    }

    public Usuario getUsuario() {
        return usuario;
    }

    public Viaje getViaje() {
        return viaje;
    }

    public BigDecimal getMontoTotal() {
        return montoTotal;
    }

    public EstadoReserva getEstado() {
        return estado;
    }

    public LocalDateTime getFechaCreacion() {
        return fechaCreacion;
    }

    public String getCodigoReserva() {
        return codigoReserva;
    }
}
