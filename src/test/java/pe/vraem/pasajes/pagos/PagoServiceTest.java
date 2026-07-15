package pe.vraem.pasajes.pagos;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import pe.vraem.pasajes.auth.model.Rol;
import pe.vraem.pasajes.auth.model.Usuario;
import pe.vraem.pasajes.pagos.model.EstadoPago;
import pe.vraem.pasajes.pagos.model.MetodoPago;
import pe.vraem.pasajes.pagos.model.Pago;
import pe.vraem.pasajes.pagos.repository.PagoRepository;
import pe.vraem.pasajes.pagos.service.PagoInvalidoException;
import pe.vraem.pasajes.pagos.service.PagoService;
import pe.vraem.pasajes.reservas.model.EstadoReserva;
import pe.vraem.pasajes.reservas.model.Reserva;
import pe.vraem.pasajes.reservas.repository.ReservaRepository;
import pe.vraem.pasajes.viajes.model.Asiento;
import pe.vraem.pasajes.viajes.model.Camioneta;
import pe.vraem.pasajes.viajes.model.EstadoAsiento;
import pe.vraem.pasajes.viajes.model.Viaje;
import pe.vraem.pasajes.viajes.repository.AsientoRepository;

@ExtendWith(MockitoExtension.class)
class PagoServiceTest {

    @Mock
    private PagoRepository pagoRepository;

    @Mock
    private ReservaRepository reservaRepository;

    @Mock
    private AsientoRepository asientoRepository;

    private PagoService pagoService;
    private Reserva reserva;
    private Asiento asiento;

    @BeforeEach
    void setUp() {
        pagoService = new PagoService(pagoRepository, reservaRepository, asientoRepository);

        Usuario comprador = new Usuario("12345678", "Juan Perez", "juan@example.com", "hash", Rol.PASAJERO);
        Camioneta camioneta = new Camioneta("ABC-123", "Ayacucho - Kimbiri");
        Viaje viaje = new Viaje("Ayacucho", "Kimbiri", LocalDate.now().plusDays(1), LocalTime.of(8, 0), camioneta,
                new BigDecimal("50.00"), 20);
        reserva = new Reserva(comprador, viaje, new BigDecimal("50.00"));
        ReflectionTestUtils.setField(reserva, "id", 1L);

        asiento = new Asiento(viaje, 1);
        asiento.ocupar(reserva, "Ana Quispe", "45678912");
    }

    @Test
    void registraUnPagoPendienteParaUnaReservaPendiente() {
        when(pagoRepository.findByReserva(reserva)).thenReturn(Optional.empty());
        when(pagoRepository.save(any(Pago.class))).thenAnswer(inv -> inv.getArgument(0));

        Pago pago = pagoService.registrarPago(reserva, MetodoPago.YAPE, "OP-12345");

        assertThat(pago.getMetodo()).isEqualTo(MetodoPago.YAPE);
        assertThat(pago.getReferencia()).isEqualTo("OP-12345");
        assertThat(pago.getEstado()).isEqualTo(EstadoPago.PENDIENTE);
    }

    @Test
    void rechazaRegistrarPagoParaUnaReservaNoPendiente() {
        reserva.marcarPagada();

        assertThatThrownBy(() -> pagoService.registrarPago(reserva, MetodoPago.PLIN, "OP-1"))
                .isInstanceOf(PagoInvalidoException.class);
    }

    @Test
    void confirmarPagoTransicionaReservaYAsientosAPagado() {
        Pago pago = new Pago(reserva, MetodoPago.YAPE, "OP-12345");
        when(pagoRepository.findByReserva(reserva)).thenReturn(Optional.of(pago));
        when(pagoRepository.save(any(Pago.class))).thenAnswer(inv -> inv.getArgument(0));
        when(reservaRepository.save(any(Reserva.class))).thenAnswer(inv -> inv.getArgument(0));
        when(asientoRepository.findAllByReserva(reserva)).thenReturn(List.of(asiento));

        pagoService.confirmarPago(reserva);

        assertThat(pago.getEstado()).isEqualTo(EstadoPago.CONFIRMADO);
        assertThat(reserva.getEstado()).isEqualTo(EstadoReserva.PAGADO);
        assertThat(asiento.getEstado()).isEqualTo(EstadoAsiento.PAGADO);
    }
}
