package com.w3villa.mini_project_backend.helpers;

import java.util.UUID;

public class UserHelper {

    // Private constructor prevents anyone from "newing up" this utility class
    private UserHelper() {}

    public static UUID parseUUID(String uuid) {
        try {
            return UUID.fromString(uuid);
        } catch (IllegalArgumentException e) {
            // It's safer to catch this so your app doesn't crash
            // if a user sends a "bad" ID string
            throw new IllegalArgumentException("Invalid UUID format: " + uuid);
        }
    }
}