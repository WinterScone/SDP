package org.example.sdpclient.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ReduceMedicineRequest {
    private String medicineName;
    private Integer quantity;
}
