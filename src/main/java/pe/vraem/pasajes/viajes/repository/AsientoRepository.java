package pe.vraem.pasajes.viajes.repository;

import java.util.List;

import jakarta.persistence.LockModeType;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import pe.vraem.pasajes.reservas.model.Reserva;
import pe.vraem.pasajes.viajes.model.Asiento;
import pe.vraem.pasajes.viajes.model.EstadoAsiento;
import pe.vraem.pasajes.viajes.model.Viaje;

public interface AsientoRepository extends JpaRepository<Asiento, Long> {

    List<Asiento> findAllByViajeOrderByNumeroAsc(Viaje viaje);

    /**
     * Relee los asientos indicados con bloqueo pesimista para evitar que dos
     * reservas concurrentes asignen el mismo asiento (FR-008).
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select a from Asiento a where a.id in :ids")
    List<Asiento> findAllByIdForUpdate(@Param("ids") List<Long> ids);

    long countByViajeAndEstadoIn(Viaje viaje, List<EstadoAsiento> estados);

    List<Asiento> findAllByReserva(Reserva reserva);
}
