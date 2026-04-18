package ac.jwooo.eye_on;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@ConfigurationPropertiesScan
@SpringBootApplication
public class EyeOnApplication {

	public static void main(String[] args) {
		SpringApplication.run(EyeOnApplication.class, args);
	}

}
