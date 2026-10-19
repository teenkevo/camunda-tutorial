package com.example.registration.integration.otp;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Component;

@Component
public class StubOtpService implements OtpService {

    private final Set<String> issued = ConcurrentHashMap.newKeySet();

    @Override
    public void send(String email, String telephone) {
        issued.add(key(email, telephone));
    }

    @Override
    public boolean verify(String email, String telephone, String otpCode) {
        if (otpCode == null || !issued.contains(key(email, telephone))) {
            return false;
        }
        String code = otpCode.trim();
        return "123456".equals(code) || code.length() >= 4;
    }

    private static String key(String email, String telephone) {
        return (email == null ? "" : email.trim().toLowerCase()) + "|" + (telephone == null ? "" : telephone.trim());
    }
}
