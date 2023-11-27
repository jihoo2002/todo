package com.example.todo.auth;

import com.example.todo.userapi.entity.User;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@Component //빈등록해야 서비스에서 부를 수(주입받을 수) 있다.
@Slf4j
//역할 : 토큰을 발급하고, 서명 위조를 검사하는 객체
public class TokenProvider {

    //서명에 사용할 값 (512비트 이상의 랜덤 문자열)
    //@Value :properties 형태의 파일의 내용을 읽어서 변수에 대입하는 아노테이션. (yml도 가능)
    @Value("${jwt.secret}") //스프링 프레임 워크 value  ->application.yml속에 설정해둠
     private String SECRET_KEY;


    //토큰 생성 메서드

    /**
     * JSON Web Token을 생성하는 메서드
     * @param userEntity - 토큰의 내용(클레임)에 포함될 유저 정보
     * @return - 생성된 JSON을 암호화 한 토큰값
     */
    public String createToken(User userEntity) {
        //토큰 만료시간 생성
        Date expiry = Date.from(
                Instant.now().plus(1, ChronoUnit.DAYS)  //오늘기준으로 하루 더함
        );


        //토큰 생성
        /*
            {
                "iss" :서비스 이름(발급자),
                "exp" :"2023-12-27(만료일자)"
                "iat" : "2023-11-27(발급일자)"
                "email" :"로그인한 사람 이메일"
                "role" : "Premium"
                ...
                ==서명
            }
         */

        //추가 클레임 정의
        Map<String, String> claims = new HashMap<>();
        claims.put("email", userEntity.getEmail());
      //   claims.put("role", userEntity.getRole());

        return Jwts.builder()
                //Token header에 들어갈 서명
                .signWith(
                        Keys.hmacShaKeyFor(SECRET_KEY.getBytes()),
                        SignatureAlgorithm.ES512
                )
                //token payload에 들어갈 클레임 설정.
                .setIssuer("Todo운영자")//iss: 발급자 정보
                .setIssuedAt(new Date()) //lat : 발급 시간
                .setExpiration(expiry) //exp : 만료 시간
                // --------------------------------- 위의 값은 필수 아래는 가공할 값
                .setSubject(userEntity.getId()) //sub : 토큰을 식별할 수 있는 주요 데이터
                .setClaims(claims)
                .compact(); //JWT에 들어갈 값을 세팅
    }
}
