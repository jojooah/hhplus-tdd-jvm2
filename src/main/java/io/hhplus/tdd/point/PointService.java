package io.hhplus.tdd.point;

import io.hhplus.tdd.Exception.*;
import io.hhplus.tdd.database.PointHistoryTable;
import io.hhplus.tdd.database.UserPointTable;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.function.Supplier;


@Service
@RequiredArgsConstructor
public class PointService {

    private final UserPointTable userPointTable;
    private final PointHistoryTable pointHistoryTable;

    public UserPoint getUserPointById(long userId) {
        return userPointTable.selectById(userId);

    }

    public List<PointHistory> getHistories(long userId) {
        return pointHistoryTable.selectAllByUserId(userId);
    }


    public UserPoint chargePoint(long userId, long amount) {
        if(amount == 0) throw new ZeroPointException("충전할 금액이 없습니다.");
        if(amount < 0) throw new MinusPointException("금액이 적절하지 않습니다(음수입력)");
        if(userPointTable.selectById(userId).point() + amount > 100000) {
            throw new PointLimitExceedeException("한도 이상 충전 불가능합니다(10만원)");
        }

        try {
            // 기존 금액 + 충전금액으로 대체
            long finalAmount = amount + userPointTable.selectById(userId).point();

            //이력 남기기
            recordHistory(userId,amount,TransactionType.CHARGE);

            // 포인트 충전
            return executeWithTimeout(() -> userPointTable.insertOrUpdate(userId, finalAmount));

        } catch (PointServiceTimeoutException e) {
            // 롤백 수행
            recordHistory(userId,amount,TransactionType.CHARGE_CANCEL);
            throw e;

        } catch (RuntimeException e) {
            // 롤백 수행
            recordHistory(userId,amount,TransactionType.CHARGE_CANCEL);
            throw new PointServiceUnavailableException("서버연결에 실패했습니다", e);
        }
    }

    private UserPoint executeWithTimeout(Supplier<UserPoint> supplier) {
        long start = System.currentTimeMillis();
        UserPoint result = supplier.get();

        if (System.currentTimeMillis() - start > 5000) {
            throw new PointServiceTimeoutException("서버 연결이 지연되고 있습니다.");
        }

        return result;
    }


    public UserPoint usePoint(long userId, long amount) {
        try {
            UserPoint current = userPointTable.selectById(userId);

            // 충분한지 확인
            if (current.point() < amount) {
                throw new InsufficientPointsException("포인트가 부족합니다.");
            }

            // 이력 남기기
            pointHistoryTable.insert(userId, amount, TransactionType.USE, System.currentTimeMillis());

            long finalAmount = current.point() - amount;

            // 실제 차감
            return executeWithTimeout(() -> userPointTable.insertOrUpdate(userId, finalAmount));

        } catch (PointServiceTimeoutException e) {
            // 롤백 이력 남기기
            pointHistoryTable.insert(userId, amount, TransactionType.USE_CANCEL, System.currentTimeMillis());
            throw e;

        } catch (InsufficientPointsException e){
            throw e;
        } catch (RuntimeException e) {
            throw new PointServiceUnavailableException("서버연결에 실패했습니다", e);
        }
    }


    private void recordHistory(long userId, long amount, TransactionType type) {
        pointHistoryTable.insert(userId, amount, type, System.currentTimeMillis());
    }
}

