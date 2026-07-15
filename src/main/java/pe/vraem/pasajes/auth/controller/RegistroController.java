package pe.vraem.pasajes.auth.controller;

import jakarta.validation.Valid;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;

import pe.vraem.pasajes.auth.service.RegistroDuplicadoException;
import pe.vraem.pasajes.auth.service.UsuarioService;

@Controller
public class RegistroController {

    private final UsuarioService usuarioService;

    public RegistroController(UsuarioService usuarioService) {
        this.usuarioService = usuarioService;
    }

    @GetMapping("/registro")
    public String mostrarFormulario(Model model) {
        if (!model.containsAttribute("registroForm")) {
            model.addAttribute("registroForm", new RegistroForm());
        }
        return "auth/registro";
    }

    @PostMapping("/registro")
    public String registrar(@Valid @ModelAttribute("registroForm") RegistroForm form, BindingResult bindingResult,
            Model model) {
        if (bindingResult.hasErrors()) {
            return "auth/registro";
        }

        try {
            usuarioService.registrar(form.getDni(), form.getNombre(), form.getEmail(), form.getPassword());
        } catch (RegistroDuplicadoException ex) {
            model.addAttribute("errorGeneral", ex.getMessage());
            return "auth/registro";
        }

        return "redirect:/login?registrado";
    }
}
