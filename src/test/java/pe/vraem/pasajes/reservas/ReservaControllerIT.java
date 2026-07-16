package pe.vraem.pasajes.reservas;

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
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

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
import pe.vraem.pasajes.reservas.model.EstadoReserva;
import pe.vraem.pasajes.reservas.model.Reserva;
import pe.vraem.pasajes.reservas.repository.ReservaRepository;
import pe.vraem.pasajes.viajes.model.Asiento;
import pe.vraem.pasajes.viajes.model.Camioneta;
import pe.vraem.pasajes.viajes.model.EstadoAsiento;
import pe.vraem.pasajes.viajes.model.Viaje;
import pe.vraem.pasajes.viajes.repository.AsientoRepository;
import pe.vraem.pasajes.viajes.repository.CamionetaRepository;
import pe.vraem.pasajes.viajes.repository.ViajeRepository;
import pe.vraem.pasajes.viajes.service.ViajeService;

@SpringBootTest
@AutoConfigureMockMvc
class ReservaControllerIT {

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

    private static final AtomicInteger PLACA_SEQ = new AtomicInteger();

    private Viaje crearViajeConAsientos() {
        Camioneta camioneta = camionetaRepository.save(new Camioneta("XYZ-" + PLACA_SEQ.incrementAndGet(), "Ayacucho - Sivia"));
        Viaje viaje = viajeRepository.save(new Viaje("Ayacucho", "Sivia", LocalDate.now().plusDays(2), LocalTime.of(9, 0),
                camioneta, new BigDecimal("40.00"), "Carlos Mamani"));
        for (int i = 1; i <= ViajeService.CAPACIDAD_ASIENTOS; i++) {
            asientoRepository.save(new Asiento(viaje, i));
        }
        return viaje;
    }

    private Usuario crearPasajero(String dni, String email) {
        return usuarioRepository.save(new Usuario(dni, "Usuario " + dni, email,
                passwordEncoder.encode("claveSegura1"), Rol.PASAJERO));
    }

    private Usuario crearAdmin(String dni, String email) {
        return usuarioRepository.save(new Usuario(dni, "Admin " + dni, email,
                passwordEncoder.encode("claveSegura1"), Rol.ADMIN));
    }

    @Test
    void creaReservaDeUnAsientoYRedirigeAlPago() throws Exception {
        Viaje viaje = crearViajeConAsientos();
        Usuario comprador = crearPasajero("45678912", "comprador1@example.com");
        Asiento asiento = asientoRepository.findAllByViajeOrderByNumeroAsc(viaje).get(0);

        mockMvc.perform(post("/viajes/{id}/asientos/{asientoId}", viaje.getId(), asiento.getId())
                        .with(csrf())
                        .with(user(comprador.getEmail()).authorities(new SimpleGrantedAuthority("ROLE_PASAJERO")))
                        .param("nombrePasajero", "Ana Quispe")
                        .param("dniPasajero", "45678912"))
                .andExpect(status().is3xxRedirection());

        Asiento asientoActualizado = asientoRepository.findById(asiento.getId()).orElseThrow();
        assertThat(asientoActualizado.getEstado()).isEqualTo(EstadoAsiento.RESERVADO);
        assertThat(asientoActualizado.getNombrePasajero()).isEqualTo("Ana Quispe");
    }

