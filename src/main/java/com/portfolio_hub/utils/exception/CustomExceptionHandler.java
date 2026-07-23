package com.portfolio_hub.utils.exception;

import com.portfolio_hub.utils.ApiResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.MultipartException;
import org.springframework.web.multipart.support.MissingServletRequestPartException;
import org.springframework.validation.FieldError;
import jakarta.validation.ConstraintViolationException;

@RestControllerAdvice
@Slf4j
public class CustomExceptionHandler {

  @ExceptionHandler(MethodArgumentNotValidException.class)
  ResponseEntity<ApiResponse<Void>> validation(
    MethodArgumentNotValidException exception
  ) {
    String message = exception
      .getBindingResult()
      .getFieldErrors()
      .stream()
      .findFirst()
      .map(this::friendlyValidationMessage)
      .orElse("Please check the information you entered and try again");
    return ResponseEntity.badRequest().body(ApiResponse.failure(message));
  }

  @ExceptionHandler(ConstraintViolationException.class)
  ResponseEntity<ApiResponse<Void>> constraintViolation(ConstraintViolationException exception) {
    String message = exception.getConstraintViolations().stream()
      .findFirst()
      .map(violation -> violation.getMessage())
      .filter(value -> value != null && !value.isBlank())
      .orElse("Please check the information you entered and try again");
    return ResponseEntity.badRequest().body(ApiResponse.failure(message));
  }

  @ExceptionHandler(InvalidInputException.class)
  ResponseEntity<ApiResponse<Void>> badRequest(RuntimeException exception) {
    return ResponseEntity.badRequest().body(
      ApiResponse.failure(exception.getMessage())
    );
  }

  @ExceptionHandler(
    { IllegalArgumentException.class, HttpMessageNotReadableException.class }
  )
  ResponseEntity<ApiResponse<Void>> malformedRequest(Exception exception) {
    log.debug("Request could not be read", exception);
    return ResponseEntity.badRequest().body(
      ApiResponse.failure(
        "Please check the information you entered and try again"
      )
    );
  }

  @ExceptionHandler(
    {
      MissingServletRequestPartException.class,
      MissingServletRequestParameterException.class,
    }
  )
  ResponseEntity<ApiResponse<Void>> missingRequestValue(Exception exception) {
    return ResponseEntity.badRequest().body(
      ApiResponse.failure(
        "Please complete all required information and try again"
      )
    );
  }

  @ExceptionHandler(ResourceExistsException.class)
  ResponseEntity<ApiResponse<Void>> conflict(
    ResourceExistsException exception
  ) {
    return ResponseEntity.status(HttpStatus.CONFLICT).body(
      ApiResponse.failure(exception.getMessage())
    );
  }

  @ExceptionHandler(TooManyRequestsException.class)
  ResponseEntity<ApiResponse<Void>> tooManyRequests(
    TooManyRequestsException exception
  ) {
    return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).body(
      ApiResponse.failure(exception.getMessage())
    );
  }

  @ExceptionHandler(ResourceNotFoundException.class)
  ResponseEntity<ApiResponse<Void>> notFound(
    ResourceNotFoundException exception
  ) {
    return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
      ApiResponse.failure(exception.getMessage())
    );
  }

  @ExceptionHandler(UnauthorizedException.class)
  ResponseEntity<ApiResponse<Void>> unauthorized(
    UnauthorizedException exception
  ) {
    return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
      ApiResponse.failure(exception.getMessage())
    );
  }

  @ExceptionHandler(InvalidOperationException.class)
  ResponseEntity<ApiResponse<Void>> forbidden(
    InvalidOperationException exception
  ) {
    return ResponseEntity.status(HttpStatus.FORBIDDEN).body(
      ApiResponse.failure(exception.getMessage())
    );
  }

  @ExceptionHandler(MaxUploadSizeExceededException.class)
  ResponseEntity<ApiResponse<Void>> uploadTooLarge() {
    return ResponseEntity.status(HttpStatus.PAYLOAD_TOO_LARGE).body(
      ApiResponse.failure(
        "This file is too large. Please choose a smaller file and try again"
      )
    );
  }

  @ExceptionHandler(MultipartException.class)
  ResponseEntity<ApiResponse<Void>> uploadProblem(
    MultipartException exception
  ) {
    log.debug("File upload request could not be processed", exception);
    return ResponseEntity.badRequest().body(
      ApiResponse.failure(
        "We could not read that file upload. Please choose the file again"
      )
    );
  }

  @ExceptionHandler(DataIntegrityViolationException.class)
  ResponseEntity<ApiResponse<Void>> duplicateOrRelatedData(
    DataIntegrityViolationException exception
  ) {
    log.warn("A database rule prevented a request from completing", exception);
    return ResponseEntity.status(HttpStatus.CONFLICT).body(
      ApiResponse.failure(
        "Those changes conflict with information that is already saved. Please review them and try again"
      )
    );
  }

  @ExceptionHandler(ErrorProcessingRequestException.class)
  ResponseEntity<ApiResponse<Void>> processing(
    ErrorProcessingRequestException exception
  ) {
    log.error("A request could not be completed", exception);
    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
      ApiResponse.failure(
        "We could not complete your request right now. Please try again"
      )
    );
  }

  @ExceptionHandler(Exception.class)
  ResponseEntity<ApiResponse<Void>> unexpected(Exception exception) {
    log.error("Unexpected request failure", exception);
    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
      ApiResponse.failure(
        "Something went wrong while completing your request. Please try again"
      )
    );
  }

  private String friendlyValidationMessage(FieldError error) {
    String field = error.getField()
      .replaceAll("([a-z])([A-Z])", "$1 $2")
      .toLowerCase();
    return switch (String.valueOf(error.getCode())) {
      case "NotBlank", "NotEmpty", "NotNull" -> "Please complete the " + field + " field";
      case "Email" -> "Enter a valid email address";
      case "Size" -> "Please enter a valid " + field;
      default -> {
        String value = error.getDefaultMessage();
        yield value == null || value.isBlank()
          ? "Please check the " + field + " field"
          : value;
      }
    };
  }
}
