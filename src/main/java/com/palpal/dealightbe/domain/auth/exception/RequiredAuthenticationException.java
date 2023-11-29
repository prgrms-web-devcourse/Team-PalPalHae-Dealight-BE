package com.palpal.dealightbe.domain.auth.exception;

import com.palpal.dealightbe.global.error.ErrorCode;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class RequiredAuthenticationException extends RuntimeException {

	private final ErrorCode errorCode;
}
