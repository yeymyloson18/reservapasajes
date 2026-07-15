package pe.vraem.pasajes.reservas.controller;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import pe.vraem.pasajes.reservas.model.Reserva;
import pe.vraem.pasajes.reservas.repository.ReservaRepository;
import pe.vraem.pasajes.viajes.model.Asiento;
import pe.vraem.pasajes.viajes.repository.AsientoRepository;

@Controller
@RequestMapping("/admin/reservas")
public class AdminReservaController {

    private final ReservaRepository reservaRepository;
    private final AsientoRepository asientoRepository;

    public AdminReservaController(ReservaRepository reservaRepository, AsientoRepository asientoRepository) {
        this.reservaRepository = reservaRepository;
        this.asientoRepository = asientoRepository;
    }

    @GetMapping
    public String listar(@RequestParam(required = false) Long viajeId, Model model) {
        List<Reserva> reservas = viajeId != null
                ? reservaRepository.findAllByViaje_IdOrderByFechaCreacionDesc(viajeId)
                : reservaRepository.findAllByOrderByFechaCreacionDesc();

        // Mapa manual (no Collectors.toMap): una reserva puede no tener aun un
        // asiento vinculado en datos de prueba, y toMap no admite valores null.
        Map<Long, Asiento> asientoPorReserva = new HashMap<>();
        for (Reserva reserva : reservas) {
            Asiento asiento = asientoRepository.findAllByReserva(reserva).stream().findFirst().orElse(null);
            asientoPorReserva.put(reserva.getId(), asiento);
        }

        model.addAttribute("reservas", reservas);
        model.addAttribute("asientoPorReserva", asientoPorReserva);
        model.addAttribute("viajeId", viajeId);
        return "admin/listado-reservas";
    }
}
