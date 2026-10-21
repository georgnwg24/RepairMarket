package at.ac.tuwien.sepr.groupphase.backend.security;

import at.ac.tuwien.sepr.groupphase.backend.config.properties.SecurityProperties;
import at.ac.tuwien.sepr.groupphase.backend.exception.UserBannedException;
import at.ac.tuwien.sepr.groupphase.backend.repository.UserRepository;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.util.List;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.filter.OncePerRequestFilter;

@Service
@Order(Ordered.LOWEST_PRECEDENCE - 1)
public class JwtAuthorizationFilter extends OncePerRequestFilter {

    private static final Logger LOGGER = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
    private final SecurityProperties securityProperties;

    private final UserRepository userRepository;

    public JwtAuthorizationFilter(SecurityProperties securityProperties, UserRepository userRepository) {
        this.securityProperties = securityProperties;
        this.userRepository = userRepository;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
        throws IOException, ServletException {
        try {
            UsernamePasswordAuthenticationToken authToken = getAuthToken(request);
            if (authToken != null) {
                SecurityContextHolder.getContext().setAuthentication(authToken);
            }
        } catch (UserBannedException e) {
            LOGGER.debug("Invalid authorization attempt: {}", e.getMessage());
            response.setStatus(423); // "Locked", we already use forbidden and unauthorized
            response.getWriter().write("Account is banned");
            return;
        } catch (IllegalArgumentException | JwtException e) {
            LOGGER.debug("Invalid authorization attempt: {}", e.getMessage());
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.getWriter().write("Invalid authorization header or token");
            return;
        }
        chain.doFilter(request, response);
    }

    private UsernamePasswordAuthenticationToken getAuthToken(HttpServletRequest request)
        throws JwtException, IllegalArgumentException {
        String token = request.getHeader(securityProperties.getAuthHeader());
        if (token == null || token.isEmpty()) {
            return null;
        }

        if (!token.startsWith(securityProperties.getAuthTokenPrefix())) {
            throw new IllegalArgumentException("Authorization header is malformed or missing");
        }

        return (UsernamePasswordAuthenticationToken) extractAuthentication(token);
    }

    public Authentication extractAuthentication(String bearer) {
        if (bearer == null || bearer.isBlank()) {
            throw new IllegalArgumentException("Missing token");
        }
        String token = bearer.startsWith(securityProperties.getAuthTokenPrefix())
            ? bearer.substring(securityProperties.getAuthTokenPrefix().length()).trim() : bearer;
        Claims claims = Jwts.parser()
            .verifyWith(Keys.hmacShaKeyFor(securityProperties.getJwtSecret().getBytes()))
            .build().parseSignedClaims(token).getPayload();
        String username = claims.getSubject();
        if (username == null || username.isBlank()) {
            throw new IllegalArgumentException("Token contains no user");
        }
        var user = userRepository.findUserByUsername(username)
            .orElseThrow(() -> new IllegalArgumentException("Account no longer exists"));
        if (user.isBanned() || "deleted user".equals(username)) {
            throw new UserBannedException("User is banned");
        }
        List<SimpleGrantedAuthority> authorities = ((List<?>) claims.get("rol")).stream()
            .map(role -> new SimpleGrantedAuthority((String) role)).toList();
        MDC.put("u", username);
        return new UsernamePasswordAuthenticationToken(username, null, authorities);
    }
}
