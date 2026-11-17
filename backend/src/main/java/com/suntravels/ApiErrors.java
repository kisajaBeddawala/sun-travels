package com.suntravels;

import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestControllerAdvice
class ApiErrors {
    record FieldError(String field, String message) {}
    record ErrorBody(String code, String message, List<FieldError> fieldErrors) {}

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<ErrorBody> invalidFields(MethodArgumentNotValidException exception) {
        var details = exception.getBindingResult().getFieldErrors().stream()
                .map(error -> new FieldError(error.getField(), error.getDefaultMessage())).toList();
        return invalid("Review the highlighted fields.", details);
    }

    @ExceptionHandler(TravelService.FieldProblem.class)
    ResponseEntity<ErrorBody> invalidRule(TravelService.FieldProblem exception) {
        return invalid(exception.getMessage(), List.of(new FieldError(exception.field(), exception.getMessage())));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    ResponseEntity<ErrorBody> malformed() {
        return invalid("Check the date format and enter whole numbers for nights, rooms and adults.", List.of());
    }

    @ExceptionHandler(ResponseStatusException.class)
    ResponseEntity<ErrorBody> httpStatus(ResponseStatusException exception) {
        return ResponseEntity.status(exception.getStatusCode())
                .body(new ErrorBody("HTTP_" + exception.getStatusCode().value(), exception.getReason(), List.of()));
    }

    private ResponseEntity<ErrorBody> invalid(String message, List<FieldError> details) {
        return ResponseEntity.badRequest().body(new ErrorBody("VALIDATION_ERROR", message, details));
    }
}
