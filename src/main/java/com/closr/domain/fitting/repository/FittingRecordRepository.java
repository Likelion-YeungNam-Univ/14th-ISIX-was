package com.closr.domain.fitting.repository;

import com.closr.domain.fitting.entity.FittingRecord;
import com.closr.domain.user.entity.Session;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface FittingRecordRepository extends JpaRepository<FittingRecord, Long> {

    /**
     * 세션의 피팅 기록을 최신순으로 가져옵니다.
     *
     * <p>목록에 의류 이름을 보여줘야 해서 garment 를 함께 읽습니다.
     * 지연 로딩에 맡기면 기록 수만큼 쿼리가 나갑니다.
     */
    @Query("select r from FittingRecord r join fetch r.garment "
            + "where r.session = :session order by r.createdAt desc")
    List<FittingRecord> findAllBySessionWithGarment(Session session);
}
