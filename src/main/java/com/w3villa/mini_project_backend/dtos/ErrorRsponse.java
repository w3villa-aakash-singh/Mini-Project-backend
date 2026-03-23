package com.w3villa.mini_project_backend.dtos;

import org.springframework.http.HttpStatus;

public record ErrorRsponse(String message , HttpStatus status ) {

}
