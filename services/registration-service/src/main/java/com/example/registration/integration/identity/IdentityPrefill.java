package com.example.registration.integration.identity;

import java.util.Map;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class IdentityPrefill {
    String authority;
    boolean matched;
    String message;
    String displayName;
    String email;
    String telephone;
    Map<String, Object> identity;
}
