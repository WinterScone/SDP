package org.example.sdpclient.dto;

import java.util.List;

public record ClusteringResultDto(
        List<CollectionSlotDto> slots,
        int originalDistinctTimes,
        int clusteredDistinctTimes,
        List<String> warnings
) {}
