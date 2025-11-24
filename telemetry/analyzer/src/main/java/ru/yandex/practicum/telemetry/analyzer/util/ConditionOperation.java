package ru.yandex.practicum.telemetry.analyzer.util;

import ru.yandex.practicum.kafka.telemetry.event.ConditionOperationAvro;

public enum ConditionOperation {
    GREATER_THAN {
        @Override
        public boolean evaluate(int actual, int expected) {
            return actual > expected;
        }
    },
    LOWER_THAN {
        @Override
        public boolean evaluate(int actual, int expected) {
            return actual < expected;
        }
    },
    EQUALS {
        @Override
        public boolean evaluate(int actual, int expected) {
            return actual == expected;
        }
    };

    public abstract boolean evaluate(int actual, int expected);

    public static ConditionOperation fromAvro(ConditionOperationAvro avro) {
        return ConditionOperation.valueOf(avro.name());
    }
}