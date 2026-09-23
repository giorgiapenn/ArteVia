package com.artevia.service;

import com.artevia.dto.ArtworkDto;
import com.artevia.dto.artic.ArticApiResponse;
import com.artevia.dto.artic.ArticApiResponse.ArticArtwork;
import com.artevia.exception.ExternalServiceUnavailableException;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientException;

@Service
@RequiredArgsConstructor
public class ArtworkOfDayService {

    private static final Logger log = LoggerFactory.getLogger(ArtworkOfDayService.class);

    private static final String FEATURED_ARTWORK_PATH_TEMPLATE =
            "/artworks/search?query[exists][field]=image_id&page=%d&limit=1&fields=id,title,artist_display,date_display,image_id";
    private static final int MAX_PAGE = 1000;    private static final String IIIF_IMAGE_URL_TEMPLATE = "https://www.artic.edu/iiif/2/%s/full/600,/0/default.jpg";


    private final WebClient.Builder webClientBuilder;

    @Value("${artic.api.base-url}")
    private final String baseUrl;

    private WebClient webClient;

    @PostConstruct
    void initWebClient() {
        this.webClient = webClientBuilder.baseUrl(baseUrl).build();
    }

    public ArtworkDto getFeaturedArtwork() {
        ArticArtwork artwork = fetchFirstArtwork();
        return toArtworkDto(artwork);
    }

    private ArticArtwork fetchFirstArtwork() {
        int randomPage = 1 + (int) (Math.random() * MAX_PAGE);
        ArticApiResponse response;
        try {
            response = webClient.get()
                    .uri(FEATURED_ARTWORK_PATH_TEMPLATE.formatted(randomPage))
                    .retrieve()
                    .bodyToMono(ArticApiResponse.class)
                    .block();
        } catch (WebClientException ex) {
            log.warn("Chiamata all'API Art Institute of Chicago fallita: {}", ex.getMessage());
            throw new ExternalServiceUnavailableException("Servizio opera del giorno temporaneamente non disponibile", ex);
        }

        if (response == null || response.data() == null || response.data().isEmpty()) {
            throw new ExternalServiceUnavailableException("Risposta ARTIC non valida: nessuna opera trovata");
        }
        return response.data().get(0);
    }

    private ArtworkDto toArtworkDto(ArticArtwork artwork) {
        return new ArtworkDto(artwork.title(), artwork.artistDisplay(), artwork.dateDisplay(),
                buildImageUrl(artwork.imageId()));
    }

        private String buildImageUrl(String imageId) {
        return imageId != null ? "/api/v1/artwork/image/" + imageId : null;
    }

        private static final java.util.regex.Pattern IMAGE_ID_PATTERN =
            java.util.regex.Pattern.compile("^[a-zA-Z0-9-]{10,60}$");

        public org.springframework.http.ResponseEntity<byte[]> fetchImageBytes(String imageId) {
        if (imageId == null || !IMAGE_ID_PATTERN.matcher(imageId).matches()) {
            throw new IllegalArgumentException("Identificativo immagine non valido");
        }
        String url = IIIF_IMAGE_URL_TEMPLATE.formatted(imageId);
        byte[] bytes;
        try {
            bytes = webClient.get()
                    .uri(java.net.URI.create(url))
                    .header(org.springframework.http.HttpHeaders.USER_AGENT,
                            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36")
                    .header(org.springframework.http.HttpHeaders.REFERER, "https://www.artic.edu/")
                    .retrieve()
                    .bodyToMono(byte[].class)
                    .block();
        } catch (WebClientException ex) {
            log.warn("Impossibile scaricare l'immagine dell'opera: {}", ex.getMessage());
            throw new ExternalServiceUnavailableException("Immagine non disponibile", ex);
        }
        if (bytes == null) {
            throw new ExternalServiceUnavailableException("Immagine non disponibile");
        }
        return org.springframework.http.ResponseEntity.ok()
                .contentType(org.springframework.http.MediaType.IMAGE_JPEG)
                .cacheControl(org.springframework.http.CacheControl.maxAge(java.time.Duration.ofHours(12)))
                .body(bytes);
    }
}