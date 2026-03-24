package org.example.sdpclient.dto;

import java.util.List;

public record CollectionSlotDto(
        String time,
        List<SlotMemberDto> medications
) {}
