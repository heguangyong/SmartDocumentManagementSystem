package com.github.sdms.service;

import com.github.sdms.model.AppUser;
import com.github.sdms.repository.UserRepository;
import com.github.sdms.util.CustomerUserDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CustomUserDetailsServices implements UserDetailsService {

    private final UserRepository userRepository;

    /**
     * 根据 email 和 libraryCode 加载用户信息
     * @param email 用户邮箱
     * @param libraryCode 租户标识
     * @return UserDetails
     * @throws UsernameNotFoundException 用户未找到异常
     */
    public UserDetails loadUserByUsernameAndLibraryCode(String email, String libraryCode) throws UsernameNotFoundException {
        // 通过 email 和 libraryCode 查找用户
        AppUser user = userRepository.findByEmailAndLibraryCode(email, libraryCode)
                .orElseThrow(() -> new UsernameNotFoundException("User not found with email: " + email + " and libraryCode: " + libraryCode));

        return new CustomerUserDetails(user);
    }

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        // 默认使用 email 查找用户（不包括 libraryCode）
        AppUser user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("User not found with email: " + email));
        return new CustomerUserDetails(user);
    }
}
