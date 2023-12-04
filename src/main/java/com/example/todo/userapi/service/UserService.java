package com.example.todo.userapi.service;

import com.example.todo.auth.TokenProvider;
import com.example.todo.auth.TokenUserInfo;
import com.example.todo.exception.NoRegisteredArgumentException;
import com.example.todo.userapi.dto.request.LoginRequestDTO;
import com.example.todo.userapi.dto.request.UserRequestSignUpDTO;
import com.example.todo.userapi.dto.response.KakaoUserDTO;
import com.example.todo.userapi.dto.response.LoginResponseDTO;
import com.example.todo.userapi.dto.response.UserSignUpResponseDTO;
import com.example.todo.userapi.entity.Role;
import com.example.todo.userapi.entity.User;
import com.example.todo.userapi.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
public class UserService {



    private final UserRepository userRepository;

    private  final PasswordEncoder passwordEncoder; //빈등록 해야 사용 가능 -> WebSecurityConfig 클래스에서 빈 등록함

    private final TokenProvider tokenProvider; //주입 받아 토큰을 이용해 유효성 검사를 해줄 것

    @Value("${kakao.client_id}")
    private  String KAKAO_CLIENT_ID;

    @Value("${kakao.redirect_url}")
    private  String KAKAO_REDIRECT_URL;

    @Value("${kakao.client_secret}")
    private String KAKAO_CLIENT_SECRET;

    @Value("${upload.path}") //yml 파일 속에 있음
    private String uploadRootPath;
    //회원가입 처리
    public UserSignUpResponseDTO create(final UserRequestSignUpDTO dto, final String uploadedFilePath) {
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
        User saved = userRepository.save(dto.toEntity(uploadedFilePath));

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

    /**
     * 업로드된 파일을 서버에 저장하고 저장 경로를 리턴.
     *
     * @param profileImg - 업로드된 파일 정보
     * @return 실제로 저장된 이미지 경로
     */
    public String uploadProfileImage(MultipartFile profileImg) throws IOException {

        //루트 디렉토리가 실존하는지 확인 후 존재하지 않으면 생성.
        File rootDir = new File(uploadRootPath);
        if(!rootDir.exists()) {
            rootDir.mkdir();
        }

        //파일명을 유니크하게 변경(이름 충돌 가능성을 대비)
        //uuid와 원본 파일명을 혼합
        String uniqueFileName = UUID.randomUUID() + "_" + profileImg.getOriginalFilename();
        
        //퍄일을 저장
        File uploadFile = new File(uploadRootPath + "/" + uniqueFileName);
        profileImg.transferTo(uploadFile);

        return uniqueFileName;
    }

    public String findProfilePath(String userId) {
        User user = userRepository.findById(userId)
                .orElseThrow();
        //DB에 저장되는 profile.img는 파일명 => service가 가지고 있는 ROOT PATH와 연결해서 리턴
        return uploadRootPath  + "/" + user.getProfileImg();
    }

    public void kakaoService(final String code) {

        // 인가코드를 통해 토큰 발급받기
        String accessToken = getKakaoAccessToken(code);
        log.info("token: {}", accessToken);

        // 토큰을 통해 사용자 정보 가져오기
        KakaoUserDTO dto = getKakaoUserInfo(accessToken);

        // 일회성 로그인으로 처리 -> dto를 바로 화면단으로 리턴
        // 회원가입 처리 -> 이메일 중복 검사 진행 -> 자체 jwt를 생성해서 토큰을 화면단에 리턴.
        // -> 화면단에서는 적절한 url을 선택하여 redirect를 진행.



    }

    private KakaoUserDTO getKakaoUserInfo(String accessToken) {

        // 요청 uri
        String requestUri = "https://kapi.kakao.com/v2/user/me";

        // 요청 헤더 설정
        HttpHeaders headers = new HttpHeaders();
        headers.add("Authorization", "Bearer " + accessToken);
        headers.add("Content-type", "application/x-www-form-urlencoded;charset=utf-8");

        // 요청 보내기
        RestTemplate template = new RestTemplate();
        ResponseEntity<KakaoUserDTO> responseEntity
                = template.exchange(requestUri, HttpMethod.GET, new HttpEntity<>(headers), KakaoUserDTO.class);

        // 응답 바디 읽기
        KakaoUserDTO responseData = responseEntity.getBody();
        log.info("user profile: {}", responseData);

        return responseData;
    }

    private String getKakaoAccessToken(String code) {

        // 요청 uri
        String requestUri = "https://kauth.kakao.com/oauth/token";

        // 요청 헤더 설정
        HttpHeaders headers = new HttpHeaders();
        headers.add("Content-type", "application/x-www-form-urlencoded;charset=utf-8");

        // 요청 바디(파라미터) 설정
        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("grant_type", "authorization_code"); // 카카오 공식 문서 기준 값으로 세팅
        params.add("client_id", KAKAO_CLIENT_ID); // 카카오 디벨로퍼 REST API 키
        params.add("redirect_uri", KAKAO_REDIRECT_URL); // 카카오 디벨로퍼 등록된 redirect uri
        params.add("code", code); // 프론트에서 인가 코드 요청시 전달받은 코드값
        params.add("client_secret", KAKAO_CLIENT_SECRET); // 카카오 디벨로퍼 client secret(활성화 시 추가해 줘야 함)

        // 헤더와 바디 정보를 합치기 위해 HttpEntity 객체 생성
        HttpEntity<Object> requestEntity = new HttpEntity<>(params, headers);

        // 카카오 서버로 POST 통신
        RestTemplate template = new RestTemplate();

        // 통신을 보내면서 응답데이터를 리턴
        // param1: 요청 url
        // param2: 요청 메서드 (전송 방식)
        // param3: 헤더와 요청 파라미터정보 엔터티
        // param4: 응답 데이터를 받을 객체의 타입 (ex: dto, map)
        // 만약 구조가 복잡한 경우에는 응답 데이터 타입을 String으로 받아서 JSON-simple 라이브러리로 직접 해체.
        ResponseEntity<Map> responseEntity
                = template.exchange(requestUri, HttpMethod.POST, requestEntity, Map.class);

        // 응답 데이터에서 필요한 정보를 가져오기
        Map<String, Object> responseData = (Map<String, Object>)responseEntity.getBody();
        log.info("토큰 요청 응답 데이터: {}", responseData);

        // 여러가지 데이터 중 access_token이라는 이름의 데이터를 리턴 (Object를 String으로 형 변환해서 리턴)
        return (String) responseData.get("access_token");
    }


}
