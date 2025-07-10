package io.hhplus.tdd.Exception;


import lombok.extern.slf4j.Slf4j;

@Slf4j
public class PointServiceTimeoutException extends RuntimeException {
    public PointServiceTimeoutException(String msg) {
        super(msg);
    }

    public PointServiceTimeoutException(Throwable  e) {
        super("서버 연결이 지연되고 있습니다.");
        log.error("서버 연결이 지연", e);
    }


}
