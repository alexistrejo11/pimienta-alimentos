package io.github.alexistrejo11.pimienta.config.security;

import io.github.alexistrejo11.pimienta.module.account.auth.core.domain.entity.ParsedAccessToken;
import io.github.alexistrejo11.pimienta.module.account.auth.core.port.input.TokenService;
import io.github.alexistrejo11.pimienta.module.pos.core.port.output.DeviceTokenIssuer;
import io.github.alexistrejo11.pimienta.module.pos.core.port.output.DeviceTokenIssuer.ParsedDeviceAccessToken;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

  private final TokenService tokenService;
  private final DeviceTokenIssuer deviceTokenIssuer;

  public JwtAuthenticationFilter(TokenService tokenService, DeviceTokenIssuer deviceTokenIssuer) {
    this.tokenService = tokenService;
    this.deviceTokenIssuer = deviceTokenIssuer;
  }

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
      throws ServletException, IOException {
    String header = request.getHeader(HttpHeaders.AUTHORIZATION);
    if (header == null || !header.startsWith("Bearer ")) {
      filterChain.doFilter(request, response);
      return;
    }
    String token = header.substring(7).trim();
    if (token.isEmpty()) {
      filterChain.doFilter(request, response);
      return;
    }
    try {
      if (tryAuthenticateDevice(request, token) || tryAuthenticateStaff(request, token)) {
        // authenticated
      } else {
        SecurityContextHolder.clearContext();
      }
    } catch (RuntimeException ignored) {
      SecurityContextHolder.clearContext();
    }
    filterChain.doFilter(request, response);
  }

  private boolean tryAuthenticateDevice(HttpServletRequest request, String token) {
    try {
      ParsedDeviceAccessToken parsed = deviceTokenIssuer.parseAccessToken(token);
      List<SimpleGrantedAuthority> authorities =
          List.of(new SimpleGrantedAuthority(DeviceAuthenticationContext.AUTHORITY_SCOPE_POS_SYNC));
      DeviceAuthenticationContext principal = new DeviceAuthenticationContext(parsed);
      UsernamePasswordAuthenticationToken auth =
          new UsernamePasswordAuthenticationToken(principal, null, authorities);
      auth.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
      SecurityContextHolder.getContext().setAuthentication(auth);
      return true;
    } catch (RuntimeException e) {
      return false;
    }
  }

  private boolean tryAuthenticateStaff(HttpServletRequest request, String token) {
    try {
      ParsedAccessToken parsed = tokenService.parseAccessToken(token);
      var authorities = new ArrayList<SimpleGrantedAuthority>();
      for (String r : parsed.roles()) {
        authorities.add(new SimpleGrantedAuthority("ROLE_" + r));
      }
      for (String p : parsed.permissions()) {
        authorities.add(new SimpleGrantedAuthority(p));
      }
      JwtAuthenticationContext principal = new JwtAuthenticationContext(parsed);
      UsernamePasswordAuthenticationToken auth =
          new UsernamePasswordAuthenticationToken(principal, null, authorities);
      auth.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
      SecurityContextHolder.getContext().setAuthentication(auth);
      return true;
    } catch (RuntimeException e) {
      return false;
    }
  }
}
