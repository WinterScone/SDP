package org.example.sdpclient.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class MachinePatientImageItem {
    private Long patientId;
    private String contentType;
    private String imageBase64;
    private String faceEnrolledAt;
}