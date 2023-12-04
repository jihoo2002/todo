package com.example.todo.userapi.repository;

import com.example.todo.userapi.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, String> {

    //이메일로 회원 정보 조회
   Optional<User>  findByEmail(String email); //메서드 이름으로 쿼리메서드 만듬
    
    //이메일 중복 체크
    boolean existsByEmail(String email);
}
