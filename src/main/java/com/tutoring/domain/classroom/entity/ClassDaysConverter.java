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

    /**
     * 엔티티 → DB. 요일 목록을 3글자 약어 CSV 로 변환한다 (예: [MONDAY, WEDNESDAY] → "MON,WED").
     * 비었거나 null 이면 컬럼에 null 을 저장한다.
     */
    @Override
    public String convertToDatabaseColumn(List<DayOfWeek> days) {
        if (days == null || days.isEmpty()) {
            return null;
        }
        return days.stream()
            .map(d -> d.name().substring(0, 3))
            .collect(Collectors.joining(","));
    }

    /**
     * DB → 엔티티. CSV 문자열을 요일 목록으로 복원한다 (예: "MON,WED" → [MONDAY, WEDNESDAY]).
     * null·빈 문자열이면 빈 목록을 반환한다.
     */
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

    /** 3글자 약어(MON, TUE …)를 {@link DayOfWeek} 로 매핑한다. 매칭되는 요일이 없으면 예외. */
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
