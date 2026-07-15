package pe.vraem.pasajes.reservas;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import pe.vraem.pasajes.auth.model.Rol;
import pe.vraem.pasajes.auth.model.Usuario;
import pe.vraem.pasajes.reservas.model.EstadoReserva;
import pe.vraem.pasajes.reservas.model.Reserva;
import pe.vraem.pasajes.reservas.repository.ReservaRepository;
import pe.vraem.pasajes.reservas.service.AsientoNoDisponibleException;
import pe.vraem.pasajes.reservas.service.ReservaService;
import pe.vraem.pasajes.reservas.service.SeleccionInvalidaException;
import pe.vraem.pasajes.viajes.model.Asiento;
import pe.vraem.pasajes.viajes.model.Camioneta;
import pe.vraem.pasajes.viajes.model.EstadoAsiento;
import pe.vraem.pasajes.viajes.model.Viaje;
import pe.vraem.pasajes.viajes.repository.AsientoRepository;

@ExtendWith(MockitoExtension.class)
class ReservaServiceTest {

    @Mock
    private ReservaRepository reservaRepository;

    @Mock
    private AsientoRepository asientoRepository;

    private ReservaService reservaService;

    private Usuario comprador;
    private Viaje viaje;

    @BeforeEach
    void setUp() {
        reservaService = new ReservaService(reservaRepository, asientoRepository, 30);

        comprador = new Usuario("12345678", "Juan Perez", "juan@example.com", "hash", Rol.PASAJERO);
        ReflectionTestUtils.setField(comprador, "id", 1L);

        Camioneta camioneta = new Camioneta("ABC-123", "Ayacucho - Kimbiri");
        viaje = new Viaje("Ayacucho", "Kimbiri", LocalDate.now().plusDays(1), LocalTime.of(8, 0), camioneta,
                new BigDecimal("50.00"), "Carlos Mamani");
        ReflectionTestUtils.setField(viaje, "id", 10L);
    }

    private Asiento asientoLibre(long id, int numero) {
        Asiento asiento = new Asiento(viaje, numero);
        ReflectionTestUtils.setField(asiento, "id", id);
        return asiento;
    }

    @Test
    void creaReservaCalculaMontoYOcupaElAsiento() {
        Asiento asiento = asientoLibre(100L, 1);

        when(asientoRepository.findAllByIdForUpdate(List.of(100L))).thenReturn(List.of(asiento));
        when(reservaRepository.existsByCodigoReserva(anyString())).thenReturn(false);
        when(reservaRepository.save(any(Reserva.class))).thenAnswer(inv -> inv.getArgument(0));

        Reserva reserva = reservaService.crearReserva(viaje, comprador, 100L, "Ana Quispe", "45678912");

        assertThat(reserva.getMontoTotal()).isEqualByComparingTo("50.00");
        assertThat(reserva.getEstado()).isEqualTo(EstadoReserva.PENDIENTE);
        assertThat(reserva.getCodigoReserva()).isNotBlank();
        assertThat(asiento.getEstado()).isEqualTo(EstadoAsiento.RESERVADO);
        assertThat(asiento.getNombrePasajero()).isEqualTo("Ana Quispe");
        assertThat(asiento.getDniPasajero()).isEqualTo("45678912");
    }

    @Test
    void rechazaNombrePasajeroVacio() {
        assertThatThrownBy(() -> reservaService.crearReserva(viaje, comprador, 100L, " ", "45678912"))
                .isInstanceOf(SeleccionInvalidaException.class);
    }

    @Test
    void rechazaDniPasajeroInvalido() {
        assertThatThrownBy(() -> reservaService.crearReserva(viaje, comprador, 100L, "Ana Quispe", "123"))
                .isInstanceOf(SeleccionInvalidaException.class);
    }

    @Test
    void rechazaAsientoQueYaNoEstaLibre() {
        Asiento asientoOcupado = asientoLibre(100L, 1);
        asientoOcupado.ocupar(null, "Otro Pasajero", "11111111");

        when(asientoRepository.findAllByIdForUpdate(List.of(100L))).thenReturn(List.of(asientoOcupado));

        assertThatThrownBy(() -> reservaService.crearReserva(viaje, comprador, 100L, "Ana Quispe", "45678912"))
                .isInstanceOf(AsientoNoDisponibleException.class);
    }

    @Test
    void liberaReservasExpiradasYDejaLosAsientosLibres() {
        Reserva reservaVencida = new Reserva(comprador, viaje, new BigDecimal("50.00"));
        ReflectionTestUtils.setField(reservaVencida, "id", 5L);

        Asiento asiento = asientoLibre(102L, 3);
        asiento.ocupar(reservaVencida, "Pedro Lima", "87654321");

        when(reservaRepository.findAllByEstadoAndFechaCreacionBefore(eq(EstadoReserva.PENDIENTE),
                any(LocalDateTime.class))).thenReturn(List.of(reservaVencida));
        when(asientoRepository.findAllByReserva(reservaVencida)).thenReturn(List.of(asiento));

        reservaService.liberarReservasExpiradas();

        assertThat(reservaVencida.getEstado()).isEqualTo(EstadoReserva.EXPIRADA);
        assertThat(asiento.getEstado()).isEqualTo(EstadoAsiento.LIBRE);
        assertThat(asiento.getReserva()).isNull();
    }
}
