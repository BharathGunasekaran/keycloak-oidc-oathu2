package com.learning.outh2_demo.auth;

import java.security.SecureRandom;
import java.time.Clock;
import java.time.Instant;
import java.util.Objects;

import jakarta.ws.rs.core.Response;
import org.keycloak.email.EmailException;
import org.keycloak.email.EmailSenderProvider;
import org.keycloak.authentication.AuthenticationFlowError;
import org.keycloak.authentication.AuthenticationFlowContext;
import org.keycloak.authentication.Authenticator;
import org.keycloak.models.*;

public class CustomAuthenticator implements Authenticator {

    private static final String OTP_NOTE = "otp";
    private static final String OTP_EXPIRY_NOTE = "otp-expiry";
    private static final String OTP_ATTEMPTS_NOTE = "otp_attempts";
    private static final String OTP_LAST_SENT_NOTE = "otp_last_sent";
    private static final String ACTION_RESEND = "resend";
    private static final int OTP_LENGTH = 6;
    private static final long OTP_TTL_SECONDS = 300;
    private static final int MAX_ATTEMPTS = 3;
    private static final long RESEND_COOLDOWN_SECONDS = 30;
    private static final SecureRandom RANDOM = new SecureRandom();
    private static final Clock CLOCK = Clock.systemUTC();

    @Override
    public void authenticate(AuthenticationFlowContext context) {
        UserModel user = context.getUser();
        if (user == null || user.getEmail() == null || user.getEmail().isBlank()) {
            Response challenge = context.form()
                    .setAttribute("errorMessage", "An email address is required for OTP verification.")
                    .createForm("otp.ftl");
            context.failureChallenge(AuthenticationFlowError.INVALID_USER, challenge);
            return;
        }

        try {
            issueOtp(context, user, true);
            context.getEvent().detail("otp", "sent");
            context.challenge(buildChallenge(context, null, null));
        } catch (EmailException e) {
            clearOtpNotes(context);
            Response challenge = buildChallenge(context, "Unable to send the OTP email. Please try again later.", null);
            context.failureChallenge(AuthenticationFlowError.INTERNAL_ERROR, challenge);
        }
    }

    @Override
    public void action(AuthenticationFlowContext context) {
        String action = context.getHttpRequest()
                .getDecodedFormParameters()
                .getFirst("action");

        if (ACTION_RESEND.equals(action)) {
            handleResend(context);
            return;
        }

        String otp = context.getHttpRequest()
                .getDecodedFormParameters()
                .getFirst("otp");

        String storedOtp = context.getAuthenticationSession().getAuthNote(OTP_NOTE);
        String storedExpiry = context.getAuthenticationSession().getAuthNote(OTP_EXPIRY_NOTE);
        int attempts = getAttemptCount(context);

        if (storedOtp == null || storedExpiry == null || isExpired(storedExpiry)) {
            context.getEvent().detail("otp", "expired");
            Response challenge = buildChallenge(context, "Your OTP has expired. Please request a new one.", null);
            context.failureChallenge(AuthenticationFlowError.EXPIRED_CODE, challenge);
            return;
        }

        if (otp == null || !Objects.equals(storedOtp, otp.trim())) {
            int newAttempts = attempts + 1;
            context.getAuthenticationSession().setAuthNote(OTP_ATTEMPTS_NOTE, Integer.toString(newAttempts));

            if (newAttempts >= MAX_ATTEMPTS) {
                context.getEvent().detail("otp", "max_attempts_reached");
                clearOtpNotes(context);
                context.failure(AuthenticationFlowError.INVALID_CREDENTIALS);
                return;
            }

            context.getEvent().detail("otp", "failed");
            Response challenge = buildChallenge(
                    context,
                    "Invalid OTP. You have " + (MAX_ATTEMPTS - newAttempts) + " attempt(s) left.",
                    null);
            context.failureChallenge(AuthenticationFlowError.INVALID_CREDENTIALS, challenge);
            return;
        }

        context.getEvent().detail("otp", "validated");
        clearOtpNotes(context);
        context.success();
    }

