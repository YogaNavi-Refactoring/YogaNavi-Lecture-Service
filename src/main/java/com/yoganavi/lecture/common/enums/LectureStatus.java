package com.yoganavi.lecture.common.enums;

public enum LectureStatus {
    UPCOMING("예정된 강의"),
    ACTIVE("진행 중"),
    COMPLETED("종료됨");

    private final String description;

    LectureStatus(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}