package net.dorokhov.pony.user.service.impl;

import net.dorokhov.pony.user.domain.User;
import net.dorokhov.pony.user.repository.UserRepository;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.Set;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class UserDetailsServiceImplTest {
    
    @InjectMocks
    private UserDetailsServiceImpl userDetailsService;
    
    @Mock
    private UserRepository userRepository;

    @Test
    public void shouldLoadUser() throws Exception {
        
        User user = User.builder()
                .name("someName")
                .email("someEmail")
                .password("somePassword")
                .addRoles(User.Role.USER, User.Role.ADMIN)
                .build();
        when(userRepository.findByEmail(any())).thenReturn(user);
        
        UserDetails userDetails = userDetailsService.loadUserByUsername("someEmail");
        
        assertThat(userDetails).isInstanceOf(UserDetailsImpl.class);
        assertThat(userDetails.getUsername()).isEqualTo("someEmail");
        assertThat(userDetails.getPassword()).isEqualTo("somePassword");
        Set<String> authorities = userDetails.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toSet());
        assertThat(authorities).containsOnly("USER", "ADMIN");
        assertThat(userDetails.isAccountNonExpired()).isTrue();
        assertThat(userDetails.isAccountNonLocked()).isTrue();
        assertThat(userDetails.isCredentialsNonExpired()).isTrue();
        assertThat(userDetails.isEnabled()).isTrue();

        UserDetailsImpl userDetailsImpl = (UserDetailsImpl) userDetails;
        assertThat(userDetailsImpl.getUser()).isSameAs(user);
    }

    @Test
    public void shouldSupportUserNotFound() throws Exception {
        when(userRepository.findByEmail(any())).thenReturn(null);
        assertThatThrownBy(() -> userDetailsService.loadUserByUsername("someEmail")).isInstanceOf(UsernameNotFoundException.class);
    }
}
