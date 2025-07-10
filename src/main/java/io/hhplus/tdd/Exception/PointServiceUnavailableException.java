package io.hhplus.tdd.Exception;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class PointServiceUnavailableException  extends RuntimeException {
    public PointServiceUnavailableException(Throwable  e) {
        super("포인트 서버 연결에 실패했습니다.");
        log.error("포인트 서버 연결 실패", e);
    }

    public PointServiceUnavailableException(String msg, Throwable  e) {
        super(msg);
        log.error("포인트 서버 연결 실패", e);
    }
}
