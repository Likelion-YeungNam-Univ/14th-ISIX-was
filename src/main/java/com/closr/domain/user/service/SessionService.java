package com.closr.domain.user.service;

import com.closr.domain.user.entity.Session;
import com.closr.domain.user.repository.SessionRepository;
import com.closr.global.exception.CustomException;
import com.closr.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SessionService {

    private final SessionRepository sessionRepository;

    @Transactional
    public String createGuestSession() {
        String token = UUID.randomUUID().toString();

        Session newSession = Session.builder()
                .sessionToken(token)
                .expiresAt(LocalDateTime.now().plusDays(30))
                .build();

        sessionRepository.save(newSession);

        return token;
    }

    public Session validateSession(String sessionToken) {
        Session session = sessionRepository.findBySessionToken(sessionToken)
                .orElseThrow(() -> new CustomException(ErrorCode.SESSION_NOT_FOUND));

        if (session.isExpired(LocalDateTime.now())) {
            throw new CustomException(ErrorCode.SESSION_EXPIRED);
        }

        return session;
    }
}