package ru.yandex.practicum.commerce.dto.exception;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ApiThrowableCause {
    private List<ApiStackTraceElement> stackTrace;
    private String message;
    private String localizedMessage;
}