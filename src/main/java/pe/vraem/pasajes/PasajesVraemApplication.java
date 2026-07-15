package pe.vraem.pasajes;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class PasajesVraemApplication {

	public static void main(String[] args) {
		SpringApplication.run(PasajesVraemApplication.class, args);
	}

}
