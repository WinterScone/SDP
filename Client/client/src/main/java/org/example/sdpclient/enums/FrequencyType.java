package org.example.sdpclient.enums;

import java.util.List;

public enum FrequencyType {
    ONCE_A_DAY(1, List.of(8)),
    TWICE_A_DAY(2, List.of(8, 20)),
    THREE_TIMES_A_DAY(3, List.of(8, 14, 20)),
    FOUR_TIMES_A_DAY(4, List.of(8, 12, 16, 20));

    private final int value;
    private final List<Integer> defaultHours;

    FrequencyType(int value, List<Integer> defaultHours) {
        this.value = value;
        this.defaultHours = defaultHours;
    }

    public int getValue() {
        return value;
    }

    public List<Integer> getDefaultHours() {
        return defaultHours;
    }

    public static FrequencyType fromValue(int value) {
        for (FrequencyType ft : values()) {
            if (ft.value == value) return ft;
        }
        throw new IllegalArgumentException("Unknown frequency value: " + value);
    }
}
