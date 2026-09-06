package com.techpulse.taskmanager.security;

import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

	private final JwtService jwtService;

	private static final String HEADER = "Authorization";
	private static final String PREFIX = "Bearer ";

	@Override
	protected void doFilterInternal(@NonNull HttpServletRequest request, @NonNull HttpServletResponse response,
			@NonNull FilterChain filterChain) throws ServletException, IOException {
		try {
			String header = request.getHeader(HEADER);

			if (header != null && header.startsWith(PREFIX)) {
				String token = header.substring(PREFIX.length());

				if (jwtService.isTokenValid(token)) {
					Long userId = jwtService.extractUserId(token);
					Long tenantId = jwtService.extractTenantId(token);
					String role = jwtService.extractRole(token);

					TenantContext.setTenantId(tenantId);
					TenantContext.setUserId(userId);
					TenantContext.setRole(role);

					var authorities = List.of(new SimpleGrantedAuthority("ROLE_" + role));
					var authentication = new UsernamePasswordAuthenticationToken(userId, null, authorities);
					SecurityContextHolder.getContext().setAuthentication(authentication);
				} else {
					sendUnauthorized(response);
					return;
				}
			}

			filterChain.doFilter(request, response);
		} catch (JwtException | IllegalArgumentException ex) {
			SecurityContextHolder.clearContext();
			sendUnauthorized(response);
		} finally {
			TenantContext.clear();
		}
	}

	private void sendUnauthorized(HttpServletResponse response) throws IOException {
		response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
		response.setContentType("application/json");
		response.getWriter()
				.write("{\"status\":401,\"error\":\"Unauthorized\",\"message\":\"Invalid or expired token\"}");
	}
}