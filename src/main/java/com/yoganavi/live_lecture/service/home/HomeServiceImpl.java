package com.yoganavi.live_lecture.service.home;

import com.yoganavi.live_lecture.common.entity.LectureSchedule;
import com.yoganavi.live_lecture.common.entity.LiveLectures;
import com.yoganavi.live_lecture.common.entity.Users;
import com.yoganavi.live_lecture.dto.HomeResponseDto;
import com.yoganavi.live_lecture.repository.LiveLecturesRepository;
import com.yoganavi.live_lecture.repository.MyLiveLectureRepository;
import com.yoganavi.live_lecture.repository.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.service.spi.ServiceException;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class HomeServiceImpl implements HomeService {

    private final LiveLecturesRepository liveLecturesRepository;
    private final MyLiveLectureRepository myLiveLectureRepository;
    private final UserRepository userRepository;

    /**
     * 사용자 강의 목록 조회
     */
    @Override
    @Transactional(readOnly = true, isolation = Isolation.READ_COMMITTED)
    public List<HomeResponseDto> getHomeData(Long userId, int page, int size) {
        log.info("사용자: {}의 홈 화면 데이터 조회, page: {}, size: {}", userId, page, size);

        try {
            if (!userRepository.existsById(userId)) {
                throw new EntityNotFoundException("사용자 없음. id: " + userId);
            }


            return liveLecturesRepository.findAllMyLectures(
                userId,
                LocalDateTime.now(),
                PageRequest.of(page, size)
            );

        } catch (EntityNotFoundException e) {
            log.error("사용자 없음: {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("홈 데이터 조회중 오류 발생: {}", e.getMessage());
            throw new ServiceException("홈 데이터 조회 중 오류 발생. 강의 데이터 조회 불가", e);
        }
    }


    @Override
    public boolean updateLiveState(Long liveId, Boolean isOnAir) {
        return false;
    }
}
