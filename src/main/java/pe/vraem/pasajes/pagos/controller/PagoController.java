package pe.vraem.pasajes.pagos.controller;

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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import pe.vraem.pasajes.auth.model.Usuario;
import pe.vraem.pasajes.auth.service.UsuarioActualProvider;
import pe.vraem.pasajes.pagos.model.MetodoPago;
import pe.vraem.pasajes.pagos.service.PagoInvalidoException;
import pe.vraem.pasajes.pagos.service.PagoService;
import pe.vraem.pasajes.pagos.service.QrCodeGenerator;
import pe.vraem.pasajes.reservas.controller.ConfirmarAsientoForm;
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
public class PagoController {

    /** Numero de Yape/Plin al que se debe pagar (no hay integracion real con su API). */
    private static final String NUMERO_YAPE_PLIN = "989516543";

    /** Referencia fija para pagos en efectivo (no existe un numero de operacion real). */
    private static final String REFERENCIA_EFECTIVO = "EFECTIVO";

    private final PagoService pagoService;
    private final ReservaService reservaService;
    private final UsuarioActualProvider usuarioActualProvider;
    private final QrCodeGenerator qrCodeGenerator;
    private final ViajeService viajeService;
    private final AsientoRepository asientoRepository;

    public PagoController(PagoService pagoService, ReservaService reservaService,
            UsuarioActualProvider usuarioActualProvider, QrCodeGenerator qrCodeGenerator,
            ViajeService viajeService, AsientoRepository asientoRepository) {
        this.pagoService = pagoService;
        this.reservaService = reservaService;
        this.usuarioActualProvider = usuarioActualProvider;
        this.qrCodeGenerator = qrCodeGenerator;
        this.viajeService = viajeService;
        this.asientoRepository = asientoRepository;
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

    @PostMapping("/admin/reservas/{id}/rechazar-pago")
    public String rechazarPago(@PathVariable Long id, @RequestParam(required = false) String motivo,
            RedirectAttributes redirectAttributes) {
        Reserva reserva = reservaService.obtenerPorId(id);
        try {
            pagoService.rechazarPago(reserva, motivo != null && !motivo.isBlank() ? motivo : null);
            redirectAttributes.addFlashAttribute("exito", "Pago rechazado. El asiento quedo libre nuevamente.");
        } catch (PagoInvalidoException ex) {
            redirectAttributes.addFlashAttribute("errorGeneral", ex.getMessage());
        }
        return "redirect:/admin/reservas";
    }

    @GetMapping("/admin/viajes/{id}/venta-efectivo")
    public String mostrarMapaVentaEfectivo(@PathVariable Long id, Model model) {
        Viaje viaje = viajeService.obtenerDetalle(id);
        List<Asiento> asientos = asientoRepository.findAllByViajeOrderByNumeroAsc(viaje);

        model.addAttribute("viaje", viaje);
        model.addAttribute("asientoAdelante", asientos.stream().filter(Asiento::esAdelante).findFirst().orElse(null));
        model.addAttribute("asientosAtras", asientos.stream().filter(a -> !a.esAdelante()).toList());
        return "admin/venta-efectivo";
    }

    @GetMapping("/admin/viajes/{id}/asientos/{asientoId}/venta-efectivo")
    public String mostrarConfirmacionVentaEfectivo(@PathVariable Long id, @PathVariable Long asientoId, Model model,
            RedirectAttributes redirectAttributes) {
        Viaje viaje = viajeService.obtenerDetalle(id);
        Asiento asiento = asientoRepository.findById(asientoId).orElse(null);

        if (asiento == null || !asiento.getViaje().getId().equals(id) || asiento.getEstado() != EstadoAsiento.LIBRE) {
            redirectAttributes.addFlashAttribute("errorGeneral", "Ese asiento ya no esta disponible.");
            return "redirect:/admin/viajes/" + id + "/venta-efectivo";
        }

        model.addAttribute("viaje", viaje);
        model.addAttribute("asiento", asiento);
        if (!model.containsAttribute("confirmarAsientoForm")) {
            model.addAttribute("confirmarAsientoForm", new ConfirmarAsientoForm());
        }
        return "admin/venta-efectivo-confirmar";
    }

    @PostMapping("/admin/viajes/{id}/asientos/{asientoId}/venta-efectivo")
    public String confirmarVentaEfectivo(@PathVariable Long id, @PathVariable Long asientoId,
            @Valid @ModelAttribute("confirmarAsientoForm") ConfirmarAsientoForm form, BindingResult bindingResult,
            Authentication authentication, Model model, RedirectAttributes redirectAttributes) {
        Viaje viaje = viajeService.obtenerDetalle(id);

        if (bindingResult.hasErrors()) {
            Asiento asiento = asientoRepository.findById(asientoId).orElse(null);
            model.addAttribute("viaje", viaje);
            model.addAttribute("asiento", asiento);
            return "admin/venta-efectivo-confirmar";
        }

        Usuario admin = usuarioActualProvider.obtener(authentication);

        try {
            Reserva reserva = reservaService.crearReserva(viaje, admin, asientoId, form.getNombrePasajero(),
                    form.getDniPasajero());
            pagoService.registrarPago(reserva, MetodoPago.EFECTIVO, REFERENCIA_EFECTIVO);
            pagoService.confirmarPago(reserva);

            redirectAttributes.addFlashAttribute("exito", "Venta registrada y pago confirmado al instante.");
            return "redirect:/reservas/" + reserva.getId();
        } catch (SeleccionInvalidaException | AsientoNoDisponibleException | PagoInvalidoException ex) {
            redirectAttributes.addFlashAttribute("errorGeneral", ex.getMessage());
            return "redirect:/admin/viajes/" + id + "/venta-efectivo";
        }
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
