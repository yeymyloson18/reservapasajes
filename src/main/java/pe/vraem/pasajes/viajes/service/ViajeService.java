package pe.vraem.pasajes.viajes.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
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

    /**
     * La camioneta tiene 4 asientos fijos para pasajeros: 1 adelante (junto al
     * chofer, numero 1) y 3 juntos atras (numeros 2-4). No es configurable.
     */
    public static final int CAPACIDAD_ASIENTOS = 4;

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

    /**
     * Lista todos los viajes (pasados y futuros) para la gestion del ADMIN.
     */
    @Transactional(readOnly = true)
    public List<Viaje> listarTodos() {
        return viajeRepository.findAllByOrderByFechaAscHoraAsc();
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
     * Cuenta los asientos libres de un viaje, para saber si esta "completo" (sin
     * asientos disponibles) al listarlo.
     */
    @Transactional(readOnly = true)
    public long contarAsientosLibres(Viaje viaje) {
        return asientoRepository.countByViajeAndEstadoIn(viaje, List.of(EstadoAsiento.LIBRE));
    }

    /**
     * Crea un viaje y genera automaticamente sus 4 asientos fijos (FR-014).
     */
    @Transactional
    public Viaje crearViaje(String origen, String destino, LocalDate fecha, LocalTime hora, Camioneta camioneta,
            BigDecimal precio, String chofer) {
        Viaje viaje = viajeRepository.save(new Viaje(origen, destino, fecha, hora, camioneta, precio, chofer));

        List<Asiento> asientos = new ArrayList<>();
        for (int numero = 1; numero <= CAPACIDAD_ASIENTOS; numero++) {
            asientos.add(new Asiento(viaje, numero));
        }
        asientoRepository.saveAll(asientos);

        return viaje;
    }

    /**
     * Edita un viaje existente. La cantidad de asientos es fija (4) y no se
     * modifica al editar.
     */
    @Transactional
    public Viaje editarViaje(Long id, String origen, String destino, LocalDate fecha, LocalTime hora,
            Camioneta camioneta, BigDecimal precio, String chofer) {
        Viaje viaje = obtenerDetalle(id);

        viaje.setOrigen(origen);
        viaje.setDestino(destino);
        viaje.setFecha(fecha);
        viaje.setHora(hora);
        viaje.setCamioneta(camioneta);
        viaje.setPrecio(precio);
        viaje.setChofer(chofer);

        return viajeRepository.save(viaje);
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
