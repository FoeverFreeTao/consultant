package com.zyt.consultant.guardrail;

public class InputRiskBlockedException extends RuntimeException {

    public InputRiskBlockedException(String message) {
        super(message);
    }

    public InputRiskBlockedException(String message, Throwable cause) {
        super(message, cause);
    }
}
