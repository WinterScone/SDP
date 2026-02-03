package org.example.sdpclient.service;

import org.example.sdpclient.dto.AdminDto;
import org.example.sdpclient.repository.AdminRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class AdminListService {
    private final AdminRepository adminRepo;

    public AdminListService(AdminRepository adminRepo) {
        this.adminRepo = adminRepo;
    }

    public List<AdminDto> getAllAdmins() {
        return adminRepo.findAll()
                .stream()
                .map(a -> new AdminDto(a.getId(), a.getUsername()))
                .toList();
    }
}