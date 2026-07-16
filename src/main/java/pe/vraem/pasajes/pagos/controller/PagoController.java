package pe.vraem.pasajes.pagos.controller;

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

import pe.vraem.pasajes.auth.model.Usuario;
import pe.vraem.pasajes.auth.service.UsuarioActualProvider;
import pe.vraem.pasajes.pagos.service.PagoInvalidoException;
import pe.vraem.pasajes.pagos.service.PagoService;
import pe.vraem.pasajes.pagos.service.QrCodeGenerator;
import pe.vraem.pasajes.reservas.model.Reserva;
import pe.vraem.pasajes.reservas.service.ReservaService;

@Controller
public class PagoController {

    /** Numero de Yape/Plin al que se debe pagar (no hay integracion real con su API). */
    private static final String NUMERO_YAPE_PLIN = "989516543";

    private final PagoService pagoService;
    private final ReservaService reservaService;
    private final UsuarioActualProvider usuarioActualProvider;
    private final QrCodeGenerator qrCodeGenerator;

    public PagoController(PagoService pagoService, ReservaService reservaService,
            UsuarioActualProvider usuarioActualProvider, QrCodeGenerator qrCodeGenerator) {
        this.pagoService = pagoService;
        this.reservaService = reservaService;
        this.usuarioActualProvider = usuarioActualProvider;
        this.qrCodeGenerator = qrCodeGenerator;
    }

    @GetMapping("/reservas/{id}/pago")
    public String mostrarFormulario(@PathVariable Long id, Authentication authentication, Model model) {
        Reserva reserva = obtenerReservaDelPropietario(id, authentication);
        model.addAttribute("reserva", reserva);
        model.addAttribute("numeroYapePlin", NUMERO_YAPE_PLIN);
        model.addAttribute("qrCodeBase64", generarQr(reserva));
        if (!model.containsAttribute("pagoForm")) {
            model.addAttribute("pagoForm", new PagoForm());
        }
        return "pagos/formulario";
    }

    @PostMapping("/reservas/{id}/pago")
    public String registrarPago(@PathVariable Long id, @Valid @ModelAttribute("pagoForm") PagoForm pagoForm,
            BindingResult bindingResult, Authentication authentication, Model model,
            RedirectAttributes redirectAttributes) {
        Reserva reserva = obtenerReservaDelPropietario(id, authentication);

        if (bindingResult.hasErrors()) {
            model.addAttribute("reserva", reserva);
            model.addAttribute("numeroYapePlin", NUMERO_YAPE_PLIN);
            model.addAttribute("qrCodeBase64", generarQr(reserva));
            return "pagos/formulario";
        }

        try {
            pagoService.registrarPago(reserva, pagoForm.getMetodo(), pagoForm.getReferencia());
        } catch (PagoInvalidoException ex) {
            model.addAttribute("reserva", reserva);
            model.addAttribute("numeroYapePlin", NUMERO_YAPE_PLIN);
            model.addAttribute("qrCodeBase64", generarQr(reserva));
            model.addAttribute("errorGeneral", ex.getMessage());
            return "pagos/formulario";
        }

        redirectAttributes.addFlashAttribute("exito",
                "Pago registrado. Un administrador lo confirmara para habilitar tu boleto.");
        return "redirect:/reservas/" + id;
    }

    @PostMapping("/admin/reservas/{id}/confirmar-pago")
    public String confirmarPago(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        Reserva reserva = reservaService.obtenerPorId(id);
        try {
            pagoService.confirmarPago(reserva);
            redirectAttributes.addFlashAttribute("exito", "Pago confirmado. El boleto ya esta disponible para el pasajero.");
        } catch (PagoInvalidoException ex) {
            redirectAttributes.addFlashAttribute("errorGeneral", ex.getMessage());
        }
        return "redirect:/admin/reservas";
    }

    private String generarQr(Reserva reserva) {
        String contenido = "Yape / Plin " + NUMERO_YAPE_PLIN + " - Reserva " + reserva.getCodigoReserva()
                + " - S/ " + reserva.getMontoTotal();
        return qrCodeGenerator.generarPngBase64(contenido);
    }

    private Reserva obtenerReservaDelPropietario(Long id, Authentication authentication) {
        Reserva reserva = reservaService.obtenerPorId(id);
        Usuario usuarioActual = usuarioActualProvider.obtener(authentication);
        if (!reserva.perteneceA(usuarioActual)) {
            throw new AccessDeniedException("No tienes permiso para pagar esta reserva");
        }
        return reserva;
    }
}
