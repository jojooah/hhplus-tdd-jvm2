package io.hhplus.tdd;

import io.hhplus.tdd.Exception.InsufficientPointsException;
import io.hhplus.tdd.Exception.PointServiceTimeoutException;
import io.hhplus.tdd.Exception.PointServiceUnavailableException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

@RestControllerAdvice
class ApiControllerAdvice extends ResponseEntityExceptionHandler {
    @ExceptionHandler(InsufficientPointsException.class)
    public ResponseEntity<String> handleInsufficientPoints(InsufficientPointsException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ex.getMessage());
    }

    @ExceptionHandler(PointServiceTimeoutException.class)
    public ResponseEntity<String> handleTimeout(PointServiceTimeoutException ex) {
        return ResponseEntity.status(HttpStatus.REQUEST_TIMEOUT).body(ex.getMessage());
    }

    @ExceptionHandler(PointServiceUnavailableException.class)
    public ResponseEntity<String> handleUnavailable(PointServiceUnavailableException ex) {
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(ex.getMessage());
    }


    @ExceptionHandler(value = Exception.class)
    public ResponseEntity<ErrorResponse> handleException(Exception e) {
        return ResponseEntity.status(500).body(new ErrorResponse("500", "에러가 발생했습니다."));
    }
}
