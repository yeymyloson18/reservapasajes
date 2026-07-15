package pe.vraem.pasajes.pagos;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
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
import pe.vraem.pasajes.reservas.model.Reserva;
import pe.vraem.pasajes.reservas.repository.ReservaRepository;
import pe.vraem.pasajes.viajes.model.Asiento;
import pe.vraem.pasajes.viajes.model.Camioneta;
import pe.vraem.pasajes.viajes.model.EstadoAsiento;
import pe.vraem.pasajes.viajes.model.Viaje;
import pe.vraem.pasajes.viajes.repository.AsientoRepository;
import pe.vraem.pasajes.viajes.repository.CamionetaRepository;
import pe.vraem.pasajes.viajes.repository.ViajeRepository;

@SpringBootTest
@AutoConfigureMockMvc
class PagoControllerIT {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private CamionetaRepository camionetaRepository;

    @Autowired
    private ViajeRepository viajeRepository;

    @Autowired
    private AsientoRepository asientoRepository;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private ReservaRepository reservaRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Test
    void flujoDePagoYConfirmacionMuestraElBoletoCompleto() throws Exception {
        Camioneta camioneta = camionetaRepository.save(new Camioneta("PGO-001", "Ayacucho - Pichari"));
        Viaje viaje = viajeRepository.save(new Viaje("Ayacucho", "Pichari", LocalDate.now().plusDays(3),
                LocalTime.of(7, 30), camioneta, new BigDecimal("60.00"), "Carlos Mamani"));
        Asiento asiento = asientoRepository.save(new Asiento(viaje, 1));

        Usuario comprador = usuarioRepository.save(new Usuario("45679001", "Ana Quispe", "pago-comprador@example.com",
                passwordEncoder.encode("claveSegura1"), Rol.PASAJERO));
        Usuario admin = usuarioRepository.save(new Usuario("99998888", "Admin VRAEM", "pago-admin@example.com",
                passwordEncoder.encode("claveSegura1"), Rol.ADMIN));

        Reserva reserva = new Reserva(comprador, viaje, new BigDecimal("60.00"));
        reserva.asignarCodigoReserva("TESTCODE");
        reserva = reservaRepository.save(reserva);
        asiento.ocupar(reserva, "Ana Quispe", "45679001");
        asientoRepository.save(asiento);

        mockMvc.perform(post("/reservas/{id}/pago", reserva.getId())
                        .with(csrf())
                        .with(user(comprador.getEmail()).authorities(new SimpleGrantedAuthority("ROLE_PASAJERO")))
                        .param("metodo", "YAPE")
                        .param("referencia", "OP-998877"))
                .andExpect(status().is3xxRedirection());

        mockMvc.perform(post("/admin/reservas/{id}/confirmar-pago", reserva.getId())
                        .with(csrf())
                        .with(user(admin.getEmail()).authorities(new SimpleGrantedAuthority("ROLE_ADMIN"))))
                .andExpect(status().is3xxRedirection());

        Asiento asientoPagado = asientoRepository.findById(asiento.getId()).orElseThrow();
        assertThat(asientoPagado.getEstado()).isEqualTo(EstadoAsiento.PAGADO);

        mockMvc.perform(get("/reservas/{id}", reserva.getId())
                        .with(user(comprador.getEmail()).authorities(new SimpleGrantedAuthority("ROLE_PASAJERO"))))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("TESTCODE")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Ana Quispe")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("45679001")));
    }
}
