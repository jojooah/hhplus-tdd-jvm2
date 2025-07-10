package io.hhplus.tdd;

import io.hhplus.tdd.Exception.InsufficientPointsException;
import io.hhplus.tdd.Exception.PointServiceTimeoutException;
import io.hhplus.tdd.Exception.PointServiceUnavailableException;
import io.hhplus.tdd.database.PointHistoryTable;
import io.hhplus.tdd.database.UserPointTable;
import io.hhplus.tdd.database.UserPointTableSlow;
import io.hhplus.tdd.point.PointHistory;
import io.hhplus.tdd.point.PointService;
import io.hhplus.tdd.point.TransactionType;
import io.hhplus.tdd.point.UserPoint;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class 포인트_사용_테스트 {
    private PointService pointService;

    @BeforeEach
    void setup() {
        pointService = new PointService(new UserPointTable(),new PointHistoryTable());
    }

    @Test
    void 포인트를_100원_사용하면_100원만큼_차감() {
        long userId = 1L;
        // 200원 충전
        pointService.chargePoint(userId, 200);

        // 100원 사용
        pointService.usePoint(userId, 100);

        UserPoint result1 = pointService.getUserPointById(userId);
        assertEquals(100, result1.point());
    }

    @Test
    void 포인트가_모자라면_예외_발생() {
        long userId = 1L;
        // 50원 충전
        pointService.chargePoint(userId, 50);

        //100원 사용하면 모자라니까 익셉션 발생
        assertThrows(InsufficientPointsException.class, () -> {
            pointService.usePoint(userId, 100);
        });
    }

    @Test
    void 서버연결_실패시_연결실패_예외발생() {
        long userId = 1L;
        long amount = 100;

        UserPointTable mockTable = Mockito.mock(UserPointTable.class);
        PointHistoryTable historyTable = new PointHistoryTable();

        // 500원 보유
        Mockito.when(mockTable.selectById(Mockito.anyLong()))
                .thenReturn(new UserPoint(userId, 500L, System.currentTimeMillis()));

        // 연결 실패하는 디비 세팅
        Mockito.when(mockTable.insertOrUpdate(Mockito.anyLong(), Mockito.anyLong()))
                .thenThrow(new RuntimeException("DB 연결 실패"));

        PointService service = new PointService(mockTable, historyTable);

        assertThrows(PointServiceUnavailableException.class, () -> {
            service.usePoint(userId, amount);
        });
    }

    @Test
    void 서버응답_5초이상_지연되면_타임아웃_예외발생() {
        long userId = 1L;
        long amount = 100;

        // 느린 DB상황가정.. 혹은 익명 클래스로 만들기??
        // 테스트 코드에서 바로 확인 가능하도록 익명클래스 사용이 나을수도
        // 500원 보유
        // 실제 메서드 실행해야 하므로 mock이 아닌 spy사용
        UserPointTableSlow spy = Mockito.spy(new UserPointTableSlow());
        Mockito.when(spy.selectById(Mockito.anyLong()))
                .thenReturn(new UserPoint(userId, 500L, System.currentTimeMillis()));

        PointService service = new PointService(spy,new PointHistoryTable());

        assertThrows(PointServiceTimeoutException.class, () -> {
            service.usePoint(userId, amount);
        });
    }


    @Test
    void 포인트사용시_히스토리도_기록() {
        long userId = 1L;
        long amount = 300L;

        UserPointTable userPointTable = new UserPointTable();
        PointHistoryTable historyTable = new PointHistoryTable();

        // 포인트를 먼저 충전
        PointService service = new PointService(userPointTable, historyTable);
        service.chargePoint(userId, 1000L);

        // 포인트 사용
        service.usePoint(userId, amount);

        // 히스토리 확인
        List<PointHistory> historyList = historyTable.selectAllByUserId(userId);

        assertEquals(2, historyList.size());  // 1건은 CHARGE, 1건은 USE
        PointHistory useHistory = historyList.get(1);

        assertEquals(userId, useHistory.userId());
        assertEquals(amount, useHistory.amount());
        assertEquals(TransactionType.USE, useHistory.type());
    }

    // 서버 연결 실패 에러는 애초에 이력 등록도 하지 않으므로 테스트 할 필요 x
    @Test
    void 포인트사용_실패시_USE_CANCEL_이력_기록_타임아웃에러() {
        long userId = 1L;
        long amount = 100L;

        PointHistoryTable historyTable = new PointHistoryTable();

        // 500원 보유
        UserPoint mockUserPoint = new UserPoint(userId, 500L, System.currentTimeMillis());

        // 실패하도록 구성 (타임아웃 에러)
        UserPointTable mockTable = Mockito.mock(UserPointTable.class);
        Mockito.when(mockTable.selectById(Mockito.anyLong())).thenReturn(mockUserPoint);
        Mockito.when(mockTable.insertOrUpdate(Mockito.anyLong(), Mockito.anyLong()))
                .thenThrow(new PointServiceTimeoutException("DB 연결 실패"));

        PointService service = new PointService(mockTable, historyTable);

        assertThrows(PointServiceTimeoutException.class, () -> {
            service.usePoint(userId, amount);
        });

        // 히스토리 확인: USE → 실패 → USE_CANCEL
        List<PointHistory> historyList = historyTable.selectAllByUserId(userId);
        assertEquals(2, historyList.size());

        PointHistory cancelHistory = historyList.get(1);
        assertEquals(TransactionType.USE_CANCEL, cancelHistory.type());
        assertEquals(userId, cancelHistory.userId());
        assertEquals(amount, cancelHistory.amount());
    }

}
