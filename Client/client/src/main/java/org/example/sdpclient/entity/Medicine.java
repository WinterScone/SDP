package org.example.sdpclient.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.example.sdpclient.enums.MedicineType;

@Entity
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class Medicine {

    @Id
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, updatable = false)
    private MedicineType medicineId;

    @Column(nullable = false)
    private String medicineName;

    private String shape;
    private String colour;
    private String dosagePerForm;
    private int quantity;

}
