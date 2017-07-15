package com.example.ffc;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.Date;


@SpringBootApplication
public class FfcApplication {

	@Bean
	WebClient webClient() {
		return WebClient.create();
	}

	@Bean
	CommandLineRunner demo(WebClient client) {
		return args -> {
			client.get().uri("http://localhost:8080/movies")
					.exchange()
					.subscribe(cr -> cr.bodyToFlux(Movie.class)
                    .filter(movie -> movie.getTitle().equalsIgnoreCase("Terminator"))
					.subscribe(silence->{
						client
						    .get()
                            .uri("http://localhost:8080/movies/"+silence.getId()+"/events")
                            .exchange()
                            .subscribe(cr2->cr2.bodyToFlux(MovieEvent.class)
                            .subscribe(System.out::println));
					}));
		};
	}

	public static void main(String[] args) {
		SpringApplication.run(FfcApplication.class, args);
	}

}

@Data
@AllArgsConstructor
@NoArgsConstructor
class Movie {
	private String id;
	private String title;
}

@Data
@NoArgsConstructor
@AllArgsConstructor
class MovieEvent {
	private Movie movie;
	private Date when;
}
