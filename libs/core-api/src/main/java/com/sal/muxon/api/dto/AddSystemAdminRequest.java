package com.sal.muxon.api.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for adding a new system admin user.
 * Used when bootstrap has already been performed and additional system admin users need to be added.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AddSystemAdminRequest {

    /**
     * Username for the new system admin user.
     * Must be unique and between 3 and 50 characters.
     */
    @NotBlank(message = "Username is required")
    @Size(min = 3, max = 50, message = "Username must be between 3 and 50 characters")
    private String username;

    /**
     * Email for the new system admin user.
     * Must be a valid email format.
     */
    @NotBlank(message = "Email is required")
    @Email(message = "Email must be valid")
    private String email;

    /**
     * Password for the new system admin user.
     * Must be at least 8 characters long.
     */
    @NotBlank(message = "Password is required")
    @Size(min = 8, message = "Password must be at least 8 characters")
    private String password;

    /**
     * First name of the new system admin user.
     */
    @NotBlank(message = "First name is required")
    private String firstName;

    /**
     * Last name of the new system admin user.
     */
    @NotBlank(message = "Last name is required")
    private String lastName;
}
