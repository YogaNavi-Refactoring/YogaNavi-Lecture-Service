package com.yoganavi.lecture.common.entity;

import jakarta.persistence.*;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Getter
@Setter
@NoArgsConstructor
@Table(name = "users")
public class Users {

    @Id
    private Long userId;
    private String nickname;
    private String role;
    private String profileImageUrl;
    private String profileImageUrlSmall;
    private Boolean isActive = true;
    private LocalDateTime updatedAt;
    @Column(length = 100)
    private String content; // 강사 소개 내용

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<LiveLectures> liveLectures = new ArrayList<>();

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<MyLiveLecture> myLiveLectures = new ArrayList<>();

//    @ManyToMany(cascade = {CascadeType.PERSIST, CascadeType.MERGE}, fetch = FetchType.LAZY)
//    @JoinTable(
//        name = "user_hashtags",
//        joinColumns = @JoinColumn(name = "user_id"),
//        inverseJoinColumns = @JoinColumn(name = "hashtag_id")
//    )
//    private Set<Hashtag> hashtags = new HashSet<>();

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<RecordedLecture> recordedLectures = new ArrayList<>();

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<RecordedLectureLike> recordedLectureLikes = new ArrayList<>();

    @Column
    private Instant deletedAt;

    @Column(nullable = false)
    private Boolean isDeleted = false;

    @Column(length = 512)
    private String fcmToken;

}
