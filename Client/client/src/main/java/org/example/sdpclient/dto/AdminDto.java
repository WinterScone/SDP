package org.example.sdpclient.dto;

public record AdminDto(
        Long id,
        String username,
        String firstName,
        String lastName,
        String email,
        String phone,
        boolean root
) { }