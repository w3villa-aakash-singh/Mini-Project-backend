package com.w3villa.mini_project_backend.exceptions;


import com.w3villa.mini_project_backend.dtos.ErrorRsponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler (ResourceNotFoundException.class) // Added this!
    public ResponseEntity<ErrorRsponse> handleResourceNotFound(ResourceNotFoundException exception) {

        // Create the response object
        ErrorRsponse error = new ErrorRsponse(
                exception.getMessage(),
                HttpStatus.NOT_FOUND
        );

        // Return the object in the body with a 404 status
        return new ResponseEntity<>(error, HttpStatus.NOT_FOUND);
    }

    // Recommended: Catch general arguments (like the "Email already exists" error)
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorRsponse> handleIllegalArgument(IllegalArgumentException exception) {
        ErrorRsponse error = new ErrorRsponse(exception.getMessage(), HttpStatus.BAD_REQUEST);
        return new ResponseEntity<>(error, HttpStatus.BAD_REQUEST);
    }
}