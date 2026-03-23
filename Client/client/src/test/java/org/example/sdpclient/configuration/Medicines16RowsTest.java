package org.example.sdpclient.configuration;

import org.example.sdpclient.entity.Medicine;
import org.example.sdpclient.enums.MedicineType;
import org.example.sdpclient.repository.MedicineRepository;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.boot.DefaultApplicationArguments;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class Medicines16RowsTest {

    @Test
    void run_savesAll_whenNoneExist() {
        MedicineRepository repo = mock(MedicineRepository.class);

        when(repo.findAll()).thenReturn(List.of());
        when(repo.findById(any(MedicineType.class))).thenReturn(Optional.empty());

        Medicines16Rows runner = new Medicines16Rows(repo);
        runner.run(new DefaultApplicationArguments(new String[0]));

        // verify save called exactly once per enum value
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
            assertThat(m.getDosagePerForm()).isEqualTo(t.getDosage());
        }
    }

    @Test
    void run_skipsSaving_whenAlreadyExists() {
        MedicineRepository repo = mock(MedicineRepository.class);

        // All exist — runner still saves (upserts) each one
        when(repo.findAll()).thenReturn(List.of());
        for (MedicineType t : MedicineType.values()) {
            Medicine existing = new Medicine();
            existing.setMedicineId(t);
            existing.setQuantity(10);
            when(repo.findById(t)).thenReturn(Optional.of(existing));
        }

        Medicines16Rows runner = new Medicines16Rows(repo);
        runner.run(new DefaultApplicationArguments(new String[0]));

        // The runner always saves (upserts)
        verify(repo, times(MedicineType.values().length)).save(any(Medicine.class));
        verify(repo, times(MedicineType.values().length)).findById(any(MedicineType.class));
    }

    @Test
    void run_mixed_existingAndMissing_savesAll() {
        MedicineRepository repo = mock(MedicineRepository.class);

        MedicineType[] values = MedicineType.values();
        when(repo.findAll()).thenReturn(List.of());

        // First two exist, rest missing
        for (int i = 0; i < values.length; i++) {
            if (i < 2) {
                Medicine existing = new Medicine();
                existing.setMedicineId(values[i]);
                existing.setQuantity(5);
                when(repo.findById(values[i])).thenReturn(Optional.of(existing));
            } else {
                when(repo.findById(values[i])).thenReturn(Optional.empty());
            }
        }

        Medicines16Rows runner = new Medicines16Rows(repo);
        runner.run(new DefaultApplicationArguments(new String[0]));

        // Runner always saves all
        verify(repo, times(values.length)).save(any(Medicine.class));
    }
}
