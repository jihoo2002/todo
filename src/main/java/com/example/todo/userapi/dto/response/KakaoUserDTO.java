package com.example.todo.userapi.dto.response;

import com.example.todo.userapi.entity.User;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;


import java.time.LocalDateTime;

@Setter @Getter
@ToString
public class KakaoUserDTO {

    private long id;

    @JsonProperty("connected_at")
    private LocalDateTime connectedAt;

    @JsonProperty("kakao_account")
    private KakaoAccount kakaoAccount;

    @Setter @Getter @ToString
    public static class KakaoAccount {

        private String email;
        private Profile profile; //제이쓴 프로파일 안붙여도 됨, 이름 똑같음

        @Getter @Setter @ToString
        public static class Profile {
            private String nickname;

            @JsonProperty("profile_image_url")
            private String profileImageUrl;
        }



    }

    public User toEntity(String accessToken) {
        return User.builder()
                //id를 줬지만 user쪽에 uuid니까 uuid로 데이터베이스쪽으로 들ㅇ간다.
                .email(this.kakaoAccount.email)
                .userName(this.kakaoAccount.profile.nickname)
                .password("password!") //아무값이나 넣어준다.
                .profileImg(this.kakaoAccount.profile.profileImageUrl)
                .accessToken(accessToken)
                .build();
    }
}