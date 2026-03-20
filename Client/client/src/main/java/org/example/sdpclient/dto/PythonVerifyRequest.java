package org.example.sdpclient.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class PythonVerifyRequest {

    @JsonProperty("request_id")
    private String requestId;

    @JsonProperty("image_id")
    private Long imageId;

    @JsonProperty("image_url")
    private String imageUrl;

    @JsonProperty("image_access_token")
    private String imageAccessToken;
}
