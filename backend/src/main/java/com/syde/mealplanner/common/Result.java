package com.syde.mealplanner.common;

public class Result<T> {

    private int code;
    private T data;
    private String message;

    public Result() {
    }

    public Result(int code, T data, String message) {
        this.code = code;
        this.data = data;
        this.message = message;
    }

    public static <T> Result<T> success() {
        return new Result<>(200, null, "success");
    }

    public static <T> Result<T> success(T data) {
        return new Result<>(200, data, "success");
    }

    public static <T> Result<T> error(int code, String message) {
        return new Result<>(code, null, message);
    }

    public int getCode() {
        return code;
    }

    public void setCode(int code) {
        this.code = code;
    }

    public T getData() {
        return data;
    }

    public void setData(T data) {
        this.data = data;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
