package com.closr.domain.garment.repository;

import com.closr.domain.garment.entity.GarmentLike;
import com.closr.domain.user.entity.Session;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GarmentLikeRepository extends JpaRepository<GarmentLike, Long> {

    boolean existsBySessionAndGarmentId(Session session, Long garmentId);

    Optional<GarmentLike> findBySessionAndGarmentId(Session session, Long garmentId);

    @EntityGraph(attributePaths = {"garment"})
    List<GarmentLike> findAllBySessionOrderByCreatedAtDesc(Session session);
}
