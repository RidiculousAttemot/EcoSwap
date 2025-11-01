# SMTP Email Configuration Guide for Supabase

## Why You Need This
Supabase's default email service:
- ❌ Limits: 2 emails per hour
- ❌ Unreliable for production
- ❌ May not send OTP codes consistently

Custom SMTP:
- ✅ Unlimited emails
- ✅ Reliable delivery
- ✅ Professional sender name

---

## Option 1: Gmail SMTP (Free & Easy)

### Step 1: Create Gmail App Password

1. Go to your Google Account: https://myaccount.google.com/
2. Click **Security** in the left sidebar
3. Enable **2-Step Verification** (required for app passwords)
4. Once enabled, search for **"App passwords"**
5. Click **App passwords**
6. Select:
   - **App**: Mail
   - **Device**: Other (custom name) → Type "Supabase EcoSwap"
7. Click **Generate**
8. **Copy the 16-character password** (you won't see it again!)

### Step 2: Configure Supabase SMTP

1. Go to: https://app.supabase.com/project/dhtrnbejkmbhsiqsoeru/settings/auth
2. Scroll down to **SMTP Settings**
3. Click **Enable Custom SMTP**
4. Fill in these values:

```
Sender email:         your-email@gmail.com
Sender name:          EcoSwap
Host:                 smtp.gmail.com
Port number:          587
Username:             your-email@gmail.com
Password:             [paste the 16-char app password]
```

5. Click **Save**
6. Click **Send Test Email** to verify it works

---

## Option 2: SendGrid (Free 100 emails/day)

### Step 1: Create SendGrid Account

1. Go to: https://signup.sendgrid.com/
2. Sign up for free account
3. Verify your email
4. Complete the setup wizard

### Step 2: Create API Key

1. In SendGrid dashboard, go to **Settings** → **API Keys**
2. Click **Create API Key**
3. Name: "Supabase EcoSwap"
4. Permission: **Full Access**
5. Click **Create & View**
6. **Copy the API key** (you won't see it again!)

### Step 3: Verify Sender Identity

1. Go to **Settings** → **Sender Authentication**
2. Click **Verify a Single Sender**
3. Fill in your email and details
4. Check your email and verify

### Step 4: Configure Supabase SMTP

1. Go to: https://app.supabase.com/project/dhtrnbejkmbhsiqsoeru/settings/auth
2. Scroll to **SMTP Settings**
3. Enable Custom SMTP:

```
Sender email:         your-verified-email@domain.com
Sender name:          EcoSwap
Host:                 smtp.sendgrid.net
Port number:          587
Username:             apikey
Password:             [paste your SendGrid API key]
```

4. Click **Save**

---

## Option 3: Resend (Modern, Developer-Friendly)

### Step 1: Create Resend Account

1. Go to: https://resend.com/signup
2. Sign up for free (3,000 emails/month free)
3. Verify your email

### Step 2: Add Domain (or use their test domain)

For testing, you can use: `onboarding@resend.dev`

For production:
1. Go to **Domains**
2. Click **Add Domain**
3. Add your domain and verify DNS records

### Step 3: Create API Key

1. Go to **API Keys**
2. Click **Create API Key**
3. Name: "Supabase EcoSwap"
4. Permission: **Sending access**
5. **Copy the API key**

### Step 4: Get SMTP Credentials

1. Go to **SMTP**
2. You'll see:
   - Host: `smtp.resend.com`
   - Port: `587` or `465`
   - Username: `resend`
   - Password: [your API key]

### Step 5: Configure Supabase

```
Sender email:         onboarding@resend.dev (or your domain)
Sender name:          EcoSwap
Host:                 smtp.resend.com
Port number:          587
Username:             resend
Password:             [your API key]
```

---

## After Setup: Test OTP Flow

1. **Save SMTP settings** in Supabase
2. **Send test email** to verify
3. In your app, **register with a new email**
4. Check email - should receive **6-digit OTP code** within seconds
5. Enter code in app to verify

---

## Email Template Customization

After SMTP is working, customize your OTP email:

1. Go to: https://app.supabase.com/project/dhtrnbejkmbhsiqsoeru/auth/templates
2. Find **"Confirmation"** or **"Magic Link"** template
3. Edit to show the OTP code:

```html
<h2>Your EcoSwap Verification Code</h2>
<p>Hi there!</p>
<p>Enter this code to verify your account:</p>
<h1 style="font-size: 32px; letter-spacing: 5px;">{{ .Token }}</h1>
<p>This code expires in 60 minutes.</p>
<p>If you didn't sign up for EcoSwap, you can ignore this email.</p>
```

---

## Troubleshooting

### Emails still not sending
- Check spam/junk folder
- Verify SMTP credentials are correct
- Check SendGrid/Gmail account isn't suspended
- Try sending test email from Supabase dashboard

### Gmail "Less secure app" error
- Make sure 2-Step Verification is enabled
- Use App Password, not your regular password
- Check that IMAP is enabled in Gmail settings

### SendGrid "Sender not verified"
- Complete sender verification process
- Check verification email
- Wait a few minutes after verification

---

## Recommended for Production

**For EcoSwap production:**
- ✅ **Gmail** - Good for small apps (500 emails/day limit)
- ✅ **SendGrid** - Best for medium apps (100/day free, scales well)
- ✅ **Resend** - Best for developers (modern API, 3000/month free)

All three options are reliable and will make your OTP emails work perfectly!
