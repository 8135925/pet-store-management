package com.petshop.module.boarding;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface BoardingRoomMapper {

    BoardingRoom findById(@Param("id") Long id);

    List<BoardingRoom> findAll();

    List<BoardingRoom> findActive();

    /** 带在住数量（看板） */
    List<BoardingRoom> findAllWithUsage();

    int insert(BoardingRoom r);

    int update(BoardingRoom r);

    int setActive(@Param("id") Long id, @Param("active") boolean active);
}
