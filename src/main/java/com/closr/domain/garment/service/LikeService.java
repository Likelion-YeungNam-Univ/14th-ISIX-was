package com.closr.domain.garment.service;

import com.closr.domain.garment.dto.ResponseLikeDto;
import com.closr.domain.garment.entity.Garment;
import com.closr.domain.garment.entity.GarmentLike;
import com.closr.domain.garment.repository.GarmentLikeRepository;
import com.closr.domain.garment.repository.GarmentRepository;
import com.closr.domain.user.entity.Session;
import com.closr.global.exception.CustomException;
import com.closr.global.exception.ErrorCode;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class LikeService {

    private final GarmentLikeRepository garmentLikeRepository;
    private final GarmentRepository garmentRepository;

    @Transactional
    public void like(Session session, Long garmentId) {
        Garment garment = garmentRepository.findById(garmentId)
                .orElseThrow(() -> new CustomException(ErrorCode.GARMENT_NOT_FOUND));

        if (garmentLikeRepository.existsBySessionAndGarmentId(session, garmentId)) {
            return; // 이미 찜한 상태면 그대로 성공 처리 (idempotent)
        }

        garmentLikeRepository.save(
                GarmentLike.builder().session(session).garment(garment).build()
        );
    }

    @Transactional
    public void unlike(Session session, Long garmentId) {
        garmentLikeRepository.findBySessionAndGarmentId(session, garmentId)
                .ifPresent(garmentLikeRepository::delete);
    }

    public List<ResponseLikeDto> getMyLikes(Session session) {
        return garmentLikeRepository.findAllBySessionOrderByCreatedAtDesc(session)
                .stream()
                .map(ResponseLikeDto::from)
                .toList();
    }
}
