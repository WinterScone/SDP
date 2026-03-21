package org.example.sdpclient.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Getter
@Setter
public class DispenserSlot {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "medicine_id")
    private Medicine medicine;

    @Column(nullable = false, unique = true)
    private Integer slotNumber;

    @Column(nullable = false)
    private Integer quantityRemaining;

    @Column(nullable = false)
    private boolean active = true;
}
