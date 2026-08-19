package com.closr.domain.fitting.repository;

import com.closr.domain.avatar.entity.Avatar;
import com.closr.domain.fitting.entity.FittingRecord;
import com.closr.domain.garment.entity.Garment;
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

    /**
     * 한 옷의 기록을 모두 지웁니다.
     *
     * <p>목록이 옷마다 최신 1건만 보여주므로 하나만 지우면 예전 기록이 그 자리에
     * 다시 올라옵니다. 사용자에게는 지워지지 않은 것으로 보입니다.
     *
     * <p>아바타까지 같은 것만 지웁니다. 아바타를 다시 만들었다면 다른 몸의
     * 기록이라 남겨 두는 것이 맞습니다.
     *
     * @return 지운 건수
     */
    int deleteBySessionAndAvatarAndGarment(Session session, Avatar avatar, Garment garment);
}
