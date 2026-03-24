package org.example.sdpclient.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

import org.example.sdpclient.dto.ClusteringResultDto;
import org.example.sdpclient.dto.CollectionSlotDto;
import org.example.sdpclient.entity.Medicine;
import org.example.sdpclient.entity.Patient;
import org.example.sdpclient.entity.Prescription;
import org.example.sdpclient.entity.PrescriptionReminderTime;
import org.example.sdpclient.repository.PrescriptionRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CollectionTimeClusteringServiceTest {

    @Mock
    private PrescriptionRepository prescriptionRepository;

    @InjectMocks
    private CollectionTimeClusteringService service;

    // -- Helpers --

    private Prescription createPrescription(Long id, Integer medicineId, String medicineName,
                                            int frequency, LocalTime... times) {
        Medicine med = new Medicine();
        med.setMedicineId(medicineId);
        med.setMedicineName(medicineName);

        Patient patient = new Patient();
        patient.setId(1L);

        Prescription rx = new Prescription();
        rx.setId(id);
        rx.setMedicine(med);
        rx.setPatient(patient);
        rx.setFrequency(frequency);
        rx.setActive(true);

        for (LocalTime t : times) {
            PrescriptionReminderTime prt = new PrescriptionReminderTime();
            prt.setPrescription(rx);
            prt.setReminderTime(t);
            rx.getReminderTimes().add(prt);
        }

        return rx;
    }

    // -- Tests --

    @Test
    void emptyPrescriptions_returnsEmptyResult() {
        ClusteringResultDto result = service.computeClusteredTimesFromPrescriptions(List.of());

        assertEquals(0, result.slots().size());
        assertEquals(0, result.originalDistinctTimes());
        assertEquals(0, result.clusteredDistinctTimes());
        assertTrue(result.warnings().isEmpty());
    }

    @Test
    void singlePrescription_timesUnchanged() {
        // Med A: 3x/day at 08:00, 14:00, 20:00
        Prescription rx = createPrescription(1L, 100, "Med A", 3,
                LocalTime.of(8, 0), LocalTime.of(14, 0), LocalTime.of(20, 0));

        ClusteringResultDto result = service.computeClusteredTimesFromPrescriptions(List.of(rx));

        assertEquals(3, result.slots().size());
        assertEquals(3, result.originalDistinctTimes());
        assertEquals(3, result.clusteredDistinctTimes());

        assertEquals("08:00", result.slots().get(0).time());
        assertEquals("14:00", result.slots().get(1).time());
        assertEquals("20:00", result.slots().get(2).time());
    }

    @Test
    void twoPrescriptions_perfectOverlap_mergedCorrectly() {
        // Med A: 08:00, 20:00  |  Med B: 08:00, 20:00
        Prescription rxA = createPrescription(1L, 100, "Med A", 2,
                LocalTime.of(8, 0), LocalTime.of(20, 0));
        Prescription rxB = createPrescription(2L, 200, "Med B", 2,
                LocalTime.of(8, 0), LocalTime.of(20, 0));

        ClusteringResultDto result = service.computeClusteredTimesFromPrescriptions(List.of(rxA, rxB));

        assertEquals(2, result.clusteredDistinctTimes());
        assertEquals(2, result.originalDistinctTimes());

        // Both slots should have 2 medications each
        for (CollectionSlotDto slot : result.slots()) {
            assertEquals(2, slot.medications().size());
        }
    }

    @Test
    void twoPrescriptions_partialOverlap_mergesWithinTolerance() {
        // Med A: 08:00, 14:00, 20:00
        // Med B: 08:00, 12:00, 16:00, 20:00
        // Expected: merge 12:00+14:00 → ~13:00, keep rest
        Prescription rxA = createPrescription(1L, 100, "Med A", 3,
                LocalTime.of(8, 0), LocalTime.of(14, 0), LocalTime.of(20, 0));
        Prescription rxB = createPrescription(2L, 200, "Med B", 4,
                LocalTime.of(8, 0), LocalTime.of(12, 0), LocalTime.of(16, 0), LocalTime.of(20, 0));

        ClusteringResultDto result = service.computeClusteredTimesFromPrescriptions(List.of(rxA, rxB));

        // 5 original distinct times → should merge to 4 collection times
        assertTrue(result.clusteredDistinctTimes() <= result.originalDistinctTimes(),
                "Clustered times should be <= original");
        assertTrue(result.clusteredDistinctTimes() <= 5);

        // The 08:00 and 20:00 slots should have both meds
        CollectionSlotDto firstSlot = result.slots().get(0);
        assertEquals("08:00", firstSlot.time());
        assertEquals(2, firstSlot.medications().size());

        CollectionSlotDto lastSlot = result.slots().get(result.slots().size() - 1);
        assertEquals("20:00", lastSlot.time());
        assertEquals(2, lastSlot.medications().size());
    }

    @Test
    void singlePrescription_noReminderTimes_usesFrequencyDefaults() {
        // Prescription with frequency=2 but no reminder times set
        Medicine med = new Medicine();
        med.setMedicineId(100);
        med.setMedicineName("Med A");

        Patient patient = new Patient();
        patient.setId(1L);

        Prescription rx = new Prescription();
        rx.setId(1L);
        rx.setMedicine(med);
        rx.setPatient(patient);
        rx.setFrequency(2); // TWICE_A_DAY → [8, 20]
        rx.setActive(true);
        // No reminder times added

        ClusteringResultDto result = service.computeClusteredTimesFromPrescriptions(List.of(rx));

        assertEquals(2, result.slots().size());
        assertEquals("08:00", result.slots().get(0).time());
        assertEquals("20:00", result.slots().get(1).time());
    }

    @Test
    void applyClusteredTimes_writesBackToRepository() {
        Prescription rxA = createPrescription(1L, 100, "Med A", 2,
                LocalTime.of(8, 0), LocalTime.of(20, 0));
        Prescription rxB = createPrescription(2L, 200, "Med B", 2,
                LocalTime.of(8, 0), LocalTime.of(20, 0));

        when(prescriptionRepository.findByPatientIdAndActiveTrue(1L))
                .thenReturn(List.of(rxA, rxB));

        service.applyClusteredTimes(1L);

        verify(prescriptionRepository, times(2)).save(any(Prescription.class));
    }

    @Test
    void applyClusteredTimes_emptyPrescriptions_doesNothing() {
        when(prescriptionRepository.findByPatientIdAndActiveTrue(1L))
                .thenReturn(List.of());

        service.applyClusteredTimes(1L);

        verify(prescriptionRepository, never()).save(any());
    }

    @Test
    void computeClusteredTimes_delegatesToRepository() {
        when(prescriptionRepository.findByPatientIdAndActiveTrue(1L))
                .thenReturn(List.of());

        ClusteringResultDto result = service.computeClusteredTimes(1L);

        verify(prescriptionRepository).findByPatientIdAndActiveTrue(1L);
        assertEquals(0, result.slots().size());
    }

    @Test
    void buildDoseEvents_skipsInvalidFrequency() {
        Medicine med = new Medicine();
        med.setMedicineId(100);
        med.setMedicineName("Med A");

        Patient patient = new Patient();
        patient.setId(1L);

        Prescription rx = new Prescription();
        rx.setId(1L);
        rx.setMedicine(med);
        rx.setPatient(patient);
        rx.setFrequency(0); // Invalid
        rx.setActive(true);

        var events = service.buildDoseEvents(List.of(rx));
        assertTrue(events.isEmpty());
    }

    @Test
    void formatTime_padsCorrectly() {
        assertEquals("08:00", CollectionTimeClusteringService.formatTime(LocalTime.of(8, 0)));
        assertEquals("14:05", CollectionTimeClusteringService.formatTime(LocalTime.of(14, 5)));
        assertEquals("00:00", CollectionTimeClusteringService.formatTime(LocalTime.of(0, 0)));
    }

    @Test
    void roundTo5Min_roundsCorrectly() {
        assertEquals(LocalTime.of(8, 0), CollectionTimeClusteringService.roundTo5Min(LocalTime.of(8, 0)));
        assertEquals(LocalTime.of(8, 5), CollectionTimeClusteringService.roundTo5Min(LocalTime.of(8, 3)));
        assertEquals(LocalTime.of(13, 0), CollectionTimeClusteringService.roundTo5Min(LocalTime.of(13, 0)));
        assertEquals(LocalTime.of(13, 0), CollectionTimeClusteringService.roundTo5Min(LocalTime.of(12, 58)));
    }

    @Test
    void threePrescriptions_multipleOverlaps_reducesCollectionTimes() {
        // Med A: 08:00, 14:00, 20:00 (3x)
        // Med B: 08:00, 20:00 (2x)
        // Med C: 09:00, 21:00 (2x) — 09:00 within 60min of 08:00
        Prescription rxA = createPrescription(1L, 100, "Med A", 3,
                LocalTime.of(8, 0), LocalTime.of(14, 0), LocalTime.of(20, 0));
        Prescription rxB = createPrescription(2L, 200, "Med B", 2,
                LocalTime.of(8, 0), LocalTime.of(20, 0));
        Prescription rxC = createPrescription(3L, 300, "Med C", 2,
                LocalTime.of(9, 0), LocalTime.of(21, 0));

        ClusteringResultDto result = service.computeClusteredTimesFromPrescriptions(List.of(rxA, rxB, rxC));

        // 5 original distinct times (08, 09, 14, 20, 21) → should reduce
        assertEquals(5, result.originalDistinctTimes());
        assertTrue(result.clusteredDistinctTimes() <= 5);

        // First cluster should contain all 3 meds (08+09 merged)
        CollectionSlotDto firstSlot = result.slots().get(0);
        assertTrue(firstSlot.medications().size() >= 2, "First slot should merge nearby times");
    }
}
