package com.github.sdms.repository;

import com.github.sdms.model.AppUser;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/**
 * 用户仓库接口，继承自JpaRepository，提供基于JPA的数据访问操作。
 * 主要用于根据多种用户属性查询用户信息和判断用户是否存在。
 */
public interface UserRepository extends JpaRepository<AppUser, Long> {

    /**
     * 根据邮箱查询用户
     * @param email 用户邮箱，唯一
     * @return 包含用户的Optional对象，若无对应用户则为空
     */
    Optional<AppUser> findByEmail(String email);

    /**
     * 根据用户名查询用户
     * @param username 用户名，唯一
     * @return 包含用户的Optional对象，若无对应用户则为空
     */
    Optional<AppUser> findByUsername(String username);

    /**
     * 判断邮箱是否已存在
     * @param email 用户邮箱
     * @return 若邮箱存在则返回true，否则false
     */
    boolean existsByEmail(String email);

    /**
     * 根据统一用户UID查询用户
     * @param uid 用户的统一唯一标识
     * @return 包含用户的Optional对象，若无对应用户则为空
     */
    Optional<AppUser> findByUid(String uid);

    /**
     * 判断统一用户UID是否存在
     * @param uid 统一用户唯一标识
     * @return 若UID存在则返回true，否则false
     */
    boolean existsByUid(String uid);

    /**
     * 根据手机号查询用户
     * @param mobile 手机号，唯一
     * @return 包含用户的Optional对象，若无对应用户则为空
     */
    Optional<AppUser> findByMobile(String mobile);

    /**
     * 判断手机号是否已存在
     * @param mobile 手机号
     * @return 若手机号存在则返回true，否则false
     */
    boolean existsByMobile(String mobile);

    /**
     * 根据用户名或邮箱查询用户，满足任一条件即可
     * @param username 用户名
     * @param email 邮箱
     * @return 包含用户的Optional对象，若无对应用户则为空
     */
    Optional<AppUser> findByUsernameOrEmail(String username, String email);
}
