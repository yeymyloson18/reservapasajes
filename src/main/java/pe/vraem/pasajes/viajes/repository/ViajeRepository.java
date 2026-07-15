package pe.vraem.pasajes.viajes.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import pe.vraem.pasajes.viajes.model.Viaje;

public interface ViajeRepository extends JpaRepository<Viaje, Long> {

    List<Viaje> findAllByOrderByFechaAscHoraAsc();
}
