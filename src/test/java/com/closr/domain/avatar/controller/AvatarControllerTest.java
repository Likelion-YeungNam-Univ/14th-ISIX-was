package com.closr.domain.avatar.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.closr.domain.avatar.entity.Avatar;
import com.closr.domain.avatar.service.AvatarService;
import com.closr.domain.user.entity.Session;
import com.closr.domain.user.service.SessionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Map;

/**
 * 아바타 컨트롤러 테스트.
 *
 * <p>실제 AvatarService는 Mock으로 대체하고, 세션은 request attribute로 직접 주입합니다.
 * (필터는 addFilters=false로 비활성화되어 있어, 여기서 세션을 채워주지 않으면
 * @RequestAttribute("session")이 비어있어 요청 자체가 실패합니다.)
 */
@WebMvcTest(AvatarController.class)
@AutoConfigureMockMvc(addFilters = false)
class AvatarControllerTest {

    @MockBean
    private AvatarService avatarService;

    @MockBean
    private SessionService sessionService;
    @Autowired
    private MockMvc mockMvc;

    private Session session;

    @BeforeEach
    void setUp() {
        session = Mockito.mock(Session.class);
        given(session.getId()).willReturn(1L);
    }

    private MockMultipartFile photo() {
        return new MockMultipartFile("photo", "body.jpg", "image/jpeg", "fake-image".getBytes());
    }

    @Test
    @DisplayName("아바타 생성 요청은 202 와 jobId 를 반환한다")
    void createAvatarReturnsAccepted() throws Exception {
        Avatar avatar = Mockito.mock(Avatar.class);
        given(avatar.getJobId()).willReturn("test-job-0001");
        given(avatarService.requestAvatar(any(), any(), anyInt(), anyInt())).willReturn(avatar);

        mockMvc.perform(multipart("/api/v1/avatars")
                        .file(photo())
                        .param("height", "165")
                        .param("weight", "55")
                        .requestAttr("session", session))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.jobId").value("test-job-0001"));
    }

    @ParameterizedTest(name = "height={0}, weight={1} 이면 {2} 필드가 거부된다")
    @CsvSource({
            "129, 55, height",
            "201, 55, height",
            "165, 29, weight",
            "165, 151, weight"
    })
    @DisplayName("범위를 벗어난 키 · 몸무게는 400 과 함께 문제 필드를 알려준다")
    void rejectsOutOfRangeMeasurements(String height, String weight, String field) throws Exception {
        mockMvc.perform(multipart("/api/v1/avatars")
                        .file(photo())
                        .param("height", height)
                        .param("weight", weight)
                        .requestAttr("session", session))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("INVALID_INPUT"))
                .andExpect(jsonPath("$.error.field").value(field));
    }
    @ParameterizedTest(name = "height={0}, weight={1} 은 경계값이라 통과한다")
    @CsvSource({"130, 30", "200, 150"})
    @DisplayName("경계값은 허용한다")
    void acceptsBoundaryMeasurements(String height, String weight) throws Exception {
        Avatar avatar = Mockito.mock(Avatar.class);
        given(avatar.getJobId()).willReturn("test-job-0002");
        given(avatarService.requestAvatar(any(), any(), anyInt(), anyInt())).willReturn(avatar);

        mockMvc.perform(multipart("/api/v1/avatars")
                        .file(photo())
                        .param("height", height)
                        .param("weight", weight)
                        .requestAttr("session", session))
                .andExpect(status().isAccepted());
    }

    @Test
    @DisplayName("상태 조회는 저장된 치수를 반환한다")
    void getAvatarStatusReturnsMeasurements() throws Exception {
        Avatar avatar = Mockito.mock(Avatar.class);
        given(avatar.getStatus()).willReturn("done");
        given(avatar.getId()).willReturn(1L);
        given(avatar.getMeasurements()).willReturn(Map.of("chest_circ", 87.2));
        given(avatarService.getAvatarStatus(any(), Mockito.eq("test-job-0001"))).willReturn(avatar);

        mockMvc.perform(get("/api/v1/avatars/{jobId}", "test-job-0001")
                        .requestAttr("session", session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("done"))
                .andExpect(jsonPath("$.data.measurements.chest_circ").value(87.2));
    }

    @Test
    @DisplayName("목록 조회는 세션의 아바타들을 반환한다")
    void getMyAvatarsReturnsList() throws Exception {
        Avatar avatar = Mockito.mock(Avatar.class);
        given(avatar.getStatus()).willReturn("done");
        given(avatar.getId()).willReturn(1L);
        given(avatar.getMeasurements()).willReturn(null);
        given(avatarService.getMyAvatars(any())).willReturn(List.of(avatar));

        mockMvc.perform(get("/api/v1/avatars/me")
                        .requestAttr("session", session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].avatarId").value(1))
                .andExpect(jsonPath("$.data[0].status").value("done"));
    }
}