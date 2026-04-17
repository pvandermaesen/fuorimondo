package com.fuorimondo.email;

import com.fuorimondo.users.Locale;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class ConsoleEmailSender implements EmailSender {

    private static final Logger log = LoggerFactory.getLogger(ConsoleEmailSender.class);

    @Override
    public void sendActivationCode(String to, String code, Locale locale) {
        log.info("\n==== EMAIL ====\nTO: {}\nSUBJECT: Fuori Marmo — Votre code d'activation\nLOCALE: {}\nBODY:\nVotre code d'activation : {}\n====", to, locale, code);
    }

    @Override
    public void sendWaitingListConfirmation(String to, String firstName, Locale locale) {
        log.info("\n==== EMAIL ====\nTO: {}\nSUBJECT: Fuori Marmo — Inscription enregistrée\nLOCALE: {}\nBODY:\nBonjour {},\nVotre inscription à la liste d'attente est enregistrée.\n====", to, locale, firstName);
    }

    @Override
    public void sendPasswordResetLink(String to, String resetUrl, Locale locale) {
        log.info("\n==== EMAIL ====\nTO: {}\nSUBJECT: Fuori Marmo — Réinitialisation du mot de passe\nLOCALE: {}\nBODY:\nLien : {}\n====", to, locale, resetUrl);
    }
}
