package com.sentinelcore.backend.dto;

import com.sentinelcore.backend.entity.Role;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RegisterRequest {

    @NotBlank
    @Size(max = 120)
    private String fullName;

    @NotBlank
    @Size(min = 3, max = 64)
    private String username;

    @NotBlank
    @Email
    private String email;

    @NotBlank
    @Size(min = 8, max = 128)
    private String password;

    /**
     * Ignored by the server. Kept so existing clients that send it do not fail deserialization;
     * self-registered accounts always receive {@link Role#USER}.
     */
    private Role role;

}
