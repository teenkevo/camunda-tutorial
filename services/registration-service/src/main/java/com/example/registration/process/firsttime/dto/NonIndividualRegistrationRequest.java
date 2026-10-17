package com.example.registration.process.firsttime.dto;

import com.example.governance.runtime.SensitivitySupport;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import java.util.List;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class NonIndividualRegistrationRequest extends NonIndividualContactsRequest {

    @NotBlank
    @Size(min = 4, max = 12)
    private String otpCode;

    @NotEmpty
    private List<@NotBlank String> taxTypes;

    @Override
    public String toString() {
        return SensitivitySupport.sensitiveToString(this);
    }
}
