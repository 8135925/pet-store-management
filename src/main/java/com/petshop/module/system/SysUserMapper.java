package com.petshop.module.system;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface SysUserMapper {

    SysUser findByUsername(@Param("username") String username);

    SysUser findById(@Param("id") Long id);

    List<SysUser> findAll();

    long count();

    int insert(SysUser u);

    int update(SysUser u);

    int updatePassword(@Param("id") Long id, @Param("passwordHash") String passwordHash);

    int setActive(@Param("id") Long id, @Param("active") boolean active);

    int countAdminActive(@Param("excludeId") Long excludeId);

    int updateLoginTime(@Param("id") Long id, @Param("time") LocalDateTime time);

    int deleteById(@Param("id") Long id);
}
