package com.yoganavi.lecture.recorded_lecture.service.search;

import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import co.elastic.clients.elasticsearch._types.query_dsl.QueryBuilders;
import com.yoganavi.lecture.common.document.RecordedLectureDocument;
import com.yoganavi.lecture.common.entity.RecordedLecture;
import com.yoganavi.lecture.common.entity.Users;
import com.yoganavi.lecture.common.repository.RecordedLectureRepository;
import com.yoganavi.lecture.common.repository.UserRepository;
import com.yoganavi.lecture.recorded_lecture.dto.LectureDto;
import com.yoganavi.lecture.recorded_lecture.service.recordedLectureServiceUtil.RecordedLectureServiceUtil;
import jakarta.persistence.EntityNotFoundException;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.service.spi.ServiceException;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.elasticsearch.client.elc.NativeQuery;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.data.elasticsearch.core.query.HighlightQuery;
import org.springframework.data.elasticsearch.core.query.highlight.Highlight;
import org.springframework.data.elasticsearch.core.query.highlight.HighlightField;
import org.springframework.data.elasticsearch.core.query.highlight.HighlightParameters;
import org.springframework.stereotype.Service;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.transaction.annotation.Transactional;


@Slf4j
@Service
@RequiredArgsConstructor
public class SearchRecordedLectureImpl implements SearchRecordedLecture {

    RecordedLectureServiceUtil recordedLectureServiceUtil;
    private final UserRepository userRepository;
    private final ElasticsearchOperations elasticsearchOperations;
    private final RecordedLectureRepository recordedLectureRepository;

    @Override
    @Transactional(readOnly = true)
    public List<LectureDto> searchLectures(Long userId, String keyword, String sort, int page,
        int size, boolean title, boolean content) {

        // 입력값 검증
        if (userId == null) {
            throw new IllegalArgumentException("사용자 ID는 필수입니다.");
        }

        try {
            Users user = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException(
                    "사용자를 찾을 수 없습니다. id" + userId));

            // 검색 쿼리 생성
            Query query;
            try {
                if (title && content) {
                    query = QueryBuilders.multiMatch()
                        .fields("title^2", "content", "instructor.nickname^1.5")
                        .query(keyword)
                        .build()._toQuery();
                } else if (title) {
                    query = QueryBuilders.match()
                        .field("title")
                        .query(keyword)
                        .build()._toQuery();
                } else if (content) {
                    query = QueryBuilders.match()
                        .field("content")
                        .query(keyword)
                        .build()._toQuery();
                } else {
                    query = QueryBuilders.matchAll().build()._toQuery();
                }
            } catch (Exception e) {
                log.error("검색 쿼리 생성 중 오류: keyword={}, error={}", keyword, e.getMessage());
                throw new ServiceException("검색 쿼리 생성 중 오류가 발생했습니다.", e);
            }

            // 페이징 설정
            PageRequest pageable = PageRequest.of(page, size,
                Sort.by(Sort.Direction.DESC, "createdDate"));

            // 검색 실행
            try {
                NativeQuery searchQuery = NativeQuery.builder()
                    .withQuery(query)
                    .withPageable(pageable)
                    .withHighlightQuery(new HighlightQuery(
                        new Highlight(
                            List.of(
                                new HighlightField("title"),
                                new HighlightField("content")
                            )
                        ),
                        HighlightParameters.builder().build().getClass()
                    ))
                    .build();

                SearchHits<RecordedLectureDocument> searchHits =
                    elasticsearchOperations.search(searchQuery, RecordedLectureDocument.class);

                List<Long> lectureIds = new ArrayList<>();
                for (SearchHit<RecordedLectureDocument> hit : searchHits) {
                    if (hit.getContent() != null && hit.getContent().getId() != null) {
                        lectureIds.add(hit.getContent().getId());
                    }
                }

                if (lectureIds.isEmpty()) {
                    return new ArrayList<>();
                }

                List<LectureDto> results = new ArrayList<>();
                for (RecordedLecture lecture : recordedLectureRepository.findAllById(lectureIds)) {
                    try {
                        LectureDto dto = recordedLectureServiceUtil.convertToDto(lecture);
                        try {
                            recordedLectureServiceUtil.getLikeInformation(dto, lecture, user);
                        } catch (Exception e) {
                            // 좋아요 정보 조회 실패 시 기본값 사용
                            log.warn("좋아요 정보 조회 실패: lectureId={}, error={}", lecture.getId(),
                                e.getMessage());
                            dto.setLikeCount(0L);
                            dto.setMyLike(false);
                        }
                        results.add(dto);
                    } catch (Exception e) {
                        log.warn("강의 정보 변환 중 오류 (계속 진행): lectureId={}, error={}",
                            lecture.getId(), e.getMessage());
                    }
                }

                return results;

            } catch (Exception e) {
                log.error("강의 검색 중 오류: keyword={}, error={}", keyword, e.getMessage());
                throw new ServiceException("강의 검색 중 오류 발생", e);
            }
        } catch (EntityNotFoundException e) {
            log.error("사용자 조회 실패: {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("검색 처리 중 예기치 않은 오류: keyword={}, error={}", keyword, e.getMessage());
            throw new ServiceException("강의 검색 중 오류 발생", e);
        }
    }
}
