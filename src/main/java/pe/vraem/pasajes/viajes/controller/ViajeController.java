package pe.vraem.pasajes.viajes.controller;

import java.util.List;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import pe.vraem.pasajes.reservas.controller.AsientoSeleccionDTO;
import pe.vraem.pasajes.reservas.controller.ReservaForm;
import pe.vraem.pasajes.viajes.model.Asiento;
import pe.vraem.pasajes.viajes.model.Viaje;
import pe.vraem.pasajes.viajes.repository.AsientoRepository;
import pe.vraem.pasajes.viajes.service.ViajeService;

@Controller
public class ViajeController {

    private final ViajeService viajeService;
    private final AsientoRepository asientoRepository;

    public ViajeController(ViajeService viajeService, AsientoRepository asientoRepository) {
        this.viajeService = viajeService;
        this.asientoRepository = asientoRepository;
    }

    @GetMapping("/viajes")
    public String listar(Model model) {
        model.addAttribute("viajes", viajeService.listarDisponibles());
        return "viajes/lista";
    }

    @GetMapping("/viajes/{id}")
    public String detalle(@PathVariable Long id, Model model) {
        Viaje viaje = viajeService.obtenerDetalle(id);
        List<Asiento> asientos = asientoRepository.findAllByViajeOrderByNumeroAsc(viaje);

        ReservaForm reservaForm = new ReservaForm();
        asientos.forEach(asiento -> reservaForm.getAsientos().add(new AsientoSeleccionDTO(asiento.getId())));

        model.addAttribute("viaje", viaje);
        model.addAttribute("asientos", asientos);
        model.addAttribute("reservaForm", reservaForm);
        return "viajes/detalle";
    }
}
