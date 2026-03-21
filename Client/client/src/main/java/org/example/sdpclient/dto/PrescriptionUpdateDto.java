package org.example.sdpclient.dto;


import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@AllArgsConstructor
@Getter
@Setter
@NoArgsConstructor
public class PrescriptionUpdateDto{
    String dosage;
    String frequency;
}