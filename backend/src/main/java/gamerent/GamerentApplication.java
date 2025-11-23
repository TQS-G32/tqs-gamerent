package gamerent;

import io.github.cdimascio.dotenv.Dotenv;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import java.io.File;

@SpringBootApplication
public class GamerentApplication {
    public static void main(String[] args) {
        // Try to load .env file from current dir or parent dir
        try {
            // Check if .env exists in current dir, otherwise try parent
            String directory = "./";
            if (!new File("./.env").exists() && new File("../.env").exists()) {
                directory = "../";
            }

            Dotenv dotenv = Dotenv.configure()
                .directory(directory)
                .ignoreIfMissing()
                .load();
            
            // Set system properties so Spring can pick them up
            dotenv.entries().forEach(entry -> 
                System.setProperty(entry.getKey(), entry.getValue())
            );
        } catch (Exception e) {
            // Ignore if .env not found or error
            System.out.println("Could not load .env file: " + e.getMessage());
        }

		SpringApplication.run(GamerentApplication.class, args);
	}
}
