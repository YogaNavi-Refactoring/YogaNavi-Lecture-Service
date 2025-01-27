package com.yoganavi.lecture.teacher.service;

import com.yoganavi.lecture.common.entity.Hashtag;
import com.yoganavi.lecture.common.entity.RecordedLecture;
import com.yoganavi.lecture.common.entity.TeacherLike;
import com.yoganavi.lecture.common.entity.Users;
import com.yoganavi.lecture.common.repository.RecordedLectureLikeRepository;
import com.yoganavi.lecture.common.repository.TeacherLikeRepository;
import com.yoganavi.lecture.common.repository.TeacherRepository;
import com.yoganavi.lecture.common.repository.UserRepository;
import com.yoganavi.lecture.teacher.dto.DetailedTeacherDto;
import com.yoganavi.lecture.teacher.dto.TeacherDto;
import com.yoganavi.lecture.teacher.dto.TeacherFilter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Slf4j
@Service
@RequiredArgsConstructor
public class TeacherServiceImpl implements TeacherService {

    private final TeacherRepository teacherRepository;
    private final TeacherLikeRepository teacherLikeRepository;
    private final UserRepository userRepository;
    private final RecordedLectureLikeRepository recordedLectureLikeRepository;

    @Override
    @Transactional(readOnly = true, isolation = Isolation.READ_COMMITTED)
    public List<TeacherDto> getAllTeachers(TeacherFilter filter, int sorting, long userId) {
        if (filter == null) {
            throw new IllegalArgumentException("필터와 사용자 ID는 null일 수 없습니다.");
        }

        if (userId <= 0) {
            throw new IllegalArgumentException("유효하지 않은 ID 값입니다.");
        }

        try {
            String days = filter.getDay();
            if (days != null && days.endsWith(",")) {
                days = days.substring(0, days.length() - 1);
            }

            String searchKeyword = StringUtils.hasText(filter.getSearchKeyword()) ?
                filter.getSearchKeyword().replace(" ", "") : "";

            List<Users> users = teacherRepository.findTeachersByLectureFilter(
                filter.getStartTimeAsInstant(),
                filter.getEndTimeAsInstant(),
                days,
                filter.getPeriod(),
                filter.getMaxLiveNum()
            );

            List<TeacherDto> result = new ArrayList<>();
            for (Users user : users) {
                if (matchesSearchCriteria(user, searchKeyword)) {
                    result.add(toTeacherDto(user, userId));
                }
            }

            sortTeachers(result, sorting);
            return result;

        } catch (Exception e) {
            log.error("강사 목록 조회 중 오류 발생", e);
            throw new IllegalStateException("강사 목록을 조회할 수 없습니다.", e);
        }
    }

    @Override
    @Transactional(readOnly = true, isolation = Isolation.READ_COMMITTED)
    public List<TeacherDto> getSortedTeachers(int sorting, long userId, String keyword) {

        if (userId <= 0) {
            throw new IllegalArgumentException("유효하지 않은 ID 값입니다.");
        }

        try {
            List<Users> users = teacherRepository.findAllTeachers();
            String searchKeyword = StringUtils.hasText(keyword) ? keyword.replace(" ", "") : "";

            List<TeacherDto> result = new ArrayList<>();
            for (Users user : users) {
                if (matchesSearchCriteria(user, searchKeyword)) {
                    result.add(toTeacherDto(user, userId));
                }
            }

            sortTeachers(result, sorting);
            return result;

        } catch (Exception e) {
            log.error("정렬된 강사 목록 조회 중 오류 발생", e);
            throw new IllegalStateException("강사 목록을 조회할 수 없습니다.", e);
        }
    }

    @Override
    public DetailedTeacherDto getTeacherById(long teacherId, long userId) {

        if (teacherId <= 0 || userId <= 0) {
            throw new IllegalArgumentException("유효하지 않은 ID 값입니다.");
        }

        try {
            Users user = teacherRepository.findById(teacherId)
                .orElseThrow(() -> new IllegalArgumentException("강사를 찾을 수 없습니다."));
            Users currentUser = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));
            ;

            boolean likedByUser =
                teacherLikeRepository.findByTeacherAndUser(user, currentUser) != null;

            // 녹화 강의 처리
            List<DetailedTeacherDto.LectureDto> sortedRecordedLectures = new ArrayList<>();
            for (RecordedLecture lecture : user.getRecordedLectures()) {
                boolean myLike = recordedLectureLikeRepository.existsByLectureAndUser(lecture,
                    currentUser);
                DetailedTeacherDto.LectureDto lectureDto = DetailedTeacherDto.LectureDto.builder()
                    .recordedId(lecture.getId().toString())
                    .recordTitle(lecture.getTitle())
                    .recordThumbnail(lecture.getThumbnail())
                    .recordThumbnailSmall(lecture.getThumbnailSmall())
                    .likeCount(recordedLectureLikeRepository.countByLecture(lecture))
                    .myLike(myLike)
                    .nickname(user.getNickname())
                    .build();
                sortedRecordedLectures.add(lectureDto);
            }
            sortedRecordedLectures.sort(
                (l1, l2) -> l2.getRecordedId().compareTo(l1.getRecordedId()));

