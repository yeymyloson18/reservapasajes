package pe.vraem.pasajes.auth.controller;

import jakarta.validation.Valid;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import pe.vraem.pasajes.auth.model.Usuario;
import pe.vraem.pasajes.auth.service.UsuarioActualProvider;
import pe.vraem.pasajes.auth.service.UsuarioService;

@Controller
public class PerfilController {

    private final UsuarioService usuarioService;
    private final UsuarioActualProvider usuarioActualProvider;

    public PerfilController(UsuarioService usuarioService, UsuarioActualProvider usuarioActualProvider) {
        this.usuarioService = usuarioService;
        this.usuarioActualProvider = usuarioActualProvider;
    }

    @GetMapping("/perfil")
    public String mostrarPerfil(Authentication authentication, Model model) {
        model.addAttribute("usuario", usuarioActualProvider.obtener(authentication));
        if (!model.containsAttribute("cambiarPasswordForm")) {
            model.addAttribute("cambiarPasswordForm", new CambiarPasswordForm());
        }
        return "auth/perfil";
    }

    @PostMapping("/perfil/password")
    public String cambiarPassword(@Valid @ModelAttribute("cambiarPasswordForm") CambiarPasswordForm form,
            BindingResult bindingResult, Authentication authentication, Model model,
            RedirectAttributes redirectAttributes) {
        Usuario usuario = usuarioActualProvider.obtener(authentication);

        if (bindingResult.hasErrors()) {
            model.addAttribute("usuario", usuario);
            return "auth/perfil";
        }

        usuarioService.cambiarPassword(usuario, form.getNuevaPassword());
        redirectAttributes.addFlashAttribute("exito", "Contrasena actualizada correctamente.");
        return "redirect:/perfil";
    }
}
