<!DOCTYPE html>
<html>
<head>
    <title>OTP Verification</title>
</head>
<body>

<h2>Custom OTP Page</h2>

<#if errorMessage??>
    <p style="color: red;">${errorMessage}</p>
</#if>

<#if infoMessage??>
    <p style="color: #0b6b0b;">${infoMessage}</p>
</#if>

<#if email??>
    <p>An OTP was sent to <strong>${email}</strong>.</p>
</#if>

<#if expiryCountdown??>
    <p>OTP expires in <strong>${expiryCountdown}</strong>.</p>
</#if>

<#if attemptsLeft?? && maxAttempts??>
    <p>${attemptsLeft} of ${maxAttempts} attempt(s) remaining.</p>
</#if>

<#if resendCooldown??>
    <p>Resend cooldown: <strong>${resendCooldown}</strong>.</p>
</#if>

<form action="${url.loginAction}" method="post">

    <input
            type="text"
            name="otp"
            placeholder="Enter OTP"
            autocomplete="one-time-code"
            required>

    <button type="submit" name="action" value="verify">
        Verify
    </button>

    <button type="submit" name="action" value="resend">
        Resend OTP
    </button>

</form>

</body>
</html>