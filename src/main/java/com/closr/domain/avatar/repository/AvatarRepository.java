package com.closr.domain.avatar.repository;

import com.closr.domain.avatar.entity.Avatar;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AvatarRepository extends JpaRepository<Avatar, Long> {
    Optional<Avatar> findByJobId(String jobId);
}