    @Override
    public boolean requiresUser() {
        return true;
    }

    @Override
    public boolean configuredFor(
            KeycloakSession session,
            RealmModel realm,
            UserModel user) {
        return true;
    }

    @Override
    public void setRequiredActions(
            KeycloakSession session,
            RealmModel realm,
            UserModel user) {
    }

    @Override
    public void close() {
    }

    private static String generateOtp() {
        int lowerBound = (int) Math.pow(10, OTP_LENGTH - 1);
        int upperBound = (int) Math.pow(10, OTP_LENGTH) - 1;
        return Integer.toString(RANDOM.nextInt(upperBound - lowerBound + 1) + lowerBound);
    }

    private static boolean isExpired(String storedExpiry) {
        long expiryEpochSeconds = Long.parseLong(storedExpiry);
        return Instant.now(CLOCK).getEpochSecond() > expiryEpochSeconds;
    }

    private void handleResend(AuthenticationFlowContext context) {
        String lastSent = context.getAuthenticationSession().getAuthNote(OTP_LAST_SENT_NOTE);
        if (lastSent != null && !canResend(lastSent)) {
            long remainingSeconds = getResendCooldownRemainingSeconds(lastSent);
            context.getEvent().detail("otp", "resend_rate_limited");
            Response challenge = buildChallenge(
                    context,
                    "Please wait before requesting another OTP.",
                    "You can request a new OTP in " + formatSeconds(remainingSeconds) + ".");
            context.failureChallenge(AuthenticationFlowError.EXPIRED_CODE, challenge);
            return;
        }

        UserModel user = context.getUser();
        if (user == null) {
            Response challenge = buildChallenge(context, "Unable to resend OTP for the current user.", null);
            context.failureChallenge(AuthenticationFlowError.INVALID_USER, challenge);
            return;
        }

        try {
            issueOtp(context, user, false);
            context.getEvent().detail("otp", "resend");
            Response challenge = buildChallenge(context, null, "A new OTP has been sent to your email.");
            context.challenge(challenge);
        } catch (EmailException e) {
            Response challenge = buildChallenge(context, "Unable to send the OTP email. Please try again later.", null);
            context.failureChallenge(AuthenticationFlowError.INTERNAL_ERROR, challenge);
        }
    }

    private void issueOtp(AuthenticationFlowContext context, UserModel user, boolean resetAttempts) throws EmailException {
        String otp = generateOtp();
        long nowEpochSeconds = Instant.now(CLOCK).getEpochSecond();
        long expiryEpochSeconds = nowEpochSeconds + OTP_TTL_SECONDS;

        context.getAuthenticationSession().setAuthNote(OTP_NOTE, otp);
        context.getAuthenticationSession().setAuthNote(OTP_EXPIRY_NOTE, Long.toString(expiryEpochSeconds));
        context.getAuthenticationSession().setAuthNote(OTP_LAST_SENT_NOTE, Long.toString(nowEpochSeconds));

        if (resetAttempts) {
            context.getAuthenticationSession().setAuthNote(OTP_ATTEMPTS_NOTE, "0");
        } else if (context.getAuthenticationSession().getAuthNote(OTP_ATTEMPTS_NOTE) == null) {
            context.getAuthenticationSession().setAuthNote(OTP_ATTEMPTS_NOTE, "0");
        }

        sendOtpEmail(context, user, otp);
    }

