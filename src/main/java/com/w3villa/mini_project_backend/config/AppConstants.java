package com.w3villa.mini_project_backend.config;

public class AppConstants {


    public static final String[] AUTH_PUBLIC_URLS = {
            "/api/v1/auth/**",
            "/v3/api-docs/**",
            "/swagger-ui.html",
            "/swagger-ui/**",

            // Only specific user APIs
            "/api/v1/users",              // create user
            "/api/v1/users/*",            // get/update by id
            "/api/v1/users/*/download",
            "/api/v1/users/*/upload-image"
    };

    public static final String[] AUTH_ADMIN_URLS = {
            "/api/v1/users",              // GET all users
            "/api/v1/users/*/upgrade",
            "/api/v1/users/*"             // DELETE
    };
    public static final String[] AUTH_GUEST_URLS= {

    };

    public static final String ADMIN_ROLE = "ADMIN";
    public static final String GUEST_ROLE = "GUEST";



}
