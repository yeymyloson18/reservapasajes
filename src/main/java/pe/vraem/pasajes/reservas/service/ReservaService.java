package pe.vraem.pasajes.reservas.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import pe.vraem.pasajes.auth.model.Usuario;
import pe.vraem.pasajes.reservas.model.EstadoReserva;
import pe.vraem.pasajes.reservas.model.Reserva;
import pe.vraem.pasajes.reservas.repository.ReservaRepository;
import pe.vraem.pasajes.viajes.model.Asiento;
import pe.vraem.pasajes.viajes.model.EstadoAsiento;
import pe.vraem.pasajes.viajes.model.Viaje;
import pe.vraem.pasajes.viajes.repository.AsientoRepository;

@Service
public class ReservaService {

    private final ReservaRepository reservaRepository;
    private final AsientoRepository asientoRepository;
    private final int minutosExpiracion;

    public ReservaService(ReservaRepository reservaRepository, AsientoRepository asientoRepository,
            @Value("${app.reservas.minutos-expiracion:30}") int minutosExpiracion) {
        this.reservaRepository = reservaRepository;
        this.asientoRepository = asientoRepository;
        this.minutosExpiracion = minutosExpiracion;
    }

    /**
     * Crea una reserva PENDIENTE para el asiento indicado, bloqueandolo con
     * bloqueo pesimista para evitar que dos reservas concurrentes tomen el mismo
     * asiento (FR-006, FR-007, FR-008, FR-010, FR-013). Cada reserva cubre
     * exactamente un asiento; el monto total es el precio del viaje.
     */
    @Transactional
    public Reserva crearReserva(Viaje viaje, Usuario comprador, Long asientoId, String nombrePasajero,
            String dniPasajero) {
        if (nombrePasajero == null || nombrePasajero.isBlank()) {
            throw new SeleccionInvalidaException("Falta el nombre del pasajero");
        }
        if (dniPasajero == null || !dniPasajero.matches("\\d{8}")) {
            throw new SeleccionInvalidaException("El DNI del pasajero debe tener 8 digitos numericos");
        }

        List<Asiento> asientosBloqueados = asientoRepository.findAllByIdForUpdate(List.of(asientoId));
        if (asientosBloqueados.isEmpty()) {
            throw new AsientoNoDisponibleException("El asiento seleccionado ya no existe");
        }
        Asiento asiento = asientosBloqueados.get(0);
        if (!asiento.getViaje().getId().equals(viaje.getId())) {
            throw new AsientoNoDisponibleException("El asiento no pertenece a este viaje");
        }
        if (asiento.getEstado() != EstadoAsiento.LIBRE) {
            throw new AsientoNoDisponibleException("El asiento " + asiento.getNumero() + " ya no esta disponible");
        }

        Reserva reserva = new Reserva(comprador, viaje, viaje.getPrecio());
        reserva.asignarCodigoReserva(generarCodigoUnico());
        reserva = reservaRepository.save(reserva);

        asiento.ocupar(reserva, nombrePasajero, dniPasajero);
        asientoRepository.save(asiento);

        return reserva;
    }

    private String generarCodigoUnico() {
        String codigo;
        do {
            codigo = UUID.randomUUID().toString().replace("-", "").substring(0, 8).toUpperCase();
        } while (reservaRepository.existsByCodigoReserva(codigo));
        return codigo;
    }

    @Transactional(readOnly = true)
    public Reserva obtenerPorId(Long id) {
        return reservaRepository.findById(id)
                .orElseThrow(() -> new ReservaNoEncontradaException(id));
    }

    @Transactional(readOnly = true)
    public List<Reserva> listarPorUsuario(Usuario usuario) {
        return reservaRepository.findAllByUsuarioOrderByFechaCreacionDesc(usuario);
    }

    /**
     * Libera automaticamente los asientos de reservas PENDIENTE cuyo pago no fue
     * confirmado dentro del plazo configurado (FR-011, SC-003). Se ejecuta cada
     * minuto; no requiere infraestructura en tiempo real.
     */
    @Scheduled(fixedRate = 60_000)
    @Transactional
    public void liberarReservasExpiradas() {
        LocalDateTime limite = LocalDateTime.now().minusMinutes(minutosExpiracion);
        List<Reserva> vencidas = reservaRepository.findAllByEstadoAndFechaCreacionBefore(EstadoReserva.PENDIENTE,
                limite);

        for (Reserva reserva : vencidas) {
            reserva.marcarExpirada();
            List<Asiento> asientos = asientoRepository.findAllByReserva(reserva);
            asientos.forEach(Asiento::liberar);
            asientoRepository.saveAll(asientos);
        }
        reservaRepository.saveAll(vencidas);
    }
}
