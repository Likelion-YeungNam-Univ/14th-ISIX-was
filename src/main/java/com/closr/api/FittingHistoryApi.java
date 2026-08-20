package com.closr.api;

import com.closr.domain.fitting.dto.ResponseFittingRecordDetailDto;
import com.closr.domain.fitting.dto.ResponseFittingRecordListDto;
import com.closr.domain.user.entity.Session;
import com.closr.global.common.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestMapping;

@Tag(name = "FittingHistory", description = "피팅 기록 조회 API")
@RequestMapping("/api/v1/fittings")
public interface FittingHistoryApi {

    @Operation(summary = "내 피팅 기록 목록",
            description = """
                    세션에 저장된 피팅 기록을 최신순으로 반환합니다. 상세는 fittingId 로
                    다시 조회합니다.

                    **같은 옷은 최신 1건만 나옵니다.** 기록은 피팅을 조회할 때마다
                    쌓이므로, 같은 옷을 두 번 보면 두 건이 됩니다. 옷 하나에 대해
                    알고 싶은 것은 가장 최근 결과입니다.""")
    @GetMapping("/me")
    ResponseEntity<ApiResponse<ResponseFittingRecordListDto>> getMyFittings(
            @Parameter(hidden = true) @RequestAttribute("session") Session session
    );

    @Operation(summary = "피팅 기록 삭제",
            description = """
                    피팅 기록을 지웁니다. 성공하면 본문 없이 204 를 반환합니다.

                    **같은 옷의 기록을 함께 지웁니다.** 목록이 옷마다 최신 1건만
                    보여주므로, 지목된 행만 지우면 숨어 있던 예전 기록이 그 자리에
                    다시 올라와 지워지지 않은 것처럼 보입니다.

                    아바타까지 같은 것만 지웁니다. 아바타를 다시 만든 뒤의 기록은
                    다른 몸의 결과라 남습니다.

                    없는 기록이거나 다른 세션의 기록이면 존재 여부를 감추기 위해
                    404 로 응답합니다.""")
    @DeleteMapping("/{fittingId}")
    ResponseEntity<Void> deleteFitting(
            @Parameter(hidden = true) @RequestAttribute("session") Session session,
            @Parameter(description = "피팅 기록 ID") @PathVariable("fittingId") Long fittingId
    );

    @Operation(summary = "저장된 피팅 결과 조회",
            description = "피팅 당시의 결과를 그대로 반환합니다. 다시 계산하지 않으므로 판정 기준이 바뀌어도 그때 본 값이 나옵니다.")
    @GetMapping("/{fittingId}")
    ResponseEntity<ApiResponse<ResponseFittingRecordDetailDto>> getFitting(
            @Parameter(hidden = true) @RequestAttribute("session") Session session,
            @Parameter(description = "피팅 기록 ID") @PathVariable("fittingId") Long fittingId
    );
}
