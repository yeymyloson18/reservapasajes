package pe.vraem.pasajes.reservas.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import pe.vraem.pasajes.reservas.repository.ReservaRepository;

@Controller
@RequestMapping("/admin/reservas")
public class AdminReservaController {

    private final ReservaRepository reservaRepository;

    public AdminReservaController(ReservaRepository reservaRepository) {
        this.reservaRepository = reservaRepository;
    }

    @GetMapping
    public String listar(Model model) {
        model.addAttribute("reservas", reservaRepository.findAllByOrderByFechaCreacionDesc());
        return "admin/listado-reservas";
    }
}
