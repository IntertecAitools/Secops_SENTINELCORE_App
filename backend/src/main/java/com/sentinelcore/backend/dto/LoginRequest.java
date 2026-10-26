package com.sentinelcore.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LoginRequest {

    @NotBlank
    @Size(max = 255)
    private String username;

    @NotBlank
    @Size(max = 128)
    private String password;

}
