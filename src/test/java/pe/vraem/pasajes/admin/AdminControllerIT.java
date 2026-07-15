package pe.vraem.pasajes.admin;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

import pe.vraem.pasajes.auth.model.Rol;
import pe.vraem.pasajes.auth.model.Usuario;
import pe.vraem.pasajes.auth.repository.UsuarioRepository;

@SpringBootTest
@AutoConfigureMockMvc
class AdminControllerIT {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Test
    void unAdminPuedeAccederAlPanelYUnPasajeroNo() throws Exception {
        Usuario admin = usuarioRepository.save(new Usuario("90003333", "Admin Panel", "admin-panel@example.com",
                passwordEncoder.encode("claveSegura1"), Rol.ADMIN));
        Usuario pasajero = usuarioRepository.save(new Usuario("90004444", "Pasajero Panel", "pasajero-panel@example.com",
                passwordEncoder.encode("claveSegura1"), Rol.PASAJERO));

        mockMvc.perform(get("/admin")
                        .with(user(admin.getEmail()).authorities(new SimpleGrantedAuthority("ROLE_ADMIN"))))
                .andExpect(status().isOk());

        mockMvc.perform(get("/admin")
                        .with(user(pasajero.getEmail()).authorities(new SimpleGrantedAuthority("ROLE_PASAJERO"))))
                .andExpect(status().isForbidden());
    }
}
