package net.dorokhov.pony2.web.service;

import com.google.common.collect.Sets;
import net.dorokhov.pony2.api.user.domain.User;
import net.dorokhov.pony2.api.user.service.UserService;
import net.dorokhov.pony2.web.security.BruteForceProtector;
import net.dorokhov.pony2.web.security.UserDetailsImpl;
import net.dorokhov.pony2.web.security.UserDetailsServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class UserDetailsServiceImplTest {

    @InjectMocks
    private UserDetailsServiceImpl userDetailsService;

    @Mock
    private UserService userService;
    @Mock
    private BruteForceProtector bruteForceProtector;

    @BeforeEach
    void setUp() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));
    }

    @Test
    public void shouldLoadUser() {

        User user = new User()
                .setName("someName")
                .setEmail("someEmail")
                .setPassword("somePassword")
                .setRoles(Sets.newHashSet(User.Role.USER, User.Role.ADMIN));
        when(userService.getByEmail(any())).thenReturn(Optional.of(user));

        UserDetails userDetails = userDetailsService.loadUserByUsername("someEmail");

        assertThat(userDetails).isInstanceOf(UserDetailsImpl.class);
        assertThat(userDetails.getUsername()).isEqualTo("someEmail");
        assertThat(userDetails.getPassword()).isEqualTo("somePassword");
        Set<String> authorities = userDetails.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toSet());
        assertThat(authorities).containsOnly("ROLE_USER", "ROLE_ADMIN");
        assertThat(userDetails.isAccountNonExpired()).isTrue();
        assertThat(userDetails.isAccountNonLocked()).isTrue();
        assertThat(userDetails.isCredentialsNonExpired()).isTrue();
        assertThat(userDetails.isEnabled()).isTrue();

        UserDetailsImpl userDetailsImpl = (UserDetailsImpl) userDetails;
        assertThat(userDetailsImpl.getUser()).isSameAs(user);
    }

    @Test
    public void shouldSupportUserNotFound() {

        when(userService.getByEmail(any())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userDetailsService.loadUserByUsername("someEmail")).isInstanceOf(UsernameNotFoundException.class);
    }
}
