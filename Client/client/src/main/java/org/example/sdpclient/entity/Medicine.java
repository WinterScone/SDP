package org.example.sdpclient.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "medicine")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class Medicine {

    @Id
    @Column(nullable = false, updatable = false)
    private Integer medicineId;

    @Column(nullable = false)
    private String medicineName;

    private String shape;
    private String colour;
    private Integer unitDose;
    private int quantity;

    @Column(length = 500)
    private String instruction;

}
