package pe.vraem.pasajes.viajes.controller;

import jakarta.validation.Valid;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import pe.vraem.pasajes.viajes.model.Camioneta;
import pe.vraem.pasajes.viajes.model.Viaje;
import pe.vraem.pasajes.viajes.service.ViajeInvalidoException;
import pe.vraem.pasajes.viajes.service.ViajeService;

@Controller
@RequestMapping("/admin/viajes")
public class AdminViajeController {

    private final ViajeService viajeService;

    public AdminViajeController(ViajeService viajeService) {
        this.viajeService = viajeService;
    }

    @GetMapping
    public String gestionar(Model model) {
        model.addAttribute("viajes", viajeService.listarTodos());
        return "admin/gestionar-viajes";
    }

    @GetMapping("/nuevo")
    public String mostrarFormularioCreacion(Model model) {
        if (!model.containsAttribute("viajeForm")) {
            model.addAttribute("viajeForm", new ViajeForm());
        }
        model.addAttribute("modoEdicion", false);
        return "viajes/admin-form";
    }

    @PostMapping
    public String crear(@Valid @ModelAttribute("viajeForm") ViajeForm form, BindingResult bindingResult,
            Model model, RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("modoEdicion", false);
            return "viajes/admin-form";
        }

        Camioneta camioneta = viajeService.obtenerOCrearCamioneta(form.getPlacaCamioneta(), form.getRutaCamioneta());
        viajeService.crearViaje(form.getOrigen(), form.getDestino(), form.getFecha(), form.getHora(), camioneta,
                form.getPrecio(), form.getChofer());

        redirectAttributes.addFlashAttribute("exito",
                "Viaje creado y publicado correctamente. Ya aparece en la lista de viajes disponibles.");
        return "redirect:/admin/viajes";
    }

    @GetMapping("/{id}/editar")
    public String mostrarFormularioEdicion(@PathVariable Long id, Model model) {
        if (!model.containsAttribute("viajeForm")) {
            Viaje viaje = viajeService.obtenerDetalle(id);
            ViajeForm form = new ViajeForm();
            form.setOrigen(viaje.getOrigen());
            form.setDestino(viaje.getDestino());
            form.setFecha(viaje.getFecha());
            form.setHora(viaje.getHora());
            form.setPlacaCamioneta(viaje.getCamioneta().getPlaca());
            form.setRutaCamioneta(viaje.getCamioneta().getRuta());
            form.setPrecio(viaje.getPrecio());
            form.setChofer(viaje.getChofer());
            model.addAttribute("viajeForm", form);
        }
        model.addAttribute("viajeId", id);
        model.addAttribute("modoEdicion", true);
        return "viajes/admin-form";
    }

    @PostMapping("/{id}")
    public String editar(@PathVariable Long id, @Valid @ModelAttribute("viajeForm") ViajeForm form,
            BindingResult bindingResult, Model model, RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("viajeId", id);
            model.addAttribute("modoEdicion", true);
            return "viajes/admin-form";
        }

        try {
            Camioneta camioneta = viajeService.obtenerOCrearCamioneta(form.getPlacaCamioneta(),
                    form.getRutaCamioneta());
            viajeService.editarViaje(id, form.getOrigen(), form.getDestino(), form.getFecha(), form.getHora(),
                    camioneta, form.getPrecio(), form.getChofer());
        } catch (ViajeInvalidoException ex) {
            model.addAttribute("viajeId", id);
            model.addAttribute("modoEdicion", true);
            model.addAttribute("errorGeneral", ex.getMessage());
            return "viajes/admin-form";
        }

        redirectAttributes.addFlashAttribute("exito", "Viaje actualizado correctamente.");
        return "redirect:/admin/viajes";
    }

    @PostMapping("/{id}/eliminar")
    public String eliminar(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            viajeService.eliminarViaje(id);
            redirectAttributes.addFlashAttribute("exito", "Viaje eliminado correctamente.");
        } catch (ViajeInvalidoException ex) {
            redirectAttributes.addFlashAttribute("errorGeneral", ex.getMessage());
        }
        return "redirect:/admin/viajes";
    }
}
