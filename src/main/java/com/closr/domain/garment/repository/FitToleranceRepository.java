package com.closr.domain.garment.repository;

import com.closr.domain.garment.entity.FitTolerance;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FitToleranceRepository extends JpaRepository<FitTolerance, Long> {

    /** 의류의 핏에 해당하는 부위별 허용 범위를 한 번에 가져옵니다. */
    List<FitTolerance> findByFit(String fit);
}
