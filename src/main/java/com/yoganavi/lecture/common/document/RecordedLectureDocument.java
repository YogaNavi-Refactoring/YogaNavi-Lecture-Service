package com.yoganavi.lecture.common.document;

import java.time.LocalDateTime;
import java.util.List;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.*;

@Getter
@Setter
@Document(indexName = "recorded_lectures")
public class RecordedLectureDocument {

    @Id
    private Long id;

    @Field(type = FieldType.Nested)
    private InstructorInfo instructor;

    @Field(type = FieldType.Text, analyzer = "nori")
    private String title;

    @Field(type = FieldType.Text, analyzer = "nori")
    private String content;

    @Field(type = FieldType.Nested)
    private List<ChapterInfo> chapters;

    @Field(type = FieldType.Date)
    private LocalDateTime createdDate;

}

@Setter
@Getter
class InstructorInfo {

    private Long userId;

    @Field(type = FieldType.Text, analyzer = "nori")
    private String nickname;
}

@Setter
@Getter
class ChapterInfo {

    private Long id;

    @Field(type = FieldType.Text, analyzer = "nori")
    private String title;

    @Field(type = FieldType.Text, analyzer = "nori")
    private String description;
}
