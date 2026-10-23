package com.petshop.module.customer;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface PetMapper {

    Pet findById(@Param("id") Long id);

    List<Pet> findByOwner(@Param("ownerId") Long ownerId);

    /** 开单页：所有有效宠物（带主人信息） */
    List<Pet> listActiveWithOwner();

    List<Pet> search(@Param("keyword") String keyword,
                     @Param("offset") int offset, @Param("limit") int limit);

    long countSearch(@Param("keyword") String keyword);

    int insert(Pet p);

    int update(Pet p);

    int setActive(@Param("id") Long id, @Param("active") boolean active);
}
