package com.github.sdms.service;

import com.github.sdms.model.User;
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
     * 根据 userId 加载用户信息
     * @param userId 用户ID
     * @return UserDetails
     * @throws UsernameNotFoundException 用户未找到异常
     */
    public UserDetails loadUserById(Long userId) throws UsernameNotFoundException {
        // 通过 userId 查找用户
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UsernameNotFoundException("User not found with userId: " + userId));
        return new CustomerUserDetails(user);
    }

    /**
     * 根据 username 和 libraryCode 加载用户信息
     * @param username 用户名
     * @param libraryCode 租户标识
     * @return UserDetails
     * @throws UsernameNotFoundException 用户未找到异常
     */
    public UserDetails loadUserByUsernameAndLibraryCode(String username, String libraryCode) throws UsernameNotFoundException {
        // 通过 email 和 libraryCode 查找用户
        User user = userRepository.findByUsernameAndLibraryCode(username, libraryCode)
                .orElseThrow(() -> new UsernameNotFoundException("User not found with username: " + username + " and libraryCode: " + libraryCode));

        return new CustomerUserDetails(user);
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        // 默认使用 username 查找用户（不包括 libraryCode）
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found with username: " + username));
        return new CustomerUserDetails(user);
    }
}
