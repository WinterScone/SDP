package org.example.sdpclient.service;

import org.example.sdpclient.dto.PythonVerifyRequest;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.Map;

@Service
public class PythonProxyService {

    private final RestClient pythonRestClient;

    public PythonProxyService(@Qualifier("pythonRestClient") RestClient pythonRestClient) {
        this.pythonRestClient = pythonRestClient;
    }

    public Map<?, ?> verify(PythonVerifyRequest payload) {
        return pythonRestClient.post()
                .uri("/verify")
                .body(payload)
                .retrieve()
                .onStatus(HttpStatusCode::isError, (req, res) -> {
                    throw new RuntimeException("Python service returned " + res.getStatusCode());
                })
                .body(Map.class);
    }
}
