package org.example.sdpclient.configuration;

import org.example.sdpclient.entity.Medicine;
import org.example.sdpclient.enums.MedicineType;
import org.example.sdpclient.repository.MedicineRepository;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.boot.DefaultApplicationArguments;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class Medicines16RowsTest {

    @Test
    void run_savesAll_whenNoneExist() {
        MedicineRepository repo = mock(MedicineRepository.class);

        // for every enum, pretend it doesn't exist
        when(repo.existsById(any(MedicineType.class))).thenReturn(false);

        Medicines16Rows runner = new Medicines16Rows(repo);

        runner.run(new DefaultApplicationArguments(new String[0]));

        // verify save called exactly once per enum value
        verify(repo, times(MedicineType.values().length)).save(any(Medicine.class));

        // verify contents of saved entities
        ArgumentCaptor<Medicine> captor = ArgumentCaptor.forClass(Medicine.class);
        verify(repo, times(MedicineType.values().length)).save(captor.capture());

        var saved = captor.getAllValues();
        assertThat(saved).hasSize(MedicineType.values().length);

        // spot-check each saved Medicine matches its enum
        // (order should match enum iteration order)
        for (int i = 0; i < MedicineType.values().length; i++) {
            MedicineType t = MedicineType.values()[i];
            Medicine m = saved.get(i);

            assertThat(m.getMedicineId()).isEqualTo(t);
            assertThat(m.getMedicineName()).isEqualTo(t.getName());
            assertThat(m.getShape()).isEqualTo(t.getShape());
            assertThat(m.getColour()).isEqualTo(t.getColour());
            assertThat(m.getDosagePerForm()).isEqualTo(t.getDosage());
            assertThat(m.getQuantity()).isEqualTo(0);
        }
    }

    @Test
    void run_skipsSaving_whenAlreadyExists() {
        MedicineRepository repo = mock(MedicineRepository.class);

        // everything already exists
        when(repo.existsById(any(MedicineType.class))).thenReturn(true);

        Medicines16Rows runner = new Medicines16Rows(repo);

        runner.run(new DefaultApplicationArguments(new String[0]));

        // should never save
        verify(repo, never()).save(any(Medicine.class));

        // but should check existence for every enum
        verify(repo, times(MedicineType.values().length)).existsById(any(MedicineType.class));
    }

    @Test
    void run_mixed_existingAndMissing_savesOnlyMissing() {
        MedicineRepository repo = mock(MedicineRepository.class);

        // Example: first two exist, rest missing
        MedicineType[] values = MedicineType.values();
        when(repo.existsById(any(MedicineType.class))).thenAnswer(inv -> {
            MedicineType id = inv.getArgument(0);
            return id == values[0] || id == values[1];
        });

        Medicines16Rows runner = new Medicines16Rows(repo);
        runner.run(new DefaultApplicationArguments(new String[0]));

        int expectedSaves = Math.max(0, values.length - 2);
        verify(repo, times(expectedSaves)).save(any(Medicine.class));
        verify(repo, times(values.length)).existsById(any(MedicineType.class));
    }
}
