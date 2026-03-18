package org.example.sdpclient.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class MachineIdentifyResponse {

    private boolean ok;
    private boolean matched;
    private Long patientId;
    private String firstName;
    private String lastName;
    private List<PatientPrescriptionsResponse.PrescriptionItem> prescriptions;
    private String message;
}