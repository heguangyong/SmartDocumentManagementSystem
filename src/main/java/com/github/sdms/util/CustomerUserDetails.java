package com.github.sdms.util;

import com.github.sdms.model.User;
import com.github.sdms.model.enums.RoleType;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.Collections;

@Getter
@AllArgsConstructor
public class CustomerUserDetails implements UserDetails {

    private final User user;

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        // ✅ 角色需要加上 "ROLE_" 前缀，Spring Security 默认约定
        return Collections.singletonList(new SimpleGrantedAuthority("ROLE_" + user.getRoleType()));
    }

    @Override
    public String getPassword() {
        return user.getPassword(); // 加密后的密码
    }

    @Override
    public String getUsername() {
        return user.getEmail(); // 用邮箱作为用户名
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return true;
    }

    public String getUid() {
        return user.getUid();
    }

    public Long getUserId(){ return user.getId(); }

    // 新增：获取租户信息 libraryCode
    public String getLibraryCode() {
        return user.getLibraryCode();  // 假设 user 对象中包含 libraryCode 字段
    }

    public RoleType getRoleType() {
        return user.getRoleType();
    }


    public boolean hasRole(String role) {
        if (role == null || role.isEmpty()) {
            return false;
        }
        String roleWithPrefix = role.startsWith("ROLE_") ? role : "ROLE_" + role;
        return getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(roleWithPrefix::equals);
    }
}