    @Test
    void unUsuarioDistintoDelPropietarioNoPuedeVerLaReserva() throws Exception {
        Viaje viaje = crearViajeConAsientos();
        Usuario propietario = crearPasajero("11112222", "propietario@example.com");
        Usuario otroUsuario = crearPasajero("33334444", "otro@example.com");
        Asiento asiento = asientoRepository.findAllByViajeOrderByNumeroAsc(viaje).get(0);

        mockMvc.perform(post("/viajes/{id}/asientos/{asientoId}", viaje.getId(), asiento.getId())
                        .with(csrf())
                        .with(user(propietario.getEmail()).authorities(new SimpleGrantedAuthority("ROLE_PASAJERO")))
                        .param("nombrePasajero", "Ana Quispe")
                        .param("dniPasajero", "45678912"))
                .andExpect(status().is3xxRedirection());

        Long reservaId = reservaRepository.findAllByOrderByFechaCreacionDesc().get(0).getId();

        mockMvc.perform(get("/reservas/{id}", reservaId)
                        .with(user(otroUsuario.getEmail()).authorities(new SimpleGrantedAuthority("ROLE_PASAJERO"))))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/reservas/{id}", reservaId)
                        .with(user(propietario.getEmail()).authorities(new SimpleGrantedAuthority("ROLE_PASAJERO"))))
                .andExpect(status().isOk());
    }

    @Test
    void dosSolicitudesConcurrentesSobreElMismoAsientoSoloUnaTieneExito() throws Exception {
        Viaje viaje = crearViajeConAsientos();
        Asiento asiento = asientoRepository.findAllByViajeOrderByNumeroAsc(viaje).get(0);
        Usuario usuario1 = crearPasajero("55556666", "concurrente1@example.com");
        Usuario usuario2 = crearPasajero("77778888", "concurrente2@example.com");

        ExecutorService executor = Executors.newFixedThreadPool(2);
        CountDownLatch listos = new CountDownLatch(2);
        CountDownLatch salida = new CountDownLatch(1);
        AtomicInteger exitos = new AtomicInteger(0);

        Runnable intento1 = () -> ejecutarIntentoDeReserva(viaje, asiento, usuario1, listos, salida, exitos);
        Runnable intento2 = () -> ejecutarIntentoDeReserva(viaje, asiento, usuario2, listos, salida, exitos);

        executor.submit(intento1);
        executor.submit(intento2);

        listos.await(5, TimeUnit.SECONDS);
        salida.countDown();
        executor.shutdown();
        executor.awaitTermination(10, TimeUnit.SECONDS);

        assertThat(exitos.get()).isEqualTo(1);
        Asiento asientoFinal = asientoRepository.findById(asiento.getId()).orElseThrow();
        assertThat(asientoFinal.getEstado()).isEqualTo(EstadoAsiento.RESERVADO);

        long reservasParaEsteAsiento = reservaRepository.findAllByOrderByFechaCreacionDesc().stream()
                .filter(r -> r.getViaje().getId().equals(viaje.getId()) && r.getEstado() == EstadoReserva.PENDIENTE)
                .count();
        assertThat(reservasParaEsteAsiento).isEqualTo(1);
    }

    @Test
    void misReservasMuestraElHistorialDeViajesPagados() throws Exception {
        Usuario pasajero = crearPasajero("22223333", "historial@example.com");

        Camioneta camionetaPasada = camionetaRepository.save(new Camioneta("HIS-001", "Ayacucho - Santa Rosa"));
        Viaje viajePasado = viajeRepository.save(new Viaje("Ayacucho", "Santa Rosa", LocalDate.now().minusDays(10),
                LocalTime.of(8, 0), camionetaPasada, new BigDecimal("30.00"), "Carlos Mamani"));
        Asiento asientoPasado = asientoRepository.save(new Asiento(viajePasado, 1));

        Reserva reservaPasada = new Reserva(pasajero, viajePasado, new BigDecimal("30.00"));
        reservaPasada.asignarCodigoReserva("HISTORIC");
        reservaPasada.marcarPagada();
        reservaPasada = reservaRepository.save(reservaPasada);
        asientoPasado.ocupar(reservaPasada, "Usuario 22223333", "22223333");
        asientoPasado.marcarPagado();
        asientoRepository.save(asientoPasado);

        Viaje viajeFuturo = crearViajeConAsientos();
        Asiento asientoFuturo = asientoRepository.findAllByViajeOrderByNumeroAsc(viajeFuturo).get(0);
        mockMvc.perform(post("/viajes/{id}/asientos/{asientoId}", viajeFuturo.getId(), asientoFuturo.getId())
                        .with(csrf())
                        .with(user(pasajero.getEmail()).authorities(new SimpleGrantedAuthority("ROLE_PASAJERO")))
                        .param("nombrePasajero", "Usuario 22223333")
                        .param("dniPasajero", "22223333"))
                .andExpect(status().is3xxRedirection());

        mockMvc.perform(get("/reservas")
                        .with(user(pasajero.getEmail()).authorities(new SimpleGrantedAuthority("ROLE_PASAJERO"))))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Has viajado")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Santa Rosa")));
    }

    @Test
    void unAdminQueReservaConMetodoDePagoConfirmaAlInstanteSinPasarPorPendiente() throws Exception {
        Viaje viaje = crearViajeConAsientos();
        Usuario admin = crearAdmin("66667777", "venta-admin@example.com");
        Asiento asiento = asientoRepository.findAllByViajeOrderByNumeroAsc(viaje).get(0);

        mockMvc.perform(post("/viajes/{id}/asientos/{asientoId}", viaje.getId(), asiento.getId())
                        .with(csrf())
                        .with(user(admin.getEmail()).authorities(new SimpleGrantedAuthority("ROLE_ADMIN")))
                        .param("nombrePasajero", "Cliente Presencial")
                        .param("dniPasajero", "88887777")
                        .param("metodoPago", "EFECTIVO"))
                .andExpect(status().is3xxRedirection());

        Asiento asientoVendido = asientoRepository.findById(asiento.getId()).orElseThrow();
        assertThat(asientoVendido.getEstado()).isEqualTo(EstadoAsiento.PAGADO);
        assertThat(asientoVendido.getNombrePasajero()).isEqualTo("Cliente Presencial");

        Reserva reserva = reservaRepository.findAll().stream()
                .filter(r -> r.getViaje().getId().equals(viaje.getId()))
                .findFirst().orElseThrow();
        assertThat(reserva.getEstado()).isEqualTo(EstadoReserva.PAGADO);
        assertThat(reserva.getUsuario().getId()).isEqualTo(admin.getId());
    }

    @Test
    void unAdminSinElegirMetodoDePagoDebeVolverAIntentar() throws Exception {
        Viaje viaje = crearViajeConAsientos();
        Usuario admin = crearAdmin("55557777", "venta-admin-sin-metodo@example.com");
        Asiento asiento = asientoRepository.findAllByViajeOrderByNumeroAsc(viaje).get(0);

        mockMvc.perform(post("/viajes/{id}/asientos/{asientoId}", viaje.getId(), asiento.getId())
                        .with(csrf())
                        .with(user(admin.getEmail()).authorities(new SimpleGrantedAuthority("ROLE_ADMIN")))
                        .param("nombrePasajero", "Cliente Presencial")
                        .param("dniPasajero", "88887777"))
                .andExpect(status().isOk());

        Asiento asientoSinVender = asientoRepository.findById(asiento.getId()).orElseThrow();
        assertThat(asientoSinVender.getEstado()).isEqualTo(EstadoAsiento.LIBRE);
    }

    private void ejecutarIntentoDeReserva(Viaje viaje, Asiento asiento, Usuario usuario, CountDownLatch listos,
            CountDownLatch salida, AtomicInteger exitos) {
        try {
            listos.countDown();
            salida.await(5, TimeUnit.SECONDS);

            var resultado = mockMvc.perform(post("/viajes/{id}/asientos/{asientoId}", viaje.getId(), asiento.getId())
                    .with(csrf())
                    .with(user(usuario.getEmail()).authorities(new SimpleGrantedAuthority("ROLE_PASAJERO")))
                    .param("nombrePasajero", "Pasajero " + usuario.getDni())
                    .param("dniPasajero", usuario.getDni()))
                    .andReturn();

            String location = resultado.getResponse().getRedirectedUrl();
            if (location != null && location.contains("/pago")) {
                exitos.incrementAndGet();
            }
        } catch (Exception e) {
            // intento fallido: no cuenta como exito
        }
    }
}
