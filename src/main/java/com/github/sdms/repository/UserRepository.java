package com.github.sdms.repository;

import com.github.sdms.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long>, JpaSpecificationExecutor<User> {
    // 根据用户名查询用户
    Optional<User> findByUsername(String username);

    // 根据用户名查询用户
    Optional<User> findByUsernameAndLibraryCode(@Param("username") String username, @Param("libraryCode") String libraryCode);

    // 判断用户名是否已存在
    boolean existsByUsernameAndLibraryCode(@Param("username") String username, @Param("libraryCode") String libraryCode);

    // 根据统一用户UID查询用户
    Optional<User> findByUidAndLibraryCode(@Param("uid") String uid, @Param("libraryCode") String libraryCode);

    // 判断统一用户UID是否存在
    boolean existsByUidAndLibraryCode(@Param("uid") String uid, @Param("libraryCode") String libraryCode);

    // 根据手机号查询用户
    Optional<User> findByMobileAndLibraryCode(@Param("mobile") String mobile, @Param("libraryCode") String libraryCode);

    // 判断手机号是否已存在
    boolean existsByMobileAndLibraryCode(@Param("mobile") String mobile, @Param("libraryCode") String libraryCode);

    // 根据用户名或邮箱查询用户，满足任一条件即可
    Optional<User> findByUsernameOrEmailAndLibraryCode(@Param("username") String username, @Param("email") String email, @Param("libraryCode") String libraryCode);

}
