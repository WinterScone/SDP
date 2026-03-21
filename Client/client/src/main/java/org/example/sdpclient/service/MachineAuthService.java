package org.example.sdpclient.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class MachineAuthService {

    @Value("${machine.api.key:}")
    private String configuredApiKey;

    public boolean isValid(String providedApiKey) {
        if (configuredApiKey == null || configuredApiKey.isBlank()) {
            return false;
        }
        return providedApiKey != null && providedApiKey.equals(configuredApiKey);
    }
}
