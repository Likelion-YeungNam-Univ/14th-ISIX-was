package com.closr.domain.garment.repository;

import com.closr.domain.garment.entity.GarmentSizeSpec;
import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GarmentSizeSpecRepository extends JpaRepository<GarmentSizeSpec, Long> {

    /**
     * 여러 의류의 스펙을 한 번에 가져옵니다.
     *
     * <p>의류마다 따로 조회하면 목록 크기만큼 쿼리가 나가서(N+1) 한 번에 읽습니다.
     */
    List<GarmentSizeSpec> findByGarmentIdIn(Collection<Long> garmentIds);
}
