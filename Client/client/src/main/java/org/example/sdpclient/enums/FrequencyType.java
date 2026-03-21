package org.example.sdpclient.enums;

import java.util.List;

public enum FrequencyType {
    ONCE_A_DAY(List.of(8)),
    TWICE_A_DAY(List.of(8, 20)),
    THREE_TIMES_A_DAY(List.of(8, 14, 20)),
    FOUR_TIMES_A_DAY(List.of(8, 12, 16, 20));

    private final List<Integer> defaultHours;

    FrequencyType(List<Integer> defaultHours) {
        this.defaultHours = defaultHours;
    }

    public List<Integer> getDefaultHours() {
        return defaultHours;
    }

    public static FrequencyType fromDisplayString(String display) {
        if (display == null) return null;
        return switch (display.trim().toLowerCase()) {
            case "once a day" -> ONCE_A_DAY;
            case "twice a day" -> TWICE_A_DAY;
            case "three times a day", "3 times a day" -> THREE_TIMES_A_DAY;
            case "four times a day", "4 times a day" -> FOUR_TIMES_A_DAY;
            default -> null;
        };
    }
}
