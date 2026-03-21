package org.example.sdpclient.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class FaceVerifyResponse {
    private boolean ok;
    private boolean verified;
    private Long patientId;
    private String message;
}
