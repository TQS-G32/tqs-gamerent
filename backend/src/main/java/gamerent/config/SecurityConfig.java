package gamerent.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.config.Customizer;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .cors(Customizer.withDefaults())
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/api/auth/**", "/", "/index.html", "/static/**", "/assets/**", "/favicon.ico", "/api/igdb/**").permitAll()
                .requestMatchers("/api/items/my-items").authenticated()
                .requestMatchers("/api/chats/**").authenticated()
                .requestMatchers(org.springframework.http.HttpMethod.GET, "/api/users/**").permitAll()
                .requestMatchers(org.springframework.http.HttpMethod.GET, "/api/reviews/**").permitAll()
                .requestMatchers(org.springframework.http.HttpMethod.GET, "/api/items/**").permitAll()
                .requestMatchers("/api/items/**").permitAll()
                .anyRequest().authenticated()
            );
        return http.build();
    }
}
