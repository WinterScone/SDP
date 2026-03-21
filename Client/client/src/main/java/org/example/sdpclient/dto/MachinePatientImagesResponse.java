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
public class MachinePatientImagesResponse {
    private boolean ok;
    private int count;
    private List<MachinePatientImageItem> patients;
    private String message;
}
