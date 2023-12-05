package com.example.todo.userapi.api;

import com.example.todo.auth.TokenUserInfo;
import com.example.todo.exception.NoRegisteredArgumentException;
import com.example.todo.userapi.dto.request.LoginRequestDTO;
import com.example.todo.userapi.dto.request.UserRequestSignUpDTO;
import com.example.todo.userapi.dto.response.LoginResponseDTO;
import com.example.todo.userapi.dto.response.UserSignUpResponseDTO;
import com.example.todo.userapi.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.util.FileCopyUtils;
import org.springframework.validation.BindingResult;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;

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
            @Validated @RequestPart("user") UserRequestSignUpDTO dto,
            @RequestPart(value = "profileImage" , required = false) MultipartFile profileImg,
            BindingResult result //required 가 false -> 파일이 안넘어 올 수 있어도 오류 안남
    ) {
        log.info("/api/auth POST! - {}", dto);

        if(result.hasErrors()) {
            log.warn(result.toString());
            return ResponseEntity.badRequest()
                    .body(result.getFieldError());
        }

        try {

            String uploadedFilePath = null; //기본값 null
            if(profileImg != null ) {
                log.info("attached file name : {}", profileImg.getOriginalFilename());
                //전달받은 프로필 이미지를 먼저 지정된 경로에 저장한 후 경로를 받아오자 -> 경로 DB에 저장하기 위해
               uploadedFilePath = userService.uploadProfileImage(profileImg); //이쪽으로 null아님 파일경로가 옴
            }

            UserSignUpResponseDTO responseDTO = userService.create(dto, uploadedFilePath);
            return ResponseEntity.ok().body(responseDTO);

        } catch (RuntimeException e) { //이메일 중복 뿐 아니라 파일 처리에서도 에러 발생 가능성 있음
            log.info("이메일 중복!");
            return ResponseEntity.badRequest().body(e.getMessage());
        }catch (Exception e) {
            log.warn("기타 예외가 발생했습니다.");
            e.printStackTrace();
            return ResponseEntity.internalServerError().build();
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
            return ResponseEntity.badRequest()
                    .body(e.getMessage());
        }


    }


    //일반 회원을 프리미엄 회원으로 승격하는 요청 처리
    @PutMapping("/promote")
    //권한 검사 (해당 권한이 아니라면 인가처리 거부 -> 403 코드 리턴)
    //메서드 호출 전 권한 검사 -> 요청 당시 토큰에 있는 user정보가 ROLE_COMMON이라는 권한을 가지고 있는 지 검사 . 아니라면 403
    @PreAuthorize("hasRole('ROLE_COMMON')")//이 메서드는 ROLE_COMMON인 사람만 실행할 수 있음
    public ResponseEntity<?> promote(
            @AuthenticationPrincipal TokenUserInfo userInfo
            ) {
        log.info("/api/auth/promote PUT!");

        try {
           LoginResponseDTO responseDTO =  userService.promoteToPremium(userInfo); //이사람이 커먼인지 프리미엄인지 확인, 누가 승격하려 하는지 등등
            return ResponseEntity.ok().body(responseDTO);
        }catch (NoRegisteredArgumentException |IllegalArgumentException e) {
          //예상 가능한 예외(직접 생성 하는 예외 처리)
            e.printStackTrace();
            log.warn(e.getMessage());
            return ResponseEntity.badRequest()
                    .body(e.getMessage());
        
        }catch (Exception e){
            //예상 하지 못한 예외 처리
            e.printStackTrace();
            return ResponseEntity.internalServerError()
                    .body(e.getMessage());
        }
    }

    //프로필 사진 이미지 데이터를 클라이언트에게 응답 처리
    @GetMapping("/load-profile")
    public ResponseEntity<?> loadFile(
            @AuthenticationPrincipal TokenUserInfo userInfo
    ) {
        log.info("/api/auth/load-profile - GET, user {}", userInfo.getEmail());
        System.out.println("컨트롤러 작동 됨 ");
        try {
            //클라이언트가 요청한 프로필 사진을 응답해야 한다
            //1. 프로필 사진의 경로부터 얻어야 한다.
            String filePath =  userService.findProfilePath(userInfo.getUserId());
            //프사를 지정하지 않은 사람이 오면 NULL이온다.

            //2.얻어낸 파일 경로를 통해 실제 파일 데이터를 로드하기
            File profileFile = new File(filePath);

            //모든 사용자가 프로필 사진을 가지는 것은 아니다 -> 프사 없는 사람들은 경로가 존재하지 않을 것이다.
            //만약 존재하지 않는 경로라면 클라이언트로  404 status를 리턴
            if(!profileFile.exists()) {
                if(filePath.startsWith("http")) {
                    return ResponseEntity.status(210).body(filePath);
                }
                return ResponseEntity.notFound().build();
            }


            //해당 경로에 저장된 파일을 바이트 배열로 직렬화 해서 리턴
            byte[] fileData = FileCopyUtils.copyToByteArray(profileFile);

            //3.응답 헤더에 컨텐츠 타입을 설정.
            HttpHeaders headers = new HttpHeaders();
            MediaType contentType = findExtensionAndGetMediaType(filePath);
            if(contentType == null) {
                return ResponseEntity.internalServerError()
                        .body("발견된 파일은 이미지 파일이 아닙니다.");
            }
            headers.setContentType(contentType);
            return ResponseEntity.ok().headers(headers).body(fileData);

        } catch (IOException e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError()
                    .body("파일을 찾을 수 없습니다.");
        }

    }

    private MediaType findExtensionAndGetMediaType(String filePath) {

        //파일 경로에서 확장자를 추출
        //C:/todo_upload/sdjklfjksf_abc.jpg
        //.다음부터 끝까지
        String ext = filePath.substring(filePath.lastIndexOf(".") + 1);


        //추출한 확장자를 바탕으로 MediaType을 설정. ->  Header에 들어갈 Content-type이 됨
        switch (ext.toUpperCase()) {
            case "JPG" : case "JPEG":
                return MediaType.IMAGE_JPEG;
            case "PNG":
                return  MediaType.IMAGE_PNG;
            case  "GIF" :
                return MediaType.IMAGE_GIF;
            default:
        return null; //이 확장자들이 아니라면 이미지 파일이 아닌것임 즉 null 리턴
        }

    }

    @GetMapping("/kakaoLogin")
    public ResponseEntity<?> kakaoLogin(String code) {
        log.info("/api/auth/kakaoLogin - GET -code: {}", code);
        LoginResponseDTO responseDTO = userService.kakaoService(code);
        return ResponseEntity.ok(responseDTO);
    }

    //로그아웃 처리
    @GetMapping("/logout")
    public ResponseEntity<?> logout(@AuthenticationPrincipal TokenUserInfo userInfo) {
    log.info("api/auth/logout - GET -user : {}", userInfo);
        String result = userService.logout(userInfo);
        //카카오 로그인 한 사람은 result에 아이디가 들어 있음, 아닌 사람은 null

        return ResponseEntity.ok().body(result);
    }


}