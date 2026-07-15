package pe.vraem.pasajes.reservas.controller;

import java.util.List;

import jakarta.validation.Valid;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import pe.vraem.pasajes.auth.model.Rol;
import pe.vraem.pasajes.auth.model.Usuario;
import pe.vraem.pasajes.auth.service.UsuarioActualProvider;
import pe.vraem.pasajes.pagos.model.Pago;
import pe.vraem.pasajes.pagos.repository.PagoRepository;
import pe.vraem.pasajes.reservas.model.Reserva;
import pe.vraem.pasajes.reservas.service.AsientoNoDisponibleException;
import pe.vraem.pasajes.reservas.service.ReservaService;
import pe.vraem.pasajes.reservas.service.SeleccionInvalidaException;
import pe.vraem.pasajes.viajes.model.Asiento;
import pe.vraem.pasajes.viajes.model.EstadoAsiento;
import pe.vraem.pasajes.viajes.model.Viaje;
import pe.vraem.pasajes.viajes.repository.AsientoRepository;
import pe.vraem.pasajes.viajes.service.ViajeService;

@Controller
public class ReservaController {

    private final ReservaService reservaService;
    private final ViajeService viajeService;
    private final AsientoRepository asientoRepository;
    private final PagoRepository pagoRepository;
    private final UsuarioActualProvider usuarioActualProvider;

    public ReservaController(ReservaService reservaService, ViajeService viajeService,
            AsientoRepository asientoRepository, PagoRepository pagoRepository,
            UsuarioActualProvider usuarioActualProvider) {
        this.reservaService = reservaService;
        this.viajeService = viajeService;
        this.asientoRepository = asientoRepository;
        this.pagoRepository = pagoRepository;
        this.usuarioActualProvider = usuarioActualProvider;
    }

    @GetMapping("/reservas")
    public String misReservas(Authentication authentication, Model model) {
        Usuario usuarioActual = usuarioActualProvider.obtener(authentication);
        model.addAttribute("reservas", reservaService.listarPorUsuario(usuarioActual));
        return "reservas/mis-reservas";
    }

    @GetMapping("/viajes/{id}/asientos/{asientoId}")
    public String mostrarConfirmacion(@PathVariable Long id, @PathVariable Long asientoId, Model model,
            RedirectAttributes redirectAttributes) {
        Viaje viaje = viajeService.obtenerDetalle(id);
        Asiento asiento = asientoRepository.findById(asientoId).orElse(null);

        if (asiento == null || !asiento.getViaje().getId().equals(id) || asiento.getEstado() != EstadoAsiento.LIBRE) {
            redirectAttributes.addFlashAttribute("errorReserva", "Ese asiento ya no esta disponible.");
            return "redirect:/viajes/" + id;
        }

        model.addAttribute("viaje", viaje);
        model.addAttribute("asiento", asiento);
        if (!model.containsAttribute("confirmarAsientoForm")) {
            model.addAttribute("confirmarAsientoForm", new ConfirmarAsientoForm());
        }
        return "viajes/confirmar-asiento";
    }

    @PostMapping("/viajes/{id}/asientos/{asientoId}")
    public String confirmar(@PathVariable Long id, @PathVariable Long asientoId,
            @Valid @ModelAttribute("confirmarAsientoForm") ConfirmarAsientoForm form, BindingResult bindingResult,
            Authentication authentication, Model model, RedirectAttributes redirectAttributes) {
        Viaje viaje = viajeService.obtenerDetalle(id);

        if (bindingResult.hasErrors()) {
            Asiento asiento = asientoRepository.findById(asientoId).orElse(null);
            model.addAttribute("viaje", viaje);
            model.addAttribute("asiento", asiento);
            return "viajes/confirmar-asiento";
        }

        Usuario comprador = usuarioActualProvider.obtener(authentication);

        try {
            Reserva reserva = reservaService.crearReserva(viaje, comprador, asientoId, form.getNombrePasajero(),
                    form.getDniPasajero());
            redirectAttributes.addFlashAttribute("exito", "Asiento reservado. Completa el pago para confirmar tu boleto.");
            return "redirect:/reservas/" + reserva.getId() + "/pago";
        } catch (SeleccionInvalidaException | AsientoNoDisponibleException ex) {
            redirectAttributes.addFlashAttribute("errorReserva", ex.getMessage());
            return "redirect:/viajes/" + id;
        }
    }

    @GetMapping("/reservas/{id}")
    public String detalle(@PathVariable Long id, Authentication authentication, Model model) {
        Reserva reserva = reservaService.obtenerPorId(id);
        Usuario usuarioActual = usuarioActualProvider.obtener(authentication);

        boolean esAdmin = usuarioActual.getRol() == Rol.ADMIN;
        if (!esAdmin && !reserva.perteneceA(usuarioActual)) {
            throw new AccessDeniedException("No tienes permiso para ver esta reserva");
        }

        List<Asiento> asientos = asientoRepository.findAllByReserva(reserva);
        Pago pago = pagoRepository.findByReserva(reserva).orElse(null);

        model.addAttribute("reserva", reserva);
        model.addAttribute("asientos", asientos);
        model.addAttribute("pago", pago);
        return "reservas/detalle";
    }
}
