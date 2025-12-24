package ru.yandex.practicum.commerce.dto.exception;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import org.springframework.http.HttpStatus;

import java.util.List;

@Data
@SuperBuilder
@NoArgsConstructor
public abstract class AbstractErrorResponse {
    private ApiThrowableCause cause;
    private List<ApiStackTraceElement> stackTrace;
    private List<ApiThrowableCause> suppressed;
    private String localizedMessage;
    private String message;
    private HttpStatus httpStatus;
    private String userMessage;
}