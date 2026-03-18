package fun.sunset.uavjdk;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class UavJdkApplication {

    public static void main(String[] args) {
        SpringApplication.run(UavJdkApplication.class, args);
    }

}
