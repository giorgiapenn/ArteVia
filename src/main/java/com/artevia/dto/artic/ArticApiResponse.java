package com.artevia.dto.artic;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;


@JsonIgnoreProperties(ignoreUnknown = true)
public record ArticApiResponse(List<ArticArtwork> data) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record ArticArtwork(
            Long id,
            String title,
            @JsonProperty("artist_display") String artistDisplay,
            @JsonProperty("date_display") String dateDisplay,
            @JsonProperty("image_id") String imageId
    ) {}
}