    private Response buildChallenge(AuthenticationFlowContext context, String errorMessage, String infoMessage) {
        String storedExpiry = context.getAuthenticationSession().getAuthNote(OTP_EXPIRY_NOTE);
        String storedAttempts = context.getAuthenticationSession().getAuthNote(OTP_ATTEMPTS_NOTE);
        String storedLastSent = context.getAuthenticationSession().getAuthNote(OTP_LAST_SENT_NOTE);

        org.keycloak.forms.login.LoginFormsProvider form = context.form()
                .setAttribute("email", maskEmail(context.getUser().getEmail()))
                .setAttribute("ttlMinutes", OTP_TTL_SECONDS / 60)
                .setAttribute("expiryCountdown", formatRemainingTime(storedExpiry))
                .setAttribute("attemptsLeft", Math.max(0, MAX_ATTEMPTS - parseInt(storedAttempts, 0)))
                .setAttribute("maxAttempts", MAX_ATTEMPTS)
                .setAttribute("resendCooldown", formatSeconds(getResendCooldownRemainingSeconds(storedLastSent)));

        if (errorMessage != null) {
            form.setAttribute("errorMessage", errorMessage);
        }

        if (infoMessage != null) {
            form.setAttribute("infoMessage", infoMessage);
        }

        return form.createForm("otp.ftl");
    }

    private int getAttemptCount(AuthenticationFlowContext context) {
        return parseInt(context.getAuthenticationSession().getAuthNote(OTP_ATTEMPTS_NOTE), 0);
    }

    private void clearOtpNotes(AuthenticationFlowContext context) {
        context.getAuthenticationSession().removeAuthNote(OTP_NOTE);
        context.getAuthenticationSession().removeAuthNote(OTP_EXPIRY_NOTE);
        context.getAuthenticationSession().removeAuthNote(OTP_ATTEMPTS_NOTE);
        context.getAuthenticationSession().removeAuthNote(OTP_LAST_SENT_NOTE);
    }

    private boolean canResend(String lastSent) {
        return getResendCooldownRemainingSeconds(lastSent) <= 0;
    }

    private long getResendCooldownRemainingSeconds(String lastSent) {
        long lastSentEpochSeconds = Long.parseLong(lastSent);
        long elapsedSeconds = Instant.now(CLOCK).getEpochSecond() - lastSentEpochSeconds;
        return Math.max(0, RESEND_COOLDOWN_SECONDS - elapsedSeconds);
    }

    private static int parseInt(String value, int defaultValue) {
        if (value == null || value.isBlank()) {
            return defaultValue;
        }

        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException ignored) {
            return defaultValue;
        }
    }

    private static String formatRemainingTime(String storedExpiry) {
        if (storedExpiry == null || storedExpiry.isBlank()) {
            return "00:00";
        }

        long expiryEpochSeconds = Long.parseLong(storedExpiry);
        long remainingSeconds = Math.max(0, expiryEpochSeconds - Instant.now(CLOCK).getEpochSecond());
        return formatSeconds(remainingSeconds);
    }

    private static String formatSeconds(long totalSeconds) {
        long minutes = totalSeconds / 60;
        long seconds = totalSeconds % 60;
        return String.format("%02d:%02d", minutes, seconds);
    }

    private static void sendOtpEmail(AuthenticationFlowContext context, UserModel user, String otp)
            throws EmailException {
        String subject = "Your login OTP";
        String textBody = "Your one-time passcode is " + otp + ". It expires in " + (OTP_TTL_SECONDS / 60) + " minutes.";
        String htmlBody = "<p>Your one-time passcode is <strong>" + otp + "</strong>.</p>"
                + "<p>It expires in " + (OTP_TTL_SECONDS / 60) + " minutes.</p>";

        EmailSenderProvider emailSenderProvider = context.getSession().getProvider(EmailSenderProvider.class);
        emailSenderProvider.send(context.getRealm().getSmtpConfig(), user, subject, textBody, htmlBody);
    }

    private static String maskEmail(String email) {
        int atIndex = email.indexOf('@');
        if (atIndex <= 1) {
            return "***" + email.substring(Math.max(atIndex, 0));
        }

        return email.charAt(0) + "***" + email.substring(atIndex);
    }
}