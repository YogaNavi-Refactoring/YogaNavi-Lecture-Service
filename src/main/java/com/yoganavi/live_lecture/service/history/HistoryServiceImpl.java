package com.yoganavi.live_lecture.service.history;

import com.yoganavi.live_lecture.dto.LectureHistoryDto;
import java.util.List;
import org.springframework.stereotype.Service;
import com.yoganavi.live_lecture.repository.LiveLecturesRepository;
import com.yoganavi.live_lecture.repository.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.service.spi.ServiceException;
import org.springframework.data.domain.PageRequest;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class HistoryServiceImpl implements HistoryService {

    private final LiveLecturesRepository liveLecturesRepository;
    private final UserRepository userRepository;

    @Override
    @Transactional(readOnly = true)
    public List<LectureHistoryDto> getHistory(Long userId, int page, int size) {
        log.info("사용자: {}의 수강 이력 조회, page: {}, size: {}", userId, page, size);

        try {
            if (!userRepository.existsById(userId)) {
                throw new EntityNotFoundException("사용자 없음. id: " + userId);
            }

            return liveLecturesRepository.findCompletedLectures(
                userId,
                LocalDateTime.now(),
                PageRequest.of(page, size)
            );

        } catch (EntityNotFoundException e) {
            log.error("사용자 없음: {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("수강 이력 조회중 오류 발생: {}", e.getMessage());
            throw new ServiceException("수강 이력 조회 중 오류 발생", e);
        }
    }
}