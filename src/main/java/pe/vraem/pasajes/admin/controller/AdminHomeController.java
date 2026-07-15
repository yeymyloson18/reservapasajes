package pe.vraem.pasajes.admin.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import pe.vraem.pasajes.admin.service.AdminService;

@Controller
public class AdminHomeController {

    private final AdminService adminService;

    public AdminHomeController(AdminService adminService) {
        this.adminService = adminService;
    }

    @GetMapping("/admin")
    public String panel(Model model) {
        model.addAttribute("resumen", adminService.obtenerResumen());
        return "admin/panel";
    }
}
