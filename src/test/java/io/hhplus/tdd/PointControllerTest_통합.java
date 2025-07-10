package io.hhplus.tdd;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
public class PointControllerTest_통합 {
    @Autowired
    private MockMvc mockMvc;


    @Test
    void 포인트_충전_및_조회_통합테스트() throws Exception {
        long userId = 1L;
        long amount = 500L;

        // 포인트 충전
        MvcResult chargeResult = mockMvc.perform(patch("/point/{id}/charge", userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(String.valueOf(amount)))
                        .andExpect(status().isOk())
                        .andReturn(); // 결과를 가져옴

        System.out.println("충전: " + chargeResult.getResponse().getContentAsString());

        // 포인트 조회
        MvcResult result =  mockMvc.perform(get("/point/{id}", userId))
                .andExpect(status().isOk())
                .andReturn(); //  결과를 가져옴

        System.out.println("조회: " + result.getResponse().getContentAsString());

    }

    @Test
    void 포인트_사용_통합테스트() throws Exception {
        long userId = 2L;
        long amount = 1000L;

        // 먼저 2000원 충전
        mockMvc.perform(patch("/point/{id}/charge", userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(String.valueOf(2000L)))
                .andExpect(status().isOk());

        // 1000원 사용
        MvcResult result = mockMvc.perform(patch("/point/{id}/use", userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(String.valueOf(amount)))
                .andExpect(status().isOk())
                .andReturn();

        System.out.println("포인트 사용: " + result.getResponse().getContentAsString());

        //  잔액 확인
        MvcResult leftResult = mockMvc.perform(get("/point/{id}", userId))
                .andExpect(status().isOk())
                .andReturn();

        System.out.println("남은 잔액: " + leftResult.getResponse().getContentAsString());

    }

    @Test
    void 포인트_부족시_사용_실패_테스트() throws Exception {
        long userId = 3L;

        // 충전 없이 바로 사용 요청
        mockMvc.perform(patch("/point/{id}/use", userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(String.valueOf(1000L)))
                .andExpect(status().is4xxClientError()); // 400 에러 발생. 사용자 잘못!
    }

    @Test
    void 포인트_이력_조회_통합테스트() throws Exception {
        long userId = 4L;

        // 충전
        mockMvc.perform(patch("/point/{id}/charge", userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(String.valueOf(500L)))
                .andExpect(status().isOk());

        // 사용
        mockMvc.perform(patch("/point/{id}/use", userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(String.valueOf(200L)))
                .andExpect(status().isOk());

        // 이력 조회
        MvcResult historyResult = mockMvc.perform(get("/point/{id}/histories", userId))
                .andExpect(status().isOk())
                .andReturn();

        System.out.println("포인트 이력: " + historyResult.getResponse().getContentAsString());
    }



}
