package com.yoganavi.lecture.common.entity;

import com.fasterxml.jackson.annotation.JsonProperty;
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
    @GeneratedValue(strategy = GenerationType.AUTO, generator = "native")
    @Column(name = "user_id", unique = true)
    private Long userId;

    @Column(unique = true, nullable = false)
    private String email;

    @Column(unique = true, nullable = false)
    private String nickname;

    @Column(length = 512)
    private String profileImageUrl;

    @Column(length = 512)
    private String profileImageUrlSmall;

    @Column(nullable = false)
    private String role;

    @Column(length = 100)
    private String content;

    @OneToMany(mappedBy = "teacher", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<TeacherLike> teacherLikes = new ArrayList<>();

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<LiveLectures> liveLectures = new ArrayList<>();

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<RecordedLecture> recordedLectures = new ArrayList<>();

    @Column
    private Instant deletedAt;

    @Column(nullable = false)
    private Boolean isDeleted = false;

    @Column(length = 512)
    private String fcmToken;

    @OneToMany(mappedBy = "user")
    private List<TeacherLike> userLikes;

    @ManyToMany(cascade = {CascadeType.PERSIST, CascadeType.MERGE}, fetch = FetchType.LAZY)
    @JoinTable(
        name = "user_hashtags",
        joinColumns = @JoinColumn(name = "user_id"),
        inverseJoinColumns = @JoinColumn(name = "hashtag_id")
    )
    private Set<Hashtag> hashtags = new HashSet<>();

    public String getRole() {
        return String.valueOf(role);
    }

    public Set<Hashtag> getHashtags() {
        return hashtags;
    }

    public void setHashtags(Set<Hashtag> hashtags) {
        this.hashtags = hashtags;
    }

    public void addHashtag(Hashtag hashtag) {
        if (this.hashtags == null) {
            this.hashtags = new HashSet<>();
        }
        this.hashtags.add(hashtag);
        if (hashtag.getUsers() == null) {
            hashtag.setUsers(new HashSet<>());
        }
        hashtag.getUsers().add(this);
    }

    public void removeHashtag(Hashtag hashtag) {
        this.hashtags.remove(hashtag);
        hashtag.getUsers().remove(this);
    }

}
