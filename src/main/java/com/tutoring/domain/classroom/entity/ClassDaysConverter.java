package com.tutoring.domain.classroom.entity;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import java.time.DayOfWeek;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * class_days 컬럼 ↔ List&lt;DayOfWeek&gt; 변환.
 * 저장 형식: 3글자 약어 CSV (예: "MON,WED,FRI"). DayOfWeek.name() 앞 3글자가 표준 약어와 일치.
 */
@Converter
public class ClassDaysConverter implements AttributeConverter<List<DayOfWeek>, String> {

    @Override
    public String convertToDatabaseColumn(List<DayOfWeek> days) {
        if (days == null || days.isEmpty()) {
            return null;
        }
        return days.stream()
            .map(d -> d.name().substring(0, 3))
            .collect(Collectors.joining(","));
    }

    @Override
    public List<DayOfWeek> convertToEntityAttribute(String dbData) {
        if (dbData == null || dbData.isBlank()) {
            return List.of();
        }
        return Arrays.stream(dbData.split(","))
            .map(String::trim)
            .filter(s -> !s.isEmpty())
            .map(ClassDaysConverter::toDayOfWeek)
            .toList();
    }

    private static DayOfWeek toDayOfWeek(String abbr) {
        String upper = abbr.toUpperCase();
        for (DayOfWeek d : DayOfWeek.values()) {
            if (d.name().startsWith(upper)) {
                return d;
            }
        }
        throw new IllegalArgumentException("유효하지 않은 요일 값: " + abbr);
    }
}
