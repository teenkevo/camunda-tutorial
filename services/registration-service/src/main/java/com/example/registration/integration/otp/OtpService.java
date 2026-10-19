package com.example.registration.integration.otp;

public interface OtpService {

    /** Issues an OTP to the email and telephone that will be used for this application. */
    void send(String email, String telephone);

    /**
     * Verifies an OTP previously sent to these contacts. Stub accepts {@code 123456}
     * or any code of length &gt;= 4.
     */
    boolean verify(String email, String telephone, String otpCode);
}
