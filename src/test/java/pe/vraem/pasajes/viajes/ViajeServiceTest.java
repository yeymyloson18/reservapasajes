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

    private Viaje viajeConId(long id) {
        Viaje viaje = new Viaje("Ayacucho", "Kimbiri", LocalDate.now().plusDays(1), LocalTime.of(8, 0), camioneta,
                new BigDecimal("50.00"), "Carlos Mamani");
        ReflectionTestUtils.setField(viaje, "id", id);
        return viaje;
    }

    @Test
    @SuppressWarnings("unchecked")
    void crearViajeGeneraLosCuatroAsientosFijos() {
        when(viajeRepository.save(any(Viaje.class))).thenAnswer(inv -> {
            Viaje v = inv.getArgument(0);
            ReflectionTestUtils.setField(v, "id", 1L);
            return v;
        });

        viajeService.crearViaje("Ayacucho", "Kimbiri", LocalDate.now().plusDays(1), LocalTime.of(8, 0), camioneta,
                new BigDecimal("50.00"), "Carlos Mamani");

        ArgumentCaptor<List<Asiento>> captor = ArgumentCaptor.forClass(List.class);
        verify(asientoRepository).saveAll(captor.capture());

        List<Asiento> asientos = captor.getValue();
        assertThat(asientos).hasSize(ViajeService.CAPACIDAD_ASIENTOS);
        assertThat(asientos).allMatch(a -> a.getEstado() == EstadoAsiento.LIBRE);
        assertThat(asientos).extracting(Asiento::getNumero).containsExactly(1, 2, 3, 4);
        assertThat(asientos.get(0).esAdelante()).isTrue();
        assertThat(asientos.get(1).esAdelante()).isFalse();
    }

    @Test
    void eliminarViajeRechazaSiTieneReservasPagadas() {
        Viaje viaje = viajeConId(1L);

        when(viajeRepository.findById(1L)).thenReturn(java.util.Optional.of(viaje));
        when(asientoRepository.countByViajeAndEstadoIn(viaje, List.of(EstadoAsiento.PAGADO))).thenReturn(1L);

        assertThatThrownBy(() -> viajeService.eliminarViaje(1L))
                .isInstanceOf(ViajeInvalidoException.class);

        verify(viajeRepository, never()).delete(any(Viaje.class));
        verify(asientoRepository, never()).deleteAll(anyList());
    }

    @Test
    void archivarViajeRechazaSiQuedanAsientosLibres() {
        Viaje viaje = viajeConId(1L);

        when(viajeRepository.findById(1L)).thenReturn(java.util.Optional.of(viaje));
        when(asientoRepository.countByViajeAndEstadoIn(viaje, List.of(EstadoAsiento.LIBRE))).thenReturn(1L);

        assertThatThrownBy(() -> viajeService.archivarViaje(1L))
                .isInstanceOf(ViajeInvalidoException.class);

        assertThat(viaje.isArchivado()).isFalse();
        verify(viajeRepository, never()).save(viaje);
    }

    @Test
    void archivarViajeMarcaArchivadoCuandoEstaCompleto() {
        Viaje viaje = viajeConId(1L);

        when(viajeRepository.findById(1L)).thenReturn(java.util.Optional.of(viaje));
        when(asientoRepository.countByViajeAndEstadoIn(viaje, List.of(EstadoAsiento.LIBRE))).thenReturn(0L);

        viajeService.archivarViaje(1L);

        assertThat(viaje.isArchivado()).isTrue();
        verify(viajeRepository).save(viaje);
    }
}
