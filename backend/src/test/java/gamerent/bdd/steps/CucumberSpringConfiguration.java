package gamerent.bdd.steps;

import io.cucumber.spring.CucumberContextConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import gamerent.GamerentApplication;

@CucumberContextConfiguration
@SpringBootTest(classes = GamerentApplication.class)
public class CucumberSpringConfiguration {
}

