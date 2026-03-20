package org.example.sdpclient.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.List;

import org.example.sdpclient.dto.AdminDto;
import org.example.sdpclient.entity.Admin;
import org.example.sdpclient.repository.AdminRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AdminListServiceTest {

    @Mock
    private AdminRepository adminRepo;

    @InjectMocks
    private AdminListService adminListService;

    @Test
    void getAllAdmins_shouldMapAdminsToDtos() {
        // given
        Admin admin1 = new Admin();
        admin1.setId(1L);
        admin1.setUsername("admin1");

        Admin admin2 = new Admin();
        admin2.setId(2L);
        admin2.setUsername("admin2");

        when(adminRepo.findAll()).thenReturn(List.of(admin1, admin2));

        // when
        List<AdminDto> result = adminListService.getAllAdmins();

        // then
        assertEquals(2, result.size());

        // if AdminDto is a record:
        assertEquals(1L, result.get(0).id());
        assertEquals("admin1", result.get(0).username());

        assertEquals(2L, result.get(1).id());
        assertEquals("admin2", result.get(1).username());
    }

    @Test
    void getAllAdmins_shouldReturnEmptyList_whenNoAdminsExist() {
        // given
        when(adminRepo.findAll()).thenReturn(Collections.emptyList());

        // when
        List<AdminDto> result = adminListService.getAllAdmins();

        // then
        assertNotNull(result);
        assertEquals(0, result.size());
    }
}
