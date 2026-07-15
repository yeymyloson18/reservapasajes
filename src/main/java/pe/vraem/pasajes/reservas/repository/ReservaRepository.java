package pe.vraem.pasajes.reservas.repository;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import pe.vraem.pasajes.auth.model.Usuario;
import pe.vraem.pasajes.reservas.model.EstadoReserva;
import pe.vraem.pasajes.reservas.model.Reserva;

public interface ReservaRepository extends JpaRepository<Reserva, Long> {

    boolean existsByCodigoReserva(String codigoReserva);

    List<Reserva> findAllByEstadoAndFechaCreacionBefore(EstadoReserva estado, LocalDateTime limite);

    List<Reserva> findAllByOrderByFechaCreacionDesc();

    List<Reserva> findAllByUsuarioOrderByFechaCreacionDesc(Usuario usuario);

    List<Reserva> findAllByViaje_IdOrderByFechaCreacionDesc(Long viajeId);

    long countByEstado(EstadoReserva estado);
}
