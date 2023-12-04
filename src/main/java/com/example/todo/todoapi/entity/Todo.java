package com.example.todo.todoapi.entity;

import com.example.todo.userapi.entity.User;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.GenericGenerator;

import javax.naming.Name;
import javax.persistence.*;
import java.time.LocalDateTime;

@Entity
@Getter @Setter
@ToString
@EqualsAndHashCode(of = "todoId")
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(name = "tbl.todo")
public class Todo {

    @Id
    @GeneratedValue(generator = "system-uuid")
    @GenericGenerator(name = "system-uuid", strategy = "uuid")
    private String todoId;

    @Column(nullable = false , length = 30)
    private String title; //할일

    private  boolean done; //할일 완료 여부

    private LocalDateTime createDate; //등록 시간

    //한명의 유저가 여러개의 투두를 가질 수 있다.
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;


}
