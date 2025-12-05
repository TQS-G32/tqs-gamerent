package gamerent;

import io.github.cdimascio.dotenv.Dotenv;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import java.io.File;
import java.util.logging.Logger;

@SpringBootApplication
public class GamerentApplication {
    private static final Logger logger = Logger.getLogger(GamerentApplication.class.getName());

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
            logger.warning("Could not load .env file: " + e.getMessage());
        }

		SpringApplication.run(GamerentApplication.class, args);
	}
}
