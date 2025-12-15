# Configure Supabase for OTP Email Verification

## ⚠️ IMPORTANT: Switch from Confirmation Links to OTP Codes

By default, Supabase sends **email confirmation links**. To use **6-digit OTP codes** instead, follow these steps:

## Step 1: Disable Email Confirmation Links

1. Go to your Supabase Dashboard: https://app.supabase.com/project/dhtrnbejkmbhsiqsoeru
2. Navigate to **Authentication** → **Providers**
3. Click on **Email**
4. **UNCHECK** "Enable email confirmations" (or set it to OFF)
5. Click **Save**

## Step 2: Enable Email OTP

1. Still in **Authentication** → **Providers** → **Email**
2. Look for **Email OTP** section
3. **CHECK** "Enable Email OTP" (or toggle it ON)
4. Click **Save**

## Step 3: Customize Email Template (Optional)

1. Go to **Authentication** → **Email Templates**
2. Select **Magic Link** template
3. Customize the email body to show the OTP code clearly:

```html
<h2>Your Verification Code</h2>
<p>Use this code to verify your email:</p>
<h1 style="font-size: 32px; letter-spacing: 8px;">{{ .Token }}</h1>
<p>This code will expire in 60 minutes.</p>
<p>If you didn't request this code, please ignore this email.</p>
```

4. Click **Save**

## Step 4: Test the Flow

1. **Delete existing test users** from **Authentication** → **Users**
2. **Register a new user** in your app
3. **Check your email** - you should receive a 6-digit code (not a link)
4. **Enter the code** in the verification screen
5. **Success!** You should be logged in

## Troubleshooting

### Still receiving confirmation links?
- Make sure you **disabled** "Enable email confirmations"
- Make sure you **enabled** "Enable Email OTP"
- Try clearing your browser cache and refresh Supabase Dashboard
- Wait 1-2 minutes for changes to propagate

### Not receiving emails?
- Check your spam/junk folder
- Verify your email is correct in Supabase
- Check Supabase logs: **Authentication** → **Logs**

### OTP verification failing?
- Make sure the code is entered within 60 minutes
- Code is case-sensitive (though it's usually just numbers)
- Try requesting a new code with the "Resend" button

## Current Settings Check

Run this in your browser console on Supabase Dashboard to verify:
```javascript
// This should return your auth config
await fetch('https://dhtrnbejkmbhsiqsoeru.supabase.co/auth/v1/settings', {
  headers: { 'apikey': 'YOUR_ANON_KEY' }
}).then(r => r.json())
```

Look for:
- `"enable_signup": true`
- `"enable_email_signup": true`  
- `"enable_email_otp": true` ✅ (This should be true)
- `"enable_email_confirmations": false` ✅ (This should be false)
