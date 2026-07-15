package pe.vraem.pasajes.pagos.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import pe.vraem.pasajes.pagos.model.EstadoPago;
import pe.vraem.pasajes.pagos.model.MetodoPago;
import pe.vraem.pasajes.pagos.model.Pago;
import pe.vraem.pasajes.pagos.repository.PagoRepository;
import pe.vraem.pasajes.reservas.model.EstadoReserva;
import pe.vraem.pasajes.reservas.model.Reserva;
import pe.vraem.pasajes.reservas.repository.ReservaRepository;
import pe.vraem.pasajes.viajes.model.Asiento;
import pe.vraem.pasajes.viajes.repository.AsientoRepository;

@Service
public class PagoService {

    private final PagoRepository pagoRepository;
    private final ReservaRepository reservaRepository;
    private final AsientoRepository asientoRepository;

    public PagoService(PagoRepository pagoRepository, ReservaRepository reservaRepository,
            AsientoRepository asientoRepository) {
        this.pagoRepository = pagoRepository;
        this.reservaRepository = reservaRepository;
        this.asientoRepository = asientoRepository;
    }

    /**
     * Registra el metodo y referencia de pago indicados por el pasajero (FR-009).
     * La Reserva permanece en estado PENDIENTE hasta que el ADMIN confirme el pago.
     */
    @Transactional
    public Pago registrarPago(Reserva reserva, MetodoPago metodo, String referencia) {
        if (reserva.getEstado() != EstadoReserva.PENDIENTE) {
            throw new PagoInvalidoException("Solo se puede registrar un pago para una reserva pendiente");
        }

        Pago pago = pagoRepository.findByReserva(reserva).orElse(null);
        if (pago == null) {
            pago = new Pago(reserva, metodo, referencia);
        }
        return pagoRepository.save(pago);
    }

    /**
     * Confirma manualmente el pago (accion del ADMIN) y transiciona la Reserva y
     * sus Asientos a PAGADO (FR-012).
     */
    @Transactional
    public void confirmarPago(Reserva reserva) {
        Pago pago = pagoRepository.findByReserva(reserva)
                .orElseThrow(() -> new PagoInvalidoException("La reserva no tiene un pago registrado"));

        pago.confirmar();
        pagoRepository.save(pago);

        reserva.marcarPagada();
        reservaRepository.save(reserva);

        List<Asiento> asientos = asientoRepository.findAllByReserva(reserva);
        asientos.forEach(Asiento::marcarPagado);
        asientoRepository.saveAll(asientos);
    }
}
