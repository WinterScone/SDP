package org.example.sdpclient.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
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

    private Integer dosagePerForm;

    private int quantity;

    @Column(length = 500)
    private String instruction;
}