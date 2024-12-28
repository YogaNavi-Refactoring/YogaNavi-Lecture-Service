package com.yoganavi.lecture.common.repository;

import com.querydsl.core.types.ExpressionUtils;
import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.core.types.dsl.NumberExpression;
import com.querydsl.core.types.dsl.NumberPath;
import com.querydsl.jpa.JPAExpressions;
import com.querydsl.jpa.JPQLQuery;
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
public class AllRecordedLecturesRepository {

    private final JPAQueryFactory queryFactory;

    public List<LectureDto> findAllLectures(Users user, int page, int size, String sort) {
        QRecordedLecture lecture = QRecordedLecture.recordedLecture;
        QRecordedLectureLike like = QRecordedLectureLike.recordedLectureLike;
        NumberPath<Long> likeCount = Expressions.numberPath(Long.class, "likeCount");

        NumberExpression<Long> likeCountExpression = like.count();

        JPQLQuery<LectureDto> query = queryFactory
            .select(Projections.constructor(LectureDto.class,
                lecture.id.as("recordedId"),
                lecture.user.userId.as("userId"),
                lecture.user.nickname.as("nickname"),
                lecture.title.as("recordTitle"),
                lecture.content.as("recordContent"),
                lecture.thumbnail.as("recordThumbnail"),
                lecture.thumbnailSmall.as("recordThumbnailSmall"),
                ExpressionUtils.as(
                    JPAExpressions
                        .select(likeCountExpression)
                        .from(like)
                        .where(like.lecture.eq(lecture)),
                    likeCount),
                lecture.createdDate,
                lecture.lastModifiedDate,
                JPAExpressions
                    .selectOne()
                    .from(like)
                    .where(like.lecture.eq(lecture).and(like.user.userId.eq(user.getUserId())))
                    .exists()
            ))
            .from(lecture)
            .where(lecture.user.isDeleted.eq(false));

        // 정렬
        if ("fame".equals(sort)) {
            query.orderBy(likeCountExpression.desc(), lecture.createdDate.desc());
        } else {
            query.orderBy(lecture.createdDate.desc());
        }

        // 페이지네이션
        query.offset((long) page * size)
            .limit(size);

        return query.fetch();
    }
}