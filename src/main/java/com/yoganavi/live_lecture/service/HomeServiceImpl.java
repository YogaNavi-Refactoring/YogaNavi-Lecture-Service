package com.yoganavi.live_lecture.service;

import com.yoganavi.live_lecture.dto.HomeResponseDto;
import com.yoganavi.live_lecture.repository.LiveLecturesRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class HomeServiceImpl implements HomeService {

    private final LiveLecturesRepository liveLecturesRepository;

    @Override
    public List<HomeResponseDto> getHomeData(Long userId, int page, int size) {
        return null;
    }


    @Override
    public boolean updateLiveState(Long liveId, Boolean isOnAir) {
        return false;
    }
}
