package com.example.todo.userapi.service;

import com.example.todo.userapi.dto.UserRequestSignUpDTO;
import com.example.todo.userapi.dto.UserSignUpResponseDTO;
import com.example.todo.userapi.entity.User;
import com.example.todo.userapi.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;

    private  final PasswordEncoder passwordEncoder; //빈등록 해야 사용 가능 -> WebSecurityConfig 클래스에서 빈 등록함

    //회원가입 처리
    public UserSignUpResponseDTO create(final UserRequestSignUpDTO dto) {
        String email = dto.getEmail();

        if(userRepository.existsByEmail(email)) {
            //이메일 중복이 발생했니? 
            log.warn("이메일이 중복되었습니다. -{}", email);
            throw new RuntimeException("중복된 이메일입니다.");
        }
        //존재하지 않는 다면 이쪽으로 !
        //패스워드 인코딩
        String encoded = passwordEncoder.encode(dto.getPassword());
        dto.setPassword(encoded);

        //dto를 User entity로 변환해서 저장
        User saved = userRepository.save(dto.toEntity());

        log.info("회원가입 정상 수행됨! - saved user -{}", saved);

        return new UserSignUpResponseDTO(saved);

    }



}
