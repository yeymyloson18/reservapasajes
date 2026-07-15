package pe.vraem.pasajes.viajes;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;

import pe.vraem.pasajes.auth.model.Rol;
import pe.vraem.pasajes.auth.model.Usuario;
import pe.vraem.pasajes.auth.repository.UsuarioRepository;
import pe.vraem.pasajes.viajes.model.Camioneta;
import pe.vraem.pasajes.viajes.model.Viaje;
import pe.vraem.pasajes.viajes.repository.AsientoRepository;
import pe.vraem.pasajes.viajes.repository.CamionetaRepository;
import pe.vraem.pasajes.viajes.repository.ViajeRepository;
import pe.vraem.pasajes.viajes.service.ViajeService;

@SpringBootTest
@AutoConfigureMockMvc
class AdminViajeControllerIT {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private ViajeRepository viajeRepository;

    @Autowired
    private CamionetaRepository camionetaRepository;

    @Autowired
    private AsientoRepository asientoRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Test
    void adminCreaEditaYEliminaUnViaje() throws Exception {
        Usuario admin = usuarioRepository.save(new Usuario("90001111", "Admin VRAEM", "admin-viajes@example.com",
                passwordEncoder.encode("claveSegura1"), Rol.ADMIN));

        mockMvc.perform(post("/admin/viajes")
                        .with(csrf())
                        .with(user(admin.getEmail()).authorities(new SimpleGrantedAuthority("ROLE_ADMIN")))
                        .param("origen", "Ayacucho")
                        .param("destino", "Sivia")
                        .param("fecha", LocalDate.now().plusDays(5).toString())
                        .param("hora", "07:00")
                        .param("placaCamioneta", "ADM-001")
                        .param("rutaCamioneta", "Ayacucho - Sivia")
                        .param("precio", "45.00")
                        .param("chofer", "Carlos Mamani"))
                .andExpect(status().is3xxRedirection());

        Camioneta camioneta = camionetaRepository.findByPlaca("ADM-001").orElseThrow();
        Viaje viaje = viajeRepository.findAll().stream()
                .filter(v -> v.getCamioneta().getId().equals(camioneta.getId()))
                .findFirst().orElseThrow();
        assertThat(asientoRepository.findAllByViajeOrderByNumeroAsc(viaje)).hasSize(ViajeService.CAPACIDAD_ASIENTOS);

        mockMvc.perform(post("/admin/viajes/{id}", viaje.getId())
                        .with(csrf())
                        .with(user(admin.getEmail()).authorities(new SimpleGrantedAuthority("ROLE_ADMIN")))
                        .param("origen", "Ayacucho")
                        .param("destino", "Sivia")
                        .param("fecha", LocalDate.now().plusDays(6).toString())
                        .param("hora", "08:00")
                        .param("placaCamioneta", "ADM-001")
                        .param("rutaCamioneta", "Ayacucho - Sivia")
                        .param("precio", "50.00")
                        .param("chofer", "Carlos Mamani"))
                .andExpect(status().is3xxRedirection());

        Viaje viajeEditado = viajeRepository.findById(viaje.getId()).orElseThrow();
        assertThat(viajeEditado.getPrecio()).isEqualByComparingTo("50.00");

        mockMvc.perform(post("/admin/viajes/{id}/eliminar", viaje.getId())
                        .with(csrf())
                        .with(user(admin.getEmail()).authorities(new SimpleGrantedAuthority("ROLE_ADMIN"))))
                .andExpect(status().is3xxRedirection());

        assertThat(viajeRepository.findById(viaje.getId())).isEmpty();
    }

    @Test
    void unUsuarioSinRolAdminNoPuedeCrearViajes() throws Exception {
        Usuario pasajero = usuarioRepository.save(new Usuario("90002222", "Pasajero Cualquiera",
                "pasajero-viajes@example.com", passwordEncoder.encode("claveSegura1"), Rol.PASAJERO));

        mockMvc.perform(post("/admin/viajes")
                        .with(csrf())
                        .with(user(pasajero.getEmail()).authorities(new SimpleGrantedAuthority("ROLE_PASAJERO")))
                        .param("origen", "Ayacucho")
                        .param("destino", "Pichari")
                        .param("fecha", LocalDate.now().plusDays(5).toString())
                        .param("hora", "07:00")
                        .param("placaCamioneta", "ADM-002")
                        .param("rutaCamioneta", "Ayacucho - Pichari")
                        .param("precio", "45.00")
                        .param("chofer", "Carlos Mamani"))
                .andExpect(status().isForbidden());

        assertThat(camionetaRepository.findByPlaca("ADM-002")).isEmpty();
    }
}
