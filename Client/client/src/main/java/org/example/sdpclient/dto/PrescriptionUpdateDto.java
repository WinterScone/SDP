package org.example.sdpclient.dto;


import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.example.sdpclient.enums.FrequencyType;

@AllArgsConstructor
@Getter
@Setter
@NoArgsConstructor
public class PrescriptionUpdateDto{
    String dosage;
    FrequencyType frequency;
}