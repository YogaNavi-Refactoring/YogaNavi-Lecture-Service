package com.yoganavi.live_lecture.common.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;
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

}
