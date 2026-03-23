package org.example.sdpclient.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.example.sdpclient.enums.FrequencyType;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "prescription")
@Getter
@Setter
@NoArgsConstructor
public class Prescription {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "patient_id")
    private Patient patient;

    @ManyToOne(optional = false)
    @JoinColumn(name = "medicine_id")
    private Medicine medicine;

    @Column(nullable = false)
    private String dosage;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private FrequencyType frequency;

    @Column(columnDefinition = "boolean default true")
    private boolean active = true;

    @Column(name = "start_date")
    private LocalDate startDate;

    @Column(name = "end_date")
    private LocalDate endDate;

    @OneToMany(mappedBy = "prescription", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("reminderTime ASC")
    private List<PrescriptionReminderTime> reminderTimes = new ArrayList<>();
}


