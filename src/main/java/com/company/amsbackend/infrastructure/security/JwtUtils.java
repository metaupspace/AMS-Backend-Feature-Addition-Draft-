package com.company.amsbackend.infrastructure.security;

import io.jsonwebtoken.*;
import org.springframework.stereotype.Component;
import java.util.Date;

@Component
public class JwtUtils {
    private final String jwtSecret = "2389664fd6324e7c54e1740ff21d30b6b366dc30540f7e58b62b665aa92f51bad30fdfb7a2b28ff169eca8f98214bb32bb2189d76e9c325b99e1e26596889818";  // ADD JWT SECRET KEY HERE (512 bits)
    private final long jwtExpirationMs = 86400000; // 24 hours

    public String generateJwtToken(String username, String role) {
        return Jwts.builder()
                .setSubject(username)
                .claim("role", role)
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + jwtExpirationMs))
                .signWith(SignatureAlgorithm.HS512, jwtSecret)
                .compact();
    }

    public String getUsernameFromJwtToken(String token) {
        return Jwts.parser().setSigningKey(jwtSecret).parseClaimsJws(token).getBody().getSubject();
    }

    public boolean validateJwtToken(String authToken) {
        try {
            Jwts.parser().setSigningKey(jwtSecret).parseClaimsJws(authToken);
            return true;
        } catch (JwtException e) {
            // log error
        }
        return false;
    }
}