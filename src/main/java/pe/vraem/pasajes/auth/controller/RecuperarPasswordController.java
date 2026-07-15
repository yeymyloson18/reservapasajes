package pe.vraem.pasajes.auth.controller;

import jakarta.validation.Valid;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;

import pe.vraem.pasajes.auth.service.UsuarioNoEncontradoException;
import pe.vraem.pasajes.auth.service.UsuarioService;

@Controller
public class RecuperarPasswordController {

    private final UsuarioService usuarioService;

    public RecuperarPasswordController(UsuarioService usuarioService) {
        this.usuarioService = usuarioService;
    }

    @GetMapping("/recuperar-password")
    public String mostrarFormulario(Model model) {
        if (!model.containsAttribute("recuperarPasswordForm")) {
            model.addAttribute("recuperarPasswordForm", new RecuperarPasswordForm());
        }
        return "auth/recuperar-password";
    }

    @PostMapping("/recuperar-password")
    public String recuperar(@Valid @ModelAttribute("recuperarPasswordForm") RecuperarPasswordForm form,
            BindingResult bindingResult, Model model) {
        if (bindingResult.hasErrors()) {
            return "auth/recuperar-password";
        }

        try {
            String passwordTemporal = usuarioService.recuperarPassword(form.getEmail());
            model.addAttribute("passwordTemporal", passwordTemporal);
        } catch (UsuarioNoEncontradoException ex) {
            model.addAttribute("errorGeneral", ex.getMessage());
        }

        return "auth/recuperar-password";
    }
}
