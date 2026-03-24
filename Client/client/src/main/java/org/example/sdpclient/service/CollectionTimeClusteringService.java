package org.example.sdpclient.service;

import org.example.sdpclient.dto.*;
import org.example.sdpclient.entity.Prescription;
import org.example.sdpclient.entity.PrescriptionReminderTime;
import org.example.sdpclient.enums.FrequencyType;
import org.example.sdpclient.repository.PrescriptionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalTime;
import java.time.Duration;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class CollectionTimeClusteringService {

    private static final Logger log = LoggerFactory.getLogger(CollectionTimeClusteringService.class);

    static final int MAX_DEVIATION_MINUTES = 60;
    static final int MIN_SAME_MED_GAP_HOURS = 3;
    static final LocalTime WAKING_START = LocalTime.of(7, 0);
    static final LocalTime WAKING_END = LocalTime.of(22, 0);

    private final PrescriptionRepository prescriptionRepository;

    public CollectionTimeClusteringService(PrescriptionRepository prescriptionRepository) {
        this.prescriptionRepository = prescriptionRepository;
    }

    @Transactional(readOnly = true)
    public ClusteringResultDto computeClusteredTimes(Long patientId) {
        List<Prescription> prescriptions = prescriptionRepository.findByPatientIdAndActiveTrue(patientId);
        return computeClusteredTimesFromPrescriptions(prescriptions);
    }

    ClusteringResultDto computeClusteredTimesFromPrescriptions(List<Prescription> prescriptions) {
        if (prescriptions.isEmpty()) {
            return new ClusteringResultDto(List.of(), 0, 0, List.of());
        }

        // Step 1: Build dose events
        List<DoseEvent> events = buildDoseEvents(prescriptions);
        if (events.isEmpty()) {
            return new ClusteringResultDto(List.of(), 0, 0, List.of());
        }

        // Count original distinct times
        Set<LocalTime> originalTimes = events.stream()
                .map(DoseEvent::targetTime)
                .collect(Collectors.toSet());
        int originalDistinct = originalTimes.size();

        // Step 2: Sort by target time
        events.sort(Comparator.comparing(DoseEvent::targetTime));

        // Step 3: Greedy merge
        List<CollectionSlot> slots = new ArrayList<>();
        List<String> warnings = new ArrayList<>();

        for (DoseEvent event : events) {
            CollectionSlot bestSlot = findBestSlot(event, slots);
            if (bestSlot != null) {
                bestSlot.addMember(event);
                bestSlot.recalculateTime();
            } else {
                CollectionSlot newSlot = new CollectionSlot(event.targetTime());
                newSlot.addMember(event);
                slots.add(newSlot);
            }
        }

        // Sort slots by time
        slots.sort(Comparator.comparing(s -> s.slotTime));

        // Step 4: Validate same-medication gaps
        validateSameMedGaps(slots, warnings);

        // Build result DTOs
        Map<Integer, String> medicineNames = new HashMap<>();
        for (Prescription rx : prescriptions) {
            medicineNames.put(rx.getMedicine().getMedicineId(), rx.getMedicine().getMedicineName());
        }

        List<CollectionSlotDto> slotDtos = slots.stream()
                .map(slot -> new CollectionSlotDto(
                        formatTime(slot.slotTime),
                        slot.members.stream()
                                .map(e -> new SlotMemberDto(
                                        e.prescriptionId(),
                                        medicineNames.getOrDefault(e.medicineId(), "Unknown"),
                                        formatTime(e.targetTime()),
                                        formatTime(slot.slotTime)
                                ))
                                .toList()
                ))
                .toList();

        return new ClusteringResultDto(slotDtos, originalDistinct, slotDtos.size(), warnings);
    }

    @Transactional
    public void applyClusteredTimes(Long patientId) {
        List<Prescription> prescriptions = prescriptionRepository.findByPatientIdAndActiveTrue(patientId);
        if (prescriptions.isEmpty()) return;

        ClusteringResultDto result = computeClusteredTimesFromPrescriptions(prescriptions);

        // Build a map: prescriptionId -> list of clustered times
        Map<Long, List<LocalTime>> prescriptionTimes = new HashMap<>();
        for (CollectionSlotDto slot : result.slots()) {
            LocalTime slotTime = LocalTime.parse(slot.time());
            for (SlotMemberDto member : slot.medications()) {
                prescriptionTimes.computeIfAbsent(member.prescriptionId(), k -> new ArrayList<>())
                        .add(slotTime);
            }
        }

        // Apply clustered times to each prescription
        for (Prescription rx : prescriptions) {
            List<LocalTime> times = prescriptionTimes.getOrDefault(rx.getId(), List.of());

            rx.getReminderTimes().clear();
            for (LocalTime time : times) {
                PrescriptionReminderTime prt = new PrescriptionReminderTime();
                prt.setPrescription(rx);
                prt.setReminderTime(time);
                rx.getReminderTimes().add(prt);
            }

            prescriptionRepository.save(rx);
        }

        log.info("Applied clustered times for patient {}: {} slots from {} original times",
                patientId, result.clusteredDistinctTimes(), result.originalDistinctTimes());
    }

    List<DoseEvent> buildDoseEvents(List<Prescription> prescriptions) {
        List<DoseEvent> events = new ArrayList<>();

        for (Prescription rx : prescriptions) {
            List<LocalTime> times;

            if (rx.getReminderTimes() != null && !rx.getReminderTimes().isEmpty()) {
                times = rx.getReminderTimes().stream()
                        .map(PrescriptionReminderTime::getReminderTime)
                        .sorted()
                        .toList();
            } else {
                // Fall back to FrequencyType defaults
                if (rx.getFrequency() < 1 || rx.getFrequency() > 4) continue;
                FrequencyType freq = FrequencyType.fromValue(rx.getFrequency());
                times = freq.getDefaultHours().stream()
                        .map(h -> LocalTime.of(h, 0))
                        .toList();
            }

            for (int i = 0; i < times.size(); i++) {
                LocalTime target = times.get(i);
                LocalTime earliest = target.minusMinutes(MAX_DEVIATION_MINUTES);
                LocalTime latest = target.plusMinutes(MAX_DEVIATION_MINUTES);

                // Clamp to waking hours
                if (earliest.isBefore(WAKING_START)) earliest = WAKING_START;
                if (latest.isAfter(WAKING_END)) latest = WAKING_END;

                events.add(new DoseEvent(
                        rx.getId(),
                        rx.getMedicine().getMedicineId(),
                        target,
                        earliest,
                        latest,
                        i,
                        times.size()
                ));
            }
        }

        return events;
    }

    private CollectionSlot findBestSlot(DoseEvent event, List<CollectionSlot> slots) {
        CollectionSlot best = null;
        long bestDistance = Long.MAX_VALUE;

        for (CollectionSlot slot : slots) {
            // Check if slot time is within the event's allowed window
            if (slot.slotTime.isBefore(event.earliestAllowed()) || slot.slotTime.isAfter(event.latestAllowed())) {
                continue;
            }

            // Check no same-medication duplicate in slot
            boolean hasSameMed = slot.members.stream()
                    .anyMatch(m -> m.medicineId().equals(event.medicineId()));
            if (hasSameMed) continue;

            // Check MIN_SAME_MED_GAP with adjacent slots for the same medication
            if (wouldViolateSameMedGap(event, slot, slots)) continue;

            long distance = Math.abs(Duration.between(event.targetTime(), slot.slotTime).toMinutes());
            if (distance < bestDistance) {
                bestDistance = distance;
                best = slot;
            }
        }

        return best;
    }

    private boolean wouldViolateSameMedGap(DoseEvent event, CollectionSlot candidateSlot, List<CollectionSlot> allSlots) {
        for (CollectionSlot otherSlot : allSlots) {
            if (otherSlot == candidateSlot) continue;

            boolean hasSameMed = otherSlot.members.stream()
                    .anyMatch(m -> m.medicineId().equals(event.medicineId()));
            if (!hasSameMed) continue;

            long gapMinutes = Math.abs(Duration.between(candidateSlot.slotTime, otherSlot.slotTime).toMinutes());
            if (gapMinutes < MIN_SAME_MED_GAP_HOURS * 60L) {
                return true;
            }
        }
        return false;
    }

    private void validateSameMedGaps(List<CollectionSlot> slots, List<String> warnings) {
        // Group all slot assignments by medicine
        Map<Integer, List<SlotAssignment>> medSlots = new HashMap<>();
        for (CollectionSlot slot : slots) {
            for (DoseEvent member : slot.members) {
                medSlots.computeIfAbsent(member.medicineId(), k -> new ArrayList<>())
                        .add(new SlotAssignment(slot, member));
            }
        }

        for (Map.Entry<Integer, List<SlotAssignment>> entry : medSlots.entrySet()) {
            List<SlotAssignment> assignments = entry.getValue();
            if (assignments.size() < 2) continue;

            assignments.sort(Comparator.comparing(a -> a.slot.slotTime));

            for (int i = 1; i < assignments.size(); i++) {
                SlotAssignment prev = assignments.get(i - 1);
                SlotAssignment curr = assignments.get(i);
                long gapMinutes = Duration.between(prev.slot.slotTime, curr.slot.slotTime).toMinutes();

                if (gapMinutes < MIN_SAME_MED_GAP_HOURS * 60L) {
                    // Eject the current event back to its original time
                    curr.slot.members.remove(curr.event);
                    if (curr.slot.members.isEmpty()) {
                        slots.remove(curr.slot);
                    } else {
                        curr.slot.recalculateTime();
                    }

                    // Create a new slot at the original time
                    CollectionSlot ejectedSlot = new CollectionSlot(curr.event.targetTime());
                    ejectedSlot.addMember(curr.event);
                    slots.add(ejectedSlot);
                    slots.sort(Comparator.comparing(s -> s.slotTime));

                    warnings.add("Medicine ID " + entry.getKey()
                            + ": gap between " + formatTime(prev.slot.slotTime)
                            + " and " + formatTime(curr.event.targetTime())
                            + " is less than " + MIN_SAME_MED_GAP_HOURS + "h; kept at original time");
                }
            }
        }
    }

    static String formatTime(LocalTime time) {
        return String.format("%02d:%02d", time.getHour(), time.getMinute());
    }

    static LocalTime roundTo5Min(LocalTime time) {
        int totalMinutes = time.getHour() * 60 + time.getMinute();
        int rounded = ((totalMinutes + 2) / 5) * 5;
        return LocalTime.of(Math.min(rounded / 60, 23), rounded % 60);
    }

    static class CollectionSlot {
        LocalTime slotTime;
        final List<DoseEvent> members = new ArrayList<>();

        CollectionSlot(LocalTime slotTime) {
            this.slotTime = slotTime;
        }

        void addMember(DoseEvent event) {
            members.add(event);
        }

        void recalculateTime() {
            if (members.isEmpty()) return;
            long totalMinutes = members.stream()
                    .mapToLong(e -> e.targetTime().getHour() * 60L + e.targetTime().getMinute())
                    .sum();
            int avgMinutes = (int) (totalMinutes / members.size());
            slotTime = roundTo5Min(LocalTime.of(avgMinutes / 60, avgMinutes % 60));
        }
    }

    private record SlotAssignment(CollectionSlot slot, DoseEvent event) {}
}
