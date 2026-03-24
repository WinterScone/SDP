package org.example.sdpclient.configuration;

import org.example.sdpclient.entity.Medicine;
import org.example.sdpclient.enums.MedicineType;
import org.example.sdpclient.repository.MedicineRepository;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.boot.DefaultApplicationArguments;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class SeedMedicineTest {

    private static final List<String> VALID_IDS = Arrays.stream(MedicineType.values()).map(Enum::name).toList();

    @Test
    void run_savesAll_whenNoneExist() {
        MedicineRepository repo = mock(MedicineRepository.class);
        when(repo.findById(any(MedicineType.class))).thenReturn(Optional.empty());

        SeedMedicine runner = new SeedMedicine(repo);
        runner.run(new DefaultApplicationArguments(new String[0]));

        verify(repo).deleteOrphanMedicines(VALID_IDS);

        ArgumentCaptor<Medicine> captor = ArgumentCaptor.forClass(Medicine.class);
        verify(repo, times(MedicineType.values().length)).save(captor.capture());

        var saved = captor.getAllValues();
        assertThat(saved).hasSize(MedicineType.values().length);

        for (int i = 0; i < MedicineType.values().length; i++) {
            MedicineType t = MedicineType.values()[i];
            Medicine m = saved.get(i);

            assertThat(m.getMedicineId()).isEqualTo(t);
            assertThat(m.getMedicineName()).isEqualTo(t.getName());
            assertThat(m.getShape()).isEqualTo(t.getShape());
            assertThat(m.getColour()).isEqualTo(t.getColour());
            assertThat(m.getUnitDose()).isEqualTo(t.getDosage());
        }
    }

    @Test
    void run_updatesExisting_preservesQuantityAndInstruction() {
        MedicineRepository repo = mock(MedicineRepository.class);

        Medicine existing = new Medicine();
        existing.setMedicineId(MedicineType.VTM01);
        existing.setMedicineName("Old Name");
        existing.setQuantity(42);
        existing.setInstruction("Take with food");

        when(repo.findById(MedicineType.VTM01)).thenReturn(Optional.of(existing));
        for (MedicineType t : MedicineType.values()) {
            if (t != MedicineType.VTM01) {
                when(repo.findById(t)).thenReturn(Optional.empty());
            }
        }

        SeedMedicine runner = new SeedMedicine(repo);
        runner.run(new DefaultApplicationArguments(new String[0]));

        assertThat(existing.getMedicineName()).isEqualTo("Vitamin C");
        assertThat(existing.getQuantity()).isEqualTo(42);
        assertThat(existing.getInstruction()).isEqualTo("Take with food");

        verify(repo, times(MedicineType.values().length)).save(any(Medicine.class));
    }

    @Test
    void run_callsOrphanCleanupInCorrectOrder() {
        MedicineRepository repo = mock(MedicineRepository.class);
        when(repo.findById(any(MedicineType.class))).thenReturn(Optional.empty());

        SeedMedicine runner = new SeedMedicine(repo);
        runner.run(new DefaultApplicationArguments(new String[0]));

        var inOrder = inOrder(repo);
        inOrder.verify(repo).deleteOrphanReminderTimes(VALID_IDS);
        inOrder.verify(repo).deleteOrphanReminderLogs(VALID_IDS);
        inOrder.verify(repo).deleteOrphanIntakeHistory(VALID_IDS);
        inOrder.verify(repo).deleteOrphanPrescriptions(VALID_IDS);
        inOrder.verify(repo).deleteOrphanDispenserSlots(VALID_IDS);
        inOrder.verify(repo).deleteOrphanMedicines(VALID_IDS);
    }

    @Test
    void run_fixesNegativeQuantity() {
        MedicineRepository repo = mock(MedicineRepository.class);

        Medicine negativeQty = new Medicine();
        negativeQty.setMedicineId(MedicineType.VTM01);
        negativeQty.setQuantity(-5);

        when(repo.findById(MedicineType.VTM01)).thenReturn(Optional.of(negativeQty));
        for (MedicineType t : MedicineType.values()) {
            if (t != MedicineType.VTM01) {
                when(repo.findById(t)).thenReturn(Optional.empty());
            }
        }

        SeedMedicine runner = new SeedMedicine(repo);
        runner.run(new DefaultApplicationArguments(new String[0]));

        assertThat(negativeQty.getQuantity()).isEqualTo(0);
    }
}
