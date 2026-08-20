package com.closr.domain.garment.repository;

import com.closr.domain.garment.entity.Garment;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GarmentRepository extends JpaRepository<Garment, Long> {

    /** 목록 순서를 시드 등록 순서로 고정합니다. 화면마다 순서가 달라지지 않게 합니다. */
    List<Garment> findAllByOrderByIdAsc();
}
