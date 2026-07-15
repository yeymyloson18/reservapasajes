package pe.vraem.pasajes.pagos.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import pe.vraem.pasajes.pagos.model.Pago;
import pe.vraem.pasajes.reservas.model.Reserva;

public interface PagoRepository extends JpaRepository<Pago, Long> {

    Optional<Pago> findByReserva(Reserva reserva);
}
