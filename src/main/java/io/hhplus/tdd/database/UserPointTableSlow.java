package io.hhplus.tdd.database;

import io.hhplus.tdd.point.UserPoint;
import org.springframework.stereotype.Component;

/**
 * 지연되는 상황을 위해 생성...
 */
@Component
public class UserPointTableSlow extends UserPointTable{

    @Override
    public UserPoint insertOrUpdate(long id, long amount) {
        try {
            Thread.sleep(6000);  // 6초
        } catch (InterruptedException ignored) {}
        return super.insertOrUpdate(id, amount);
    }

}
