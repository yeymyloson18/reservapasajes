package pe.vraem.pasajes.reservas;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;

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
import pe.vraem.pasajes.reservas.model.Reserva;
import pe.vraem.pasajes.reservas.repository.ReservaRepository;
import pe.vraem.pasajes.viajes.model.Camioneta;
import pe.vraem.pasajes.viajes.model.Viaje;
import pe.vraem.pasajes.viajes.repository.CamionetaRepository;
import pe.vraem.pasajes.viajes.repository.ViajeRepository;

@SpringBootTest
@AutoConfigureMockMvc
class AdminReservaControllerIT {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private CamionetaRepository camionetaRepository;

    @Autowired
    private ViajeRepository viajeRepository;

    @Autowired
    private ReservaRepository reservaRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Test
    void unAdminVeElListadoDeReservasYUnPasajeroNoPuedeAcceder() throws Exception {
        Usuario admin = usuarioRepository.save(new Usuario("90005555", "Admin Reservas", "admin-reservas@example.com",
                passwordEncoder.encode("claveSegura1"), Rol.ADMIN));
        Usuario pasajero = usuarioRepository.save(new Usuario("90006666", "Pasajero Reservas",
                "pasajero-reservas@example.com", passwordEncoder.encode("claveSegura1"), Rol.PASAJERO));
        Usuario comprador = usuarioRepository.save(new Usuario("90007777", "Comprador Reservas",
                "comprador-reservas@example.com", passwordEncoder.encode("claveSegura1"), Rol.PASAJERO));

        Camioneta camioneta = camionetaRepository.save(new Camioneta("LST-001", "Ayacucho - Santa Rosa"));
        Viaje viaje = viajeRepository.save(new Viaje("Ayacucho", "Santa Rosa", LocalDate.now().plusDays(4),
                LocalTime.of(6, 30), camioneta, new BigDecimal("48.00"), 5, "Carlos Mamani"));

        Reserva reservaPagada = new Reserva(comprador, viaje, new BigDecimal("48.00"));
        reservaPagada.asignarCodigoReserva("PAGADA01");
        reservaPagada.marcarPagada();
        reservaRepository.save(reservaPagada);

        Reserva reservaPendiente = new Reserva(comprador, viaje, new BigDecimal("48.00"));
        reservaPendiente.asignarCodigoReserva("PENDIEN1");
        reservaRepository.save(reservaPendiente);

        mockMvc.perform(get("/admin/reservas")
                        .with(user(admin.getEmail()).authorities(new SimpleGrantedAuthority("ROLE_ADMIN"))))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("PAGADA01")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("PENDIEN1")));

        mockMvc.perform(get("/admin/reservas")
                        .with(user(pasajero.getEmail()).authorities(new SimpleGrantedAuthority("ROLE_PASAJERO"))))
                .andExpect(status().isForbidden());
    }
}
