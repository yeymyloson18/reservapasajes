package pe.vraem.pasajes.viajes.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import pe.vraem.pasajes.viajes.model.Asiento;
import pe.vraem.pasajes.viajes.model.Camioneta;
import pe.vraem.pasajes.viajes.model.EstadoAsiento;
import pe.vraem.pasajes.viajes.model.Viaje;
import pe.vraem.pasajes.viajes.repository.AsientoRepository;
import pe.vraem.pasajes.viajes.repository.CamionetaRepository;
import pe.vraem.pasajes.viajes.repository.ViajeRepository;

@Service
public class ViajeService {

    private final ViajeRepository viajeRepository;
    private final AsientoRepository asientoRepository;
    private final CamionetaRepository camionetaRepository;

    public ViajeService(ViajeRepository viajeRepository, AsientoRepository asientoRepository,
            CamionetaRepository camionetaRepository) {
        this.viajeRepository = viajeRepository;
        this.asientoRepository = asientoRepository;
        this.camionetaRepository = camionetaRepository;
    }

    /**
     * Lista los viajes disponibles (fecha igual o posterior a hoy) para los pasajeros (FR-004).
     */
    @Transactional(readOnly = true)
    public List<Viaje> listarDisponibles() {
        LocalDate hoy = LocalDate.now();
        return viajeRepository.findAllByOrderByFechaAscHoraAsc().stream()
                .filter(viaje -> !viaje.getFecha().isBefore(hoy))
                .toList();
    }

    @Transactional(readOnly = true)
    public Viaje obtenerDetalle(Long id) {
        return viajeRepository.findById(id)
                .orElseThrow(() -> new ViajeNoEncontradoException(id));
    }

    /**
     * Obtiene una camioneta existente por placa, o la registra si es la primera vez
     * que se usa (alta rapida desde el formulario de viaje, FR-014).
     */
    @Transactional
    public Camioneta obtenerOCrearCamioneta(String placa, String ruta) {
        return camionetaRepository.findByPlaca(placa)
                .orElseGet(() -> camionetaRepository.save(new Camioneta(placa, ruta)));
    }

    /**
     * Crea un viaje y genera automaticamente sus asientos (FR-014).
     */
    @Transactional
    public Viaje crearViaje(String origen, String destino, LocalDate fecha, LocalTime hora, Camioneta camioneta,
            BigDecimal precio, int numeroAsientos) {
        Viaje viaje = viajeRepository.save(new Viaje(origen, destino, fecha, hora, camioneta, precio, numeroAsientos));

        List<Asiento> asientos = new ArrayList<>();
        for (int numero = 1; numero <= numeroAsientos; numero++) {
            asientos.add(new Asiento(viaje, numero));
        }
        asientoRepository.saveAll(asientos);

        return viaje;
    }

    /**
     * Edita un viaje existente. Si se reduce el numero de asientos, rechaza el
     * cambio cuando ya hay asientos reservados o pagados por encima del nuevo
     * limite (FR-015).
     */
    @Transactional
    public Viaje editarViaje(Long id, String origen, String destino, LocalDate fecha, LocalTime hora,
            Camioneta camioneta, BigDecimal precio, int numeroAsientos) {
        Viaje viaje = obtenerDetalle(id);

        ajustarNumeroDeAsientos(viaje, numeroAsientos);

        viaje.setOrigen(origen);
        viaje.setDestino(destino);
        viaje.setFecha(fecha);
        viaje.setHora(hora);
        viaje.setCamioneta(camioneta);
        viaje.setPrecio(precio);
        viaje.setNumeroAsientos(numeroAsientos);

        return viajeRepository.save(viaje);
    }

    private void ajustarNumeroDeAsientos(Viaje viaje, int nuevoNumeroAsientos) {
        List<Asiento> actuales = asientoRepository.findAllByViajeOrderByNumeroAsc(viaje);
        int actualCount = actuales.size();

        if (nuevoNumeroAsientos > actualCount) {
            int maxNumero = actuales.stream().mapToInt(Asiento::getNumero).max().orElse(0);
            List<Asiento> nuevos = new ArrayList<>();
            for (int i = 1; i <= nuevoNumeroAsientos - actualCount; i++) {
                nuevos.add(new Asiento(viaje, maxNumero + i));
            }
            asientoRepository.saveAll(nuevos);
        } else if (nuevoNumeroAsientos < actualCount) {
            long ocupados = actuales.stream().filter(a -> a.getEstado() != EstadoAsiento.LIBRE).count();
            if (ocupados > nuevoNumeroAsientos) {
                throw new ViajeInvalidoException(
                        "No se puede reducir el numero de asientos por debajo de los ya reservados o pagados");
            }
            List<Asiento> libresDescendente = actuales.stream()
                    .filter(a -> a.getEstado() == EstadoAsiento.LIBRE)
                    .sorted(Comparator.comparingInt(Asiento::getNumero).reversed())
                    .toList();
            int aEliminar = actualCount - nuevoNumeroAsientos;
            asientoRepository.deleteAll(libresDescendente.subList(0, aEliminar));
        }
    }

    /**
     * Elimina un viaje, salvo que tenga reservas pagadas asociadas (FR-016).
     */
    @Transactional
    public void eliminarViaje(Long id) {
        Viaje viaje = obtenerDetalle(id);

        long pagados = asientoRepository.countByViajeAndEstadoIn(viaje, List.of(EstadoAsiento.PAGADO));
        if (pagados > 0) {
            throw new ViajeInvalidoException("No se puede eliminar un viaje con reservas pagadas");
        }

        asientoRepository.deleteAll(asientoRepository.findAllByViajeOrderByNumeroAsc(viaje));
        viajeRepository.delete(viaje);
    }
}
