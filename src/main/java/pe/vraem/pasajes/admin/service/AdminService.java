package pe.vraem.pasajes.admin.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import pe.vraem.pasajes.reservas.model.EstadoReserva;
import pe.vraem.pasajes.reservas.repository.ReservaRepository;
import pe.vraem.pasajes.viajes.service.ViajeService;

@Service
public class AdminService {

    private final ViajeService viajeService;
    private final ReservaRepository reservaRepository;

    public AdminService(ViajeService viajeService, ReservaRepository reservaRepository) {
        this.viajeService = viajeService;
        this.reservaRepository = reservaRepository;
    }

    /**
     * Resumen operativo para el panel de administracion: viajes activos
     * (fecha igual o posterior a hoy) y reservas pendientes de pago.
     */
    @Transactional(readOnly = true)
    public ResumenAdmin obtenerResumen() {
        long viajesActivos = viajeService.listarDisponibles().size();
        long reservasPendientes = reservaRepository.countByEstado(EstadoReserva.PENDIENTE);
        return new ResumenAdmin(viajesActivos, reservasPendientes);
    }
}
