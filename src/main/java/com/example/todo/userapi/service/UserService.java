package com.example.todo.userapi.service;

import com.example.todo.auth.TokenProvider;
import com.example.todo.auth.TokenUserInfo;
import com.example.todo.exception.NoRegisteredArgumentException;
import com.example.todo.userapi.dto.request.LoginRequestDTO;
import com.example.todo.userapi.dto.request.UserRequestSignUpDTO;
import com.example.todo.userapi.dto.response.LoginResponseDTO;
import com.example.todo.userapi.dto.response.UserSignUpResponseDTO;
import com.example.todo.userapi.entity.Role;
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

    private final TokenProvider tokenProvider; //주입 받아 토큰을 이용해 유효성 검사를 해줄 것
    //회원가입 처리
    public UserSignUpResponseDTO create(final UserRequestSignUpDTO dto) {
        String email = dto.getEmail();

        if(isDuplicate(email)) {
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


    public boolean isDuplicate(String email) {
       return userRepository.existsByEmail(email); //true아님 false가 옴
    }




    //회원 인증
    public LoginResponseDTO authenticate(final LoginRequestDTO dto) {
        //이메일을 통해 회원 정보 조회
        User user = userRepository.findByEmail(dto.getEmail())
                .orElseThrow(
                        () -> new RuntimeException("가입된 회원이 아닙니다.")
                );

        //패스워드 검증
        String rawPassword = dto.getPassword(); //입력한 비번
        String encodedPassword = user.getPassword(); //DB에 저장된 암호화된 비번
        if(!passwordEncoder.matches(rawPassword, encodedPassword)) {
            //매칭이 안됐냐 ->비번 틀림
            throw new RuntimeException("비밀번호가 틀렸습니다.");

        }
            log.info("{}님 로그인 성공", user.getUserName());

            //로그인 성공 후에 클라이언트에게 뭘 리턴할 것인가??
            //->JWT를 클라이언트에게 발급해주어야 한다!

        String token = tokenProvider.createToken(user);

    return new LoginResponseDTO(user, token);

    }

    //프리미엄으로 등급 업
    public LoginResponseDTO promoteToPremium(TokenUserInfo userInfo) {
        User foundUser = userRepository.findById(userInfo.getUserId()).orElseThrow(
                () -> new NoRegisteredArgumentException("회원 조회에 실패했습니다. ")
            //원래 옵셔널이지만 오류 따로 처리 했기에 user로 받음
        );


        //일반(COMMON)회원이 아니라면 예외발생
        if(userInfo.getRole() != Role.COMMON) {
            throw new IllegalArgumentException("일반 회원이 아니라면 등급을 상승시킬 수 없습니다.");
        }
        
        //등급변경
        foundUser.changeRole(Role.PREMIUM); //메서드를 이용해 롤 수정
        User saved = userRepository.save(foundUser);

        //토큰을 재발급 , 기존 토큰은 옛날 정보이기 때문에 새 토큰으로 생성해줘야 한다.
        String token = tokenProvider.createToken(saved);
        return new LoginResponseDTO(saved, token);

    }
}
