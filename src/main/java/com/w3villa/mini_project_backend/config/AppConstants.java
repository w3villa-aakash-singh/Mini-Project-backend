package com.w3villa.mini_project_backend.config;

public class AppConstants {


    public static final String[] AUTH_PUBLIC_URLS = {
            "/api/v1/auth/login",
            "/api/v1/auth/register",
            "/api/v1/auth/verify",
            "/api/v1/auth/refresh",

            "/v3/api-docs/**",
            "/swagger-ui.html",
            "/swagger-ui/**",

            "/api/v1/users",
            "/api/v1/users/*",
            "/api/v1/users/*/download",
            "/api/v1/users/*/upload-image",

            "/api/v1/products/**",
            "/api/v1/categories/**",
            "/error"
    };

    public static final String[] AUTH_ADMIN_URLS = {
            "/api/v1/users",              // GET all users
            "/api/v1/users/*/upgrade",
            "/api/v1/users/*" ,      // DELETE
            "/api/v1/admin/products/**",
            "/api/v1/admin/categories/**"
    };
    public static final String[] AUTH_GUEST_URLS= {

    };

    public static final String ADMIN_ROLE = "ADMIN";
    public static final String GUEST_ROLE = "GUEST";



}
