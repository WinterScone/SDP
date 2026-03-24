package org.example.sdpclient.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.List;
import java.util.Optional;

import org.example.sdpclient.entity.Medicine;
import org.example.sdpclient.enums.MedicineType;
import org.example.sdpclient.repository.MedicineRepository;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Sort;

@ExtendWith(MockitoExtension.class)
class MedicineServiceTest {

    @Mock
    private MedicineRepository repo;

    @Mock
    private ActivityLogService activityLogService;

    @InjectMocks
    private MedicineService service;

    @Test
    void getAll_shouldCallFindAllSortedByMedicineId() {
        List<Medicine> medicines = List.of(new Medicine(), new Medicine());
        when(repo.findAll(Sort.by("medicineId"))).thenReturn(medicines);

        List<Medicine> result = service.getAll();

        assertSame(medicines, result);
        verify(repo).findAll(Sort.by("medicineId"));
    }

    @Test
    void exists_shouldDelegateToRepository() {
        when(repo.existsById(0)).thenReturn(true);

        boolean result = service.exists(0);

        assertTrue(result);
        verify(repo).existsById(0);
    }

    @Test
    void updateQuantity_shouldUpdateAndSave_whenMedicineExists() {
        Medicine existing = new Medicine();
        existing.setMedicineId(MedicineType.VTM01.getId());
        existing.setQuantity(5);

        when(repo.findById(0)).thenReturn(Optional.of(existing));

        service.updateQuantity(0, 42, 1L, "testAdmin");

        assertEquals(42, existing.getQuantity());

        ArgumentCaptor<Medicine> captor = ArgumentCaptor.forClass(Medicine.class);
        verify(repo).save(captor.capture());
        assertSame(existing, captor.getValue());

        verify(repo).findById(0);
    }

    @Test
    void updateQuantity_shouldThrow_whenMedicineNotFound() {
        when(repo.findById(0)).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class,
                () -> service.updateQuantity(0, 10, 1L, "testAdmin"));

        verify(repo).findById(0);
        verify(repo, never()).save(any());
    }
}
