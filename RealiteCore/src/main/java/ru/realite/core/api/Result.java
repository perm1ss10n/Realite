package ru.realite.core.api;

import java.util.Objects;
import java.util.function.Function;

/**
 * Result<T> — успешный результат или ошибка.
 *
 * Пример:
 * Result<Integer> r = parseInt("123");
 * int x = r.unwrapOr(0);
 */
public sealed interface Result<T> permits Result.Ok, Result.Err {

    record Ok<T>(T value) implements Result<T> {
        public Ok {
            // value может быть null — иногда это нужно. Если не хочешь null, включи requireNonNull.
        }
    }

    record Err<T>(String message, Throwable cause) implements Result<T> {
        public Err {
            Objects.requireNonNull(message, "message");
        }
    }

    static <T> Result<T> ok(T value) {
        return new Ok<>(value);
    }

    static <T> Result<T> err(String message) {
        return new Err<>(message, null);
    }

    static <T> Result<T> err(String message, Throwable cause) {
        return new Err<>(message, cause);
    }

    default boolean isOk() {
        return this instanceof Ok<?>;
    }

    default boolean isErr() {
        return this instanceof Err<?>;
    }

    /**
     * Достаёт значение или кидает IllegalStateException (удобно, но аккуратно).
     */
    default T unwrap() {
        if (this instanceof Ok<T> ok) return ok.value();
        Err<T> err = (Err<T>) this;
        if (err.cause() != null) {
            throw new IllegalStateException(err.message(), err.cause());
        }
        throw new IllegalStateException(err.message());
    }

    /**
     * Достаёт значение или возвращает defaultValue.
     */
    default T unwrapOr(T defaultValue) {
        return this instanceof Ok<T> ok ? ok.value() : defaultValue;
    }

    /**
     * map: Ok(value) -> Ok(mapper(value)), Err -> Err
     */
    default <U> Result<U> map(Function<? super T, ? extends U> mapper) {
        Objects.requireNonNull(mapper, "mapper");
        if (this instanceof Ok<T> ok) return Result.ok(mapper.apply(ok.value()));
        Err<T> err = (Err<T>) this;
        return Result.err(err.message(), err.cause());
    }

    /**
     * flatMap: Ok(value) -> mapper(value), Err -> Err
     */
    default <U> Result<U> flatMap(Function<? super T, Result<U>> mapper) {
        Objects.requireNonNull(mapper, "mapper");
        if (this instanceof Ok<T> ok) return mapper.apply(ok.value());
        Err<T> err = (Err<T>) this;
        return Result.err(err.message(), err.cause());
    }

    /**
     * Меняет только текст ошибки (cause сохраняется).
     */
    default Result<T> mapErr(Function<String, String> mapper) {
        Objects.requireNonNull(mapper, "mapper");
        if (this instanceof Ok<T>) return this;
        Err<T> err = (Err<T>) this;
        return Result.err(mapper.apply(err.message()), err.cause());
    }
}
