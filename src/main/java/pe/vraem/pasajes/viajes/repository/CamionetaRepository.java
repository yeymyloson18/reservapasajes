package pe.vraem.pasajes.viajes.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import pe.vraem.pasajes.viajes.model.Camioneta;

public interface CamionetaRepository extends JpaRepository<Camioneta, Long> {

    Optional<Camioneta> findByPlaca(String placa);
}
