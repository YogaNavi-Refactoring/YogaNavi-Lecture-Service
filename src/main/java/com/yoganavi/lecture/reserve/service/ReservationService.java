package com.yoganavi.lecture.reserve.service;

import com.yoganavi.lecture.reserve.dto.LiveLectureDto;
import com.yoganavi.lecture.reserve.dto.ReservationInfoDto;
import com.yoganavi.lecture.reserve.dto.ReservationRequestDto;
import java.util.List;

public interface ReservationService {

    void createReservation(long userId, ReservationRequestDto reservationRequest);

    List<ReservationInfoDto> getLiveLecturesByTeacherAndMethod(long teacherId, int method);

    List<LiveLectureDto> getAllLiveLectures(int method);
}
