package io.hhplus.tdd;

import io.hhplus.tdd.Exception.*;
import io.hhplus.tdd.database.PointHistoryTable;
import io.hhplus.tdd.database.UserPointTable;
import io.hhplus.tdd.database.UserPointTableSlow;
import io.hhplus.tdd.point.PointHistory;
import io.hhplus.tdd.point.PointService;
import io.hhplus.tdd.point.TransactionType;
import io.hhplus.tdd.point.UserPoint;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class 포인트_충전_테스트 {

    @Test
    void 포인트를_100원_충전하면_100원이_저장됨() {
        PointService pointService = new PointService(new UserPointTable(),new PointHistoryTable());
        long userId = 1L;

        UserPoint result = pointService.chargePoint(userId, 100);
        assertEquals(100, result.point());

    }

    @Test
    void 빵원을_충전하면_예외발생() {
        long userId = 1L;
        PointService pointService = new PointService(new UserPointTable(),new PointHistoryTable());

        assertThrows(ZeroPointException.class, () -> {
            pointService.chargePoint(userId, 0);
        });

    }

    @Test
    void 음수를_충전하면_예외발생() {
        long userId = 1L;
        PointService pointService = new PointService(new UserPointTable(),new PointHistoryTable());

        assertThrows(MinusPointException.class, () -> {
            pointService.chargePoint(userId, -1);
        });

    }

    @Test
    void 기존_금액에_충전한_금액_더해져야됨() {
        long userId = 1L;
        PointService pointService = new PointService(new UserPointTable(),new PointHistoryTable());

        UserPoint result = pointService.chargePoint(userId, 100);
        assertEquals(100, result.point());

        UserPoint result1 = pointService.chargePoint(userId, 200);
        assertEquals(300, result1.point());
    }

    @Test
    void 포인트충전시_히스토리도_기록() {
        long userId = 1L;
        UserPointTable userPointTable = new UserPointTable();
        PointHistoryTable historyTable = new PointHistoryTable();
        PointService service = new PointService(userPointTable, historyTable);

        service.chargePoint(userId, 500);

        List<PointHistory> historyList = historyTable.selectAllByUserId(userId);
        assertEquals(1, historyList.size());
        assertEquals(TransactionType.CHARGE, historyList.get(0).type());
        assertEquals(500, historyList.get(0).amount());
    }

    @Test
    void 충전_실패시_실패_이력_기록_DB연결_실패() {
        long userId = 1L;
        long amount = 100L;

        PointHistoryTable historyTable = Mockito.spy(new PointHistoryTable());
        UserPointTable mockTable = Mockito.mock(UserPointTable.class);

        // 조회할때 npe나지 않도록 설정
        Mockito.when(mockTable.selectById(Mockito.anyLong()))
                .thenReturn(new UserPoint(userId, 0L, System.currentTimeMillis()));

        // 연결 실패하는 디비 세팅
        Mockito.when(mockTable.insertOrUpdate(Mockito.anyLong(), Mockito.anyLong()))
                .thenThrow(new RuntimeException("DB 연결 실패"));

        PointService service = new PointService(mockTable, historyTable);

        assertThrows(PointServiceUnavailableException.class, () -> {
            service.chargePoint(userId, amount);
        });


        List<PointHistory> histories = historyTable.selectAllByUserId(userId);
        assertEquals(2, histories.size());

    }


    @Test
    void 충전_한도_10만원_넘어가면_예외발생() {
        long userId = 1L;
        PointService pointService = new PointService(new UserPointTable(),new PointHistoryTable());

        assertThrows(PointLimitExceedeException.class, () -> {
            pointService.chargePoint(userId, 100001);
        });
    }

    @Test
    void 서버연결_실패시_연결실패_예외발생() {
        long userId = 1L;
        UserPointTable mockTable = Mockito.mock(UserPointTable.class);
        PointHistoryTable pointHistoryTable = new PointHistoryTable();

        // 조회할때 npe나지 않도록 설정
        Mockito.when(mockTable.selectById(Mockito.anyLong()))
               .thenReturn(new UserPoint(userId, 0L, System.currentTimeMillis()));

        // 연결 실패하는 디비 세팅
        Mockito.when(mockTable.insertOrUpdate(Mockito.anyLong(), Mockito.anyLong()))
                .thenThrow(new RuntimeException("DB 연결 실패"));

        PointService service = new PointService(mockTable,pointHistoryTable);

        assertThrows(PointServiceUnavailableException.class, () -> {
            service.chargePoint(userId, 100);
        });
    }

    @Test
    void 서버응답_5초이상_지연되면_타임아웃_예외발생() {
        // 느린 DB상황가정.. 혹은 익명 클래스로 만들기??
        // 테스트 코드에서 바로 확인 가능하도록 익명클래스 사용이 나을수도
        UserPointTable userPointTable = new UserPointTableSlow();
        PointService service = new PointService(userPointTable,new PointHistoryTable());

        assertThrows(PointServiceTimeoutException.class, () -> {
            service.chargePoint(1L, 100);
        });
    }

}