            // 공지사항 처리
            List<DetailedTeacherDto.NoticeDto> sortedNotices = new ArrayList<>();
//            for (Article article : user.getArticles()) {
//                DetailedTeacherDto.NoticeDto noticeDto = createNoticeDto(article, user);
//                sortedNotices.add(noticeDto);
//            }
            Collections.sort(sortedNotices,
                (n1, n2) -> n2.getArticleId().compareTo(n1.getArticleId()));

            // 해시태그 처리
            Set<String> hashtagNames = new HashSet<>();
            for (Hashtag hashtag : user.getHashtags()) {
                hashtagNames.add(hashtag.getName());
            }

            String content = user.getContent();
            if (content == null || content.trim().isEmpty()) {
                content = "안녕하세요! " + user.getNickname() + "입니다.";
            }

            return DetailedTeacherDto.builder()
                .id(user.getUserId())
                .email(user.getEmail())
                .nickname(user.getNickname())
                .profileImageUrl(user.getProfileImageUrl())
                .profileImageUrlSmall(user.getProfileImageUrlSmall())
                .content(content)
                .hashtags(hashtagNames)
                .recordedLectures(sortedRecordedLectures)
                .notices(sortedNotices)
                .likeCount(user.getTeacherLikes().size())
                .liked(likedByUser)
                .build();

        } catch (Exception e) {
            log.error("강사 상세 정보 조회 중 오류 발생. teacherId={}, userId={}", teacherId, userId, e);
            throw new IllegalStateException("강사 상세 정보를 조회할 수 없습니다.", e);

        }
    }

    @Transactional(isolation = Isolation.REPEATABLE_READ)
    @Override
    public boolean toggleLike(long teacherId, long userId) {

        if (userId <= 0) {
            throw new IllegalArgumentException("유효하지 않은 ID 값입니다.");
        }

        try {
            Users teacher = teacherRepository.findById(teacherId)
                .orElseThrow(() -> new IllegalArgumentException("강사를 찾을 수 없습니다."));
            Users user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));

            TeacherLike existingLike = teacherLikeRepository.findByTeacherAndUser(teacher, user);
            if (existingLike != null) {
                teacherLikeRepository.delete(existingLike);
                return false;
            }

            TeacherLike like = new TeacherLike();
            like.setTeacher(teacher);
            like.setUser(user);
            teacherLikeRepository.save(like);
            return true;

        } catch (Exception e) {
            log.error("좋아요 토글 중 오류 발생", e);
            throw new IllegalStateException("좋아요 처리 중 오류가 발생했습니다.", e);
        }
    }

    @Override
    @Transactional(readOnly = true, isolation = Isolation.READ_COMMITTED)
    public List<TeacherDto> getLikeTeachers(long userId) {
        if (userId <= 0) {
            throw new IllegalArgumentException("유효하지 않은 ID 값입니다.");
        }

        try {
            List<Users> teachers = userRepository.findLikedTeachersByUserId(userId);
            List<TeacherDto> result = new ArrayList<>();
            for (Users teacher : teachers) {
                result.add(toTeacherDto(teacher, userId));
            }
            return result;

        } catch (Exception e) {
            log.error("좋아요한 강사 목록 조회 중 오류 발생", e);
            throw new IllegalStateException("좋아요한 강사 목록을 조회할 수 없습니다.", e);
        }
    }

    private TeacherDto toTeacherDto(Users user, long userId) {
        Users currentUser = teacherRepository.findById(userId)
            .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));

        boolean likedByUser = teacherLikeRepository.findByTeacherAndUser(user, currentUser) != null;
        Set<String> hashtagNames = new HashSet<>();
        for (Hashtag hashtag : user.getHashtags()) {
            hashtagNames.add(hashtag.getName());
        }

        return new TeacherDto(
            user.getUserId(),
            user.getEmail(),
            user.getNickname(),
            user.getProfileImageUrl(),
            user.getProfileImageUrlSmall(),
            user.getContent(),
            hashtagNames,
            likedByUser,
            user.getTeacherLikes().size()
        );
    }

    private boolean matchesSearchCriteria(Users user, String searchKeyword) {
        if (!StringUtils.hasText(searchKeyword)) {
            return true;
        }

        if (user.getNickname().replace(" ", "").contains(searchKeyword)) {
            return true;
        }

        for (Hashtag hashtag : user.getHashtags()) {
            if (hashtag.getName().replace(" ", "").contains(searchKeyword)) {
                return true;
            }
        }

        return false;
    }

    private void sortTeachers(List<TeacherDto> teachers, int sorting) {
        if (sorting == 0) {
            teachers.sort((t1, t2) -> Long.compare(t2.getId(), t1.getId()));
        } else {
            teachers.sort((t1, t2) -> Integer.compare(t2.getLikeCount(), t1.getLikeCount()));
        }
    }
}