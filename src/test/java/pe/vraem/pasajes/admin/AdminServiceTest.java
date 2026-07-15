package pe.vraem.pasajes.admin;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import pe.vraem.pasajes.admin.service.AdminService;
import pe.vraem.pasajes.admin.service.ResumenAdmin;
import pe.vraem.pasajes.reservas.model.EstadoReserva;
import pe.vraem.pasajes.reservas.repository.ReservaRepository;
import pe.vraem.pasajes.viajes.model.Camioneta;
import pe.vraem.pasajes.viajes.model.Viaje;
import pe.vraem.pasajes.viajes.service.ViajeService;

@ExtendWith(MockitoExtension.class)
class AdminServiceTest {

    @Mock
    private ViajeService viajeService;

    @Mock
    private ReservaRepository reservaRepository;

    private AdminService adminService;

    @BeforeEach
    void setUp() {
        adminService = new AdminService(viajeService, reservaRepository);
    }

    @Test
    void calculaElResumenDeViajesActivosYReservasPendientes() {
        Camioneta camioneta = new Camioneta("ABC-123", "Ayacucho - Kimbiri");
        Viaje viaje1 = new Viaje("Ayacucho", "Kimbiri", LocalDate.now().plusDays(1), LocalTime.of(8, 0), camioneta,
                new BigDecimal("50.00"), 10, "Carlos Mamani");
        Viaje viaje2 = new Viaje("Ayacucho", "Pichari", LocalDate.now().plusDays(2), LocalTime.of(9, 0), camioneta,
                new BigDecimal("55.00"), 10, "Carlos Mamani");

        when(viajeService.listarDisponibles()).thenReturn(List.of(viaje1, viaje2));
        when(reservaRepository.countByEstado(EstadoReserva.PENDIENTE)).thenReturn(4L);

        ResumenAdmin resumen = adminService.obtenerResumen();

        assertThat(resumen.viajesActivos()).isEqualTo(2);
        assertThat(resumen.reservasPendientes()).isEqualTo(4);
    }
}
