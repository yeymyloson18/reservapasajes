package pe.vraem.pasajes.viajes;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import pe.vraem.pasajes.viajes.model.Asiento;
import pe.vraem.pasajes.viajes.model.Camioneta;
import pe.vraem.pasajes.viajes.model.EstadoAsiento;
import pe.vraem.pasajes.viajes.model.Viaje;
import pe.vraem.pasajes.viajes.repository.AsientoRepository;
import pe.vraem.pasajes.viajes.repository.CamionetaRepository;
import pe.vraem.pasajes.viajes.repository.ViajeRepository;
import pe.vraem.pasajes.viajes.service.ViajeInvalidoException;
import pe.vraem.pasajes.viajes.service.ViajeService;

@ExtendWith(MockitoExtension.class)
class ViajeServiceTest {

    @Mock
    private ViajeRepository viajeRepository;

    @Mock
    private AsientoRepository asientoRepository;

    @Mock
    private CamionetaRepository camionetaRepository;

    private ViajeService viajeService;

    private Camioneta camioneta;

    @BeforeEach
    void setUp() {
        viajeService = new ViajeService(viajeRepository, asientoRepository, camionetaRepository);
        camioneta = new Camioneta("ABC-123", "Ayacucho - Kimbiri");
    }

    private Viaje viajeConId(long id, int numeroAsientos) {
        Viaje viaje = new Viaje("Ayacucho", "Kimbiri", LocalDate.now().plusDays(1), LocalTime.of(8, 0), camioneta,
                new BigDecimal("50.00"), numeroAsientos);
        ReflectionTestUtils.setField(viaje, "id", id);
        return viaje;
    }

    @Test
    @SuppressWarnings("unchecked")
    void crearViajeGeneraAsientosLibresSegunElNumeroConfigurado() {
        when(viajeRepository.save(any(Viaje.class))).thenAnswer(inv -> {
            Viaje v = inv.getArgument(0);
            ReflectionTestUtils.setField(v, "id", 1L);
            return v;
        });

        viajeService.crearViaje("Ayacucho", "Kimbiri", LocalDate.now().plusDays(1), LocalTime.of(8, 0), camioneta,
                new BigDecimal("50.00"), 5);

        ArgumentCaptor<List<Asiento>> captor = ArgumentCaptor.forClass(List.class);
        verify(asientoRepository).saveAll(captor.capture());

        List<Asiento> asientos = captor.getValue();
        assertThat(asientos).hasSize(5);
        assertThat(asientos).allMatch(a -> a.getEstado() == EstadoAsiento.LIBRE);
        assertThat(asientos).extracting(Asiento::getNumero).containsExactly(1, 2, 3, 4, 5);
    }

    @Test
    void editarViajeRechazaReducirAsientosPorDebajoDeLosOcupados() {
        Viaje viaje = viajeConId(1L, 3);
        Asiento libre = new Asiento(viaje, 1);
        Asiento reservado = new Asiento(viaje, 2);
        reservado.ocupar(null, "Pasajero", "12345678");
        Asiento pagado = new Asiento(viaje, 3);
        pagado.ocupar(null, "Pasajero2", "87654321");
        pagado.marcarPagado();

        when(viajeRepository.findById(1L)).thenReturn(java.util.Optional.of(viaje));
        when(asientoRepository.findAllByViajeOrderByNumeroAsc(viaje)).thenReturn(List.of(libre, reservado, pagado));

        assertThatThrownBy(() -> viajeService.editarViaje(1L, "Ayacucho", "Kimbiri", viaje.getFecha(), viaje.getHora(),
                camioneta, new BigDecimal("50.00"), 1))
                .isInstanceOf(ViajeInvalidoException.class);

        verify(viajeRepository, never()).save(any(Viaje.class));
    }

    @Test
    void eliminarViajeRechazaSiTieneReservasPagadas() {
        Viaje viaje = viajeConId(1L, 2);

        when(viajeRepository.findById(1L)).thenReturn(java.util.Optional.of(viaje));
        when(asientoRepository.countByViajeAndEstadoIn(viaje, List.of(EstadoAsiento.PAGADO))).thenReturn(1L);

        assertThatThrownBy(() -> viajeService.eliminarViaje(1L))
                .isInstanceOf(ViajeInvalidoException.class);

        verify(viajeRepository, never()).delete(any(Viaje.class));
        verify(asientoRepository, never()).deleteAll(anyList());
    }
}
