package com.fuorimondo.email;

import com.fuorimondo.users.Locale;

public interface EmailSender {
    void sendActivationCode(String to, String code, Locale locale);
    void sendWaitingListConfirmation(String to, String firstName, Locale locale);
    void sendPasswordResetLink(String to, String resetUrl, Locale locale);
}
