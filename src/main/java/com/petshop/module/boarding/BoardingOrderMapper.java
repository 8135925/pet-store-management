package com.petshop.module.boarding;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface BoardingOrderMapper {

    BoardingOrder findById(@Param("id") Long id);

    /** 笼位时间区间冲突数（BR-10）：start_at < newEnd 且 oldEnd > newStart */
    int countConflict(@Param("roomId") Long roomId,
                      @Param("startAt") String startAt,
                      @Param("end") String end,
                      @Param("excludeId") Long excludeId);

    /** 该宠物是否有在店寄养（BR-13 提示） */
    int countActiveByPet(@Param("petId") Long petId);

    int insert(BoardingOrder o);

    int markCheckedIn(@Param("id") Long id, @Param("startAt") String startAt);

    int extend(@Param("id") Long id, @Param("plannedEndOn") String plannedEndOn, @Param("remark") String remark);

    /** 退住结算：状态 + 时间 + 晚数 + 账单号一次落库 */
    int checkout(@Param("id") Long id, @Param("endAt") String endAt,
                 @Param("days") int days, @Param("billId") Long billId);

    int cancel(@Param("id") Long id);

    // ---------------- 看板 / 列表 ----------------

    List<BoardingOrder> findInHouse();

    List<BoardingOrder> findTodayArrivals(@Param("today") String today);

    List<BoardingOrder> findTodayDue(@Param("today") String today);

    List<BoardingOrder> search(@Param("keyword") String keyword,
                               @Param("dateFrom") String dateFrom,
                               @Param("dateTo") String dateTo,
                               @Param("offset") int offset, @Param("limit") int limit);

    long countSearch(@Param("keyword") String keyword,
                     @Param("dateFrom") String dateFrom,
                     @Param("dateTo") String dateTo);

    long countByNoPrefix(@Param("prefix") String prefix);
}
