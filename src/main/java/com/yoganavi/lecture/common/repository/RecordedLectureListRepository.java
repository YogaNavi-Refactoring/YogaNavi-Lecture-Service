package com.yoganavi.lecture.common.repository;

import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.jpa.JPAExpressions;
import com.querydsl.jpa.impl.JPAQueryFactory;
import com.yoganavi.lecture.common.entity.QRecordedLecture;
import com.yoganavi.lecture.common.entity.QRecordedLectureLike;
import com.yoganavi.lecture.common.entity.Users;
import com.yoganavi.lecture.recorded_lecture.dto.LectureDto;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class RecordedLectureListRepository {

    private final JPAQueryFactory queryFactory;

    public List<LectureDto> findAllLectures(Users user) {
        QRecordedLecture lecture = QRecordedLecture.recordedLecture;
        QRecordedLectureLike like = QRecordedLectureLike.recordedLectureLike;

        return queryFactory
            .select(Projections.constructor(LectureDto.class,
                lecture.id.as("recordedId"),
                lecture.user.userId.as("userId"),
                lecture.title.as("recordTitle"),
                lecture.thumbnailSmall.as("recordThumbnailSmall"),
                lecture.thumbnail.as("recordThumbnail"),
                Expressions.as(
                    JPAExpressions
                        .select(like.count())
                        .from(like)
                        .where(like.lecture.eq(lecture)),
                    "likeCount"
                ),
                JPAExpressions.selectOne()
                    .from(like)
                    .where(like.lecture.eq(lecture).and(like.user.eq(user)))
                    .exists().as("myLike")
            ))
            .from(lecture)
            .where(lecture.user.eq(user))
            .fetch();
    }
}
