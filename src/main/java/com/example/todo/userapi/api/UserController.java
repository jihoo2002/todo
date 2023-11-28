package com.example.todo.userapi.api;

import com.example.todo.userapi.dto.request.LoginRequestDTO;
import com.example.todo.userapi.dto.request.UserRequestSignUpDTO;
import com.example.todo.userapi.dto.response.LoginResponseDTO;
import com.example.todo.userapi.dto.response.UserSignUpResponseDTO;
import com.example.todo.userapi.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@Slf4j
@RequiredArgsConstructor
@RequestMapping("/api/auth")
@CrossOrigin //이거 없으면 요청이 안보내짐
public class UserController {
    private final UserService userService;


    //이메일 중복 확인 요청 처리
    //GET: /api/auth/check?email=zzzz@xxx.com (예시)
    @GetMapping("/check")
    public ResponseEntity<?> check(String email) {
        //request param 생략 -> 이름이 같음
        if(email.trim().equals("")) {
            return ResponseEntity.badRequest().body("이메일이 없습니다.");
        }

      boolean resultFlag =  userService.isDuplicate(email);
        log.info("{} 중복? - {}", email, resultFlag);
        return ResponseEntity.ok().body(resultFlag);
    }




    //회원 가입 요청 처리
    //POST : /api/auth

    @PostMapping
    public ResponseEntity<?> signUp(
            @Validated @RequestBody UserRequestSignUpDTO dto,
            BindingResult result
    ) {
        log.info("/api/auth POST! - {}", dto);

        if(result.hasErrors()) {
            log.warn(result.toString());
            return ResponseEntity.badRequest()
                    .body(result.getFieldError());
        }

        try {
            UserSignUpResponseDTO responseDTO = userService.create(dto);
            return ResponseEntity.ok().body(responseDTO);
        } catch (Exception e) {
            log.info("이메일 중복!");
            return ResponseEntity.badRequest().body(e.getMessage());
        }

    }

    //로그인 요청 처리
    @PostMapping("/signin")
    public  ResponseEntity<?> signIn(
         @Validated @RequestBody LoginRequestDTO dto
    ) {
        try {
            LoginResponseDTO responseDTO
                    = userService.authenticate(dto);

            return ResponseEntity.ok().body(responseDTO);
        } catch (Exception e) {
            e.printStackTrace(); //에러 원인 확인
            return  ResponseEntity.badRequest().body(e.getMessage());
        }


    }


}