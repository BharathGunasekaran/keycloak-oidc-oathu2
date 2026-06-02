<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>OTP Verification</title>
    <style>
        :root {
            --primary-color: #4f46e5;
            --primary-hover: #4338ca;
            --bg-color: #f9fafb;
            --card-bg: #ffffff;
            --text-main: #1f2937;
            --text-muted: #4b5563;
            --error-color: #dc2626;
            --success-color: #16a34a;
            --border-color: #d1d5db;
        }

        body {
            font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, Helvetica, Arial, sans-serif;
            background-color: var(--bg-color);
            color: var(--text-main);
            display: flex;
            justify-content: center;
            align-items: center;
            min-height: 100vh;
            margin: 0;
            padding: 20px;
            box-sizing: border-box;
        }

        .otp-container {
            background: var(--card-bg);
            padding: 2.5rem 2rem;
            border-radius: 12px;
            box-shadow: 0 4px 6px -1px rgba(0, 0, 0, 0.1), 0 2px 4px -1px rgba(0, 0, 0, 0.06);
            width: 100%;
            max-width: 400px;
            text-align: center;
        }

        h2 {
            margin-top: 0;
            margin-bottom: 0.5rem;
            color: var(--text-main);
            font-size: 1.5rem;
            font-weight: 700;
        }

        .subtitle {
            color: var(--text-muted);
            font-size: 0.95rem;
            margin-bottom: 1.5rem;
        }

        /* Message Boxes */
        .msg {
            padding: 0.75rem 1rem;
            border-radius: 6px;
            font-size: 0.9rem;
            margin-bottom: 1.25rem;
            text-align: left;
        }
        .msg-error {
            background-color: #fef2f2;
            color: var(--error-color);
            border: 1px solid #fca5a5;
        }
        .msg-info {
            background-color: #f0fdf4;
            color: var(--success-color);
            border: 1px solid #86efac;
        }

        /* Meta details (timers, attempts) */
        .meta-info {
            font-size: 0.87rem;
            color: var(--text-muted);
            margin-bottom: 1.5rem;
            background: #f3f4f6;
            padding: 0.75rem;
            border-radius: 8px;
            display: flex;
            flex-direction: column;
            gap: 0.35rem;
            text-align: left;
        }
        .meta-info p {
            margin: 0;
        }

        /* Form Controls */
        .form-group {
            margin-bottom: 1.25rem;
        }

        input[type="text"] {
            width: 100%;
            padding: 0.75rem 1rem;
            font-size: 1.1rem;
            letter-spacing: 0.1em;
            text-align: center;
            border: 2px solid var(--border-color);
            border-radius: 8px;
            box-sizing: border-box;
            transition: border-color 0.2s, box-shadow 0.2s;
        }

        input[type="text"]:focus {
            outline: none;
            border-color: var(--primary-color);
            box-shadow: 0 0 0 3px rgba(79, 70, 229, 0.15);
        }

        /* Button Layout */
        .btn-group {
            display: flex;
            flex-direction: column;
            gap: 0.75rem;
        }

        button {
            width: 100%;
            padding: 0.75rem;
            font-size: 1rem;
            font-weight: 600;
            border-radius: 8px;
            cursor: pointer;
            transition: background-color 0.2s, color 0.2s;
            border: none;
        }

        .btn-verify {
            background-color: var(--primary-color);
            color: white;
        }

        .btn-verify:hover {
            background-color: var(--primary-hover);
        }

        .btn-resend {
            background-color: transparent;
            color: var(--primary-color);
            border: 1px solid var(--primary-color);
        }

        .btn-resend:hover {
            background-color: rgba(79, 70, 229, 0.05);
        }
    </style>
</head>
<body>

<div class="otp-container">

    <h2>Verify Your Identity</h2>

    <#if email??>
        <p class="subtitle">An OTP was sent to <strong>${email}</strong>.</p>
    <#else>
        <p class="subtitle">Please enter the security code sent to your device.</p>
    </#if>

    <#if errorMessage??>
        <div class="msg msg-error">${errorMessage}</div>
    </#if>

    <#if infoMessage??>
        <div class="msg msg-info">${infoMessage}</div>
    </#if>

    <#if expiryCountdown?? || attemptsLeft?? || resendCooldown??>
        <div class="meta-info">
            <#if expiryCountdown??>
                <p>⏰ Expires in: <strong>${expiryCountdown}</strong></p>
            </#if>
            <#if attemptsLeft?? && maxAttempts??>
                <p>🔢 Attempts remaining: <strong>${attemptsLeft} / ${maxAttempts}</strong></p>
            </#if>
            <#if resendCooldown??>
                <p>⏳ Resend cooldown: <strong>${resendCooldown}</strong></p>
            </#if>
        </div>
    </#if>

    <form action="${url.loginAction}" method="post">
        <div class="form-group">
            <input
                type="text"
                name="otp"
                placeholder="Enter OTP"
                autocomplete="one-time-code"
                required>
        </div>

        <div class="btn-group">
            <button type="submit" name="action" value="verify" class="btn-verify">
                Verify Code
            </button>

            <button type="submit" name="action" value="resend" class="btn-resend">
                Resend OTP
            </button>
        </div>
    </form>

</div>

</body>
</html>