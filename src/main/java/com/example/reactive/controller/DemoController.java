package com.example.reactive.controller;

import com.example.reactive.dto.Person;
import com.example.reactive.dto.Photo;
import com.example.reactive.dto.PhotoWrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.SneakyThrows;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.util.UriComponentsBuilder;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.util.Arrays;
import java.util.Comparator;

@RestController
@RequestMapping
public class DemoController {

  private final static String URL = "http://93.175.204.87:8080/reactive/persons";
  private final static String KEY = "DEMO_KEY";

  @SneakyThrows
  @GetMapping("/reactive/persons/max")
  public Person getReactiveMax() {
    final HttpClient httpClient = HttpClient.newHttpClient();
    final HttpResponse<String> response = httpClient.send(
        HttpRequest.newBuilder().GET().uri(URI.create(URL)).build(),
        BodyHandlers.ofString()
    );
    final ObjectMapper objectMapper = new ObjectMapper();
    final Person[] persons = objectMapper.readValue(response.body(), Person[].class);
    return Arrays.stream(persons)
        .max(Comparator.comparing(Person::getReactiveProgrammingLevel))
        .orElseThrow();
  }

  @GetMapping(value = "/pictures/{sol}/largest", produces = MediaType.IMAGE_PNG_VALUE)
  public Mono<byte[]> largestPicture(@PathVariable int sol) {
    final URI nasaUri = configureUri(sol);
    System.out.println(nasaUri);

    return WebClient.create()
        .get()
        .uri(nasaUri)
        .exchangeToMono(resp -> resp.bodyToMono(PhotoWrapper.class))
        .flatMapMany(a -> Flux.fromIterable(a.getPhotos()))
        .map(Photo::getImg_src)
        .flatMap(url -> WebClient.create()
            .head()
            .uri(URI.create(url))
            .retrieve()
            .toBodilessEntity()
            .map(HttpEntity::getHeaders)
            .mapNotNull(HttpHeaders::getLocation)
            .map(URI::toString)
        .flatMap(el -> WebClient.create()
              .mutate()
              .codecs(c -> c.defaultCodecs().maxInMemorySize(10_000_000))
              .build()
            .get()
            .uri(el)
            .retrieve()
            .toEntity(byte[].class)))
        .reduce((e1, e2) -> e1.getHeaders().getContentLength() > e2.getHeaders().getContentLength() ? e1 : e2)
        .mapNotNull(HttpEntity::getBody);
  }

  private URI configureUri(int sol) {
    return UriComponentsBuilder
        .fromHttpUrl("https://api.nasa.gov/mars-photos/api/v1/rovers/curiosity/photos")
        .queryParam("sol", sol)
        .queryParam("api_key", KEY)
        .build().toUri();
  }
}
