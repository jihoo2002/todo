package com.example.todo.userapi.dto;

import com.example.todo.userapi.entity.User;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.*;

import java.time.LocalDateTime;

@Setter
@Getter
@ToString
@EqualsAndHashCode(of = "email")
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserSignUpResponseDTO {

    private String email;

    private  String userName;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime joinDate;

    public UserSignUpResponseDTO(User saved) {
        //엔터티를 dto로 바꾼다.
        this.email = saved.getEmail();
        this.userName = saved.getUserName();
        this.joinDate = saved.getJoinDate();



    }
}
