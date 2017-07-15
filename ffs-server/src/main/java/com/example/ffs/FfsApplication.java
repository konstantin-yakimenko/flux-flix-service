package com.example.ffs;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import org.springframework.http.MediaType;
import org.springframework.stereotype.*;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.reactive.function.server.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.util.function.Tuple2;

import java.awt.*;
import java.time.Duration;
import java.util.Date;
import java.util.UUID;
import java.util.stream.Stream;

import static org.springframework.web.reactive.function.server.RequestPredicates.GET;
import static org.springframework.web.reactive.function.server.RouterFunctions.route;

@SpringBootApplication
public class FfsApplication {

	public static void main(String[] args) {
		SpringApplication.run(FfsApplication.class, args);
	}

	@Component
    public static class RouteHandlers {

	    private final FluxFlixService fluxFlixService;

        public RouteHandlers(FluxFlixService fluxFlixService) {
            this.fluxFlixService = fluxFlixService;
        }

        public Mono<ServerResponse> all(ServerRequest serverRequest) {
            return ServerResponse.ok().body(fluxFlixService.all(), Movie.class);
        }
        public Mono<ServerResponse> events(ServerRequest serverRequest) {
            String movieId = serverRequest.pathVariable("movieId");
            return ServerResponse.ok()
                    .contentType(MediaType.TEXT_EVENT_STREAM)
                    .body(fluxFlixService.streamTheStreams(movieId), MovieEvent.class);
        }
        public Mono<ServerResponse> byId(ServerRequest serverRequest) {
            String movieId = serverRequest.pathVariable("movieId");
            return ServerResponse.ok().body(fluxFlixService.byId(movieId), Movie.class);
        }
    }

	@Bean
    RouterFunction<?> routes(RouteHandlers routeHandlers) {
	    return route(GET("/movies"), routeHandlers::all)
            .andRoute(GET("/movies/{movieId}"), routeHandlers::byId)
            .andRoute(GET("/movies/{movieId}/events"), routeHandlers::events);
    }


	@Bean
	CommandLineRunner movies(MovieRepository movieRepository) {
		return args->{
		    movieRepository.deleteAll().subscribe(null, null, () -> {
                Stream.of("Terminator", "Game of trones", "Lord of the rings",
                            "Back to seal", "The matrix", "Titanic")
                        .forEach(movieTitle->movieRepository.save(
                                new Movie(UUID.randomUUID().toString(), movieTitle))
                                .subscribe(movie -> System.out.println(movie.toString())));
            });

//			movieRepository.findAll().subscribe(System.out::println);
		};
	}
}

//@RestController
class FluxFlixRestController {

    private final FluxFlixService fluxFlixService;

    public FluxFlixRestController(FluxFlixService fluxFlixService) {
        this.fluxFlixService = fluxFlixService;
    }

    @GetMapping("/movies")
    public Flux<Movie> all() {
        return fluxFlixService.all();
    }

    @GetMapping("/movies/{movieId}")
    public Mono<Movie> byId(@PathVariable String movieId) {
        return fluxFlixService.byId(movieId);
    }

    @GetMapping(produces = MediaType.TEXT_EVENT_STREAM_VALUE, value = "/movies/{movieId}/events")
    public Flux<MovieEvent> crossTheStreams(@PathVariable String movieId) {
        return fluxFlixService.streamTheStreams(movieId);
    }
}


@Service
class FluxFlixService {

    private final MovieRepository movieRepository;

    public FluxFlixService(MovieRepository movieRepository) {
        this.movieRepository = movieRepository;
    }

    public Flux<MovieEvent> streamTheStreams(String movieId) {
        return byId(movieId).flatMapMany(movie -> {
            Flux<Long> interval = Flux.interval(Duration.ofSeconds(1));
            Flux<MovieEvent> events = Flux.fromStream(Stream.generate(()->new MovieEvent(movie, new Date())));
            return Flux.zip(interval, events).map(Tuple2::getT2);
        });
    }

    public Mono<Movie> byId(String movieId) {
        return movieRepository.findById(movieId);
    }

    public Flux<Movie> all() {
        return movieRepository.findAll();
    }
}

interface MovieRepository extends ReactiveMongoRepository<Movie, String> {
}

@Data
@AllArgsConstructor
@NoArgsConstructor
@Document
class Movie {
	@Id
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
