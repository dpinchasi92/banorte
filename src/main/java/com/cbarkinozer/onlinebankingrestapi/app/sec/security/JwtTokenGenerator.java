package com.cbarkinozer.onlinebankingrestapi.app.sec.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

@Component
public class JwtTokenGenerator {

    private final SecretKey signingKey;

    private final Long EXPIRE_TIME;

    /** HS512 requires a key of at least 512 bits (64 bytes); jjwt rejects shorter keys. */
    public JwtTokenGenerator(@Value("${onlinebankingrestapi.jwt.security.app.key}") String appKey,
                             @Value("${onlinebankingrestapi.jwt.security.expire.time}") Long expireTime) {
        this.signingKey = Keys.hmacShaKeyFor(appKey.getBytes(StandardCharsets.UTF_8));
        this.EXPIRE_TIME = expireTime;
    }

    public String generateJwtToken(Authentication authentication){

        JwtUserDetails jwtUserDetails = (JwtUserDetails) authentication.getPrincipal();
        Date expireDate = new Date(new Date().getTime() + EXPIRE_TIME);

        String token = Jwts.builder()
                .subject(Long.toString(jwtUserDetails.getId()))
                .issuedAt(new Date())
                .expiration(expireDate)
                .signWith(signingKey, Jwts.SIG.HS512)
                .compact();

        return token;
    }

    public Long findUserIdByToken(String token){

        Jws<Claims> claimsJws = parseToken(token);

        String userIdStr = claimsJws
                .getPayload()
                .getSubject();

        return Long.parseLong(userIdStr);
    }

    private Jws<Claims> parseToken(String token) {
        Jws<Claims> claimsJws = Jwts.parser()
                .verifyWith(signingKey)
                .build()
                .parseSignedClaims(token);
        return claimsJws;
    }

    public boolean validateToken(String token){

        boolean isValid;

        try {
            Jws<Claims> claimsJws = parseToken(token);

            isValid = !isTokenExpired(claimsJws);
        } catch (Exception e){
            isValid = false;
        }

        return isValid;
    }

    private boolean isTokenExpired(Jws<Claims> claimsJws) {

        Date expirationDate = claimsJws.getPayload().getExpiration();

        return expirationDate.before(new Date());
    }
}
