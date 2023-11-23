package com.example.todo.todoapi.dto.request;


import lombok.*;

import javax.validation.constraints.NotBlank;

@Setter
@Getter
@ToString
@EqualsAndHashCode
@NoArgsConstructor
@AllArgsConstructor
@Builder
//체크박스를 눌렀을 때 done의 상태를 변경해줄 dto
public class TodoModifyRequestDTO {

    @NotBlank
    private String id;
    private boolean done;
}
