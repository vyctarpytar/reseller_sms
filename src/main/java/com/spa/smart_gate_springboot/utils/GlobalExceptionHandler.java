package com.spa.smart_gate_springboot.utils;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import org.springframework.web.bind.MethodArgumentNotValidException;

import java.util.HashMap;
import java.util.Map;

@ControllerAdvice
public class GlobalExceptionHandler {

    private static String BACKEND_ERROR =  "Failed !!  Backend Error";
    private static String ACCESS_DENIED_ERROR =  "Access Denied - User Not AUthorised To Perform Action- ";

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<StandardJsonResponse> handleRuntimeException(RuntimeException ex) {
        ex.printStackTrace();
        StandardJsonResponse response = new StandardJsonResponse();
        response.setSuccess(false);
        response.setMessage("message",BACKEND_ERROR + " - " + ex.getMessage(), response);
        response.setStatus(500);
        return new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
    }



    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<StandardJsonResponse> handleResourceNotFound(ResourceNotFoundException ex) {
        StandardJsonResponse response = new StandardJsonResponse();
        response.setSuccess(false);
        response.setMessage("message",BACKEND_ERROR+ " - " + ex.getMessage(), response);
        response.setStatus(HttpStatus.NOT_FOUND.value());
        return new ResponseEntity<>(response, HttpStatus.NOT_FOUND);
    }

    public static class ResourceNotFoundException extends RuntimeException {
        public ResourceNotFoundException(String message) {
            super(message);
        }
    }

    @ExceptionHandler( AuthenticationException.class)
    public ResponseEntity<StandardJsonResponse> handleAuthenticationException(AuthenticationException ex) {
        ex.printStackTrace();
        StandardJsonResponse response = new StandardJsonResponse();
        response.setSuccess(false);
        response.setMessage("message", "Invalid Token", response);
        response.setStatus(401);
        return new ResponseEntity<>(response, HttpStatus.UNAUTHORIZED);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<StandardJsonResponse> handleException(Exception ex) {
        ex.printStackTrace();
        StandardJsonResponse response = new StandardJsonResponse();
        response.setSuccess(false);
        response.setMessage("message",BACKEND_ERROR+ " - " + ex.getMessage(), response);
        response.setStatus(400);
        return new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<StandardJsonResponse>  handleValidationExceptions(MethodArgumentNotValidException ex) {
        ex.printStackTrace();
        StandardJsonResponse response = new StandardJsonResponse();
        response.setSuccess(false);
        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getFieldErrors().forEach(error -> errors.put(error.getField(), error.getDefaultMessage()));
        response.setMessage("message",errors,response);
        return  new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
    }


    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<StandardJsonResponse> handleIllegalArgumentException(IllegalArgumentException ex) {
        ex.printStackTrace();
        StandardJsonResponse response = new StandardJsonResponse();
        response.setSuccess(false);
        response.setMessage("message", BACKEND_ERROR+ " - " + ex.getMessage(),response);
        response.setStatus(400);
        return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<StandardJsonResponse> handleAccessDeniedException(AccessDeniedException ex) {
        ex.printStackTrace();
        StandardJsonResponse response = new StandardJsonResponse();
        response.setSuccess(false);

        response.setMessage("message", ACCESS_DENIED_ERROR,response);
        response.setStatus(403);
        return new ResponseEntity<>(response, HttpStatus.FORBIDDEN);
    }
}
