# 🌱 EcoSwap - Supabase Integration Guide

## 📋 What You Need to Provide

To connect your EcoSwap app to Supabase, you need **2 pieces of information** from your Supabase project:

### 1️⃣ Supabase URL
- Format: `https://xxxxxxxxxxxxx.supabase.co`
- Example: `https://abcdefghijklmno.supabase.co`

### 2️⃣ Supabase Anon Key
- Format: Long string starting with `eyJ...`
- Example: `eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...` (much longer)

### 3️⃣ Storage Bucket Names
- `SUPABASE_STORAGE_BUCKET` → Holds profile avatars (default `ecoswap-images`)
- `SUPABASE_LISTINGS_BUCKET` → Holds listing photos (default `listing-photos`)
- You can rename the buckets, but the values must match what you create in Supabase Storage

---

## 🔍 Where to Find These Credentials

### Step 1: Go to Supabase
1. Visit [https://supabase.com](https://supabase.com)
2. Log in to your account
3. Select your **EcoSwap** project (or create a new one)

### Step 2: Navigate to API Settings
1. Click on **Settings** (⚙️ icon in the sidebar)
2. Click on **API** in the settings menu

### Step 3: Copy Your Credentials
You'll see a page with these sections:

**Project URL:**
```
https://xxxxxxxxxxxxx.supabase.co
```
👆 Copy this entire URL

**Project API keys:**
- Find the **anon** / **public** key
- Click the **copy** icon next to it
```
eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6...
```
👆 Copy this entire key

---

## ✍️ How to Add Credentials to Your App

### Option 1: Edit local.properties (Recommended)
1. Open the file: `EcoSwap/local.properties`
2. Find these lines:
   ```properties
   SUPABASE_URL=your_supabase_url_here
   SUPABASE_ANON_KEY=your_supabase_anon_key_here
   ```
3. Replace with your actual credentials and bucket names:
   ```properties
   SUPABASE_URL=https://abcdefghijklmno.supabase.co
   SUPABASE_ANON_KEY=eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSI...
   SUPABASE_STORAGE_BUCKET=ecoswap-images
   SUPABASE_LISTINGS_BUCKET=listing-photos
   ```
4. Save the file

### Option 2: Tell Me Your Credentials
Just reply with:
```
URL: https://your-project.supabase.co
KEY: eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
```

And I'll update the file for you! 🚀

---

## 🗄️ Database Setup

After adding your credentials, you need to set up the database:

### Quick Setup:
1. Go to your Supabase project dashboard
2. Click **SQL Editor** in the sidebar
3. Click **New Query**
4. Open the file `database/schema.sql` in this project
5. Copy all the SQL code
6. Paste it into the Supabase SQL Editor
7. Click **Run** (or press Ctrl+Enter)
8. Wait for "Success" message

This will create all the necessary tables:
- ✅ Users/Profiles
- ✅ Items (for swap/donation/bidding)
- ✅ Posts (forum)
- ✅ Comments
- ✅ Bids
- ✅ Eco Savings tracker
- ✅ Transactions history

---

## 🪣 Storage Buckets (Avatars + Listings)

EcoSwap uses two Supabase Storage buckets:

- `ecoswap-images` (or the value in `SUPABASE_STORAGE_BUCKET`) for profile avatars
- `listing-photos` (or the value in `SUPABASE_LISTINGS_BUCKET`) for marketplace listing pictures

### Create the Listing Bucket
1. In Supabase, open **Storage → Buckets → New bucket**
2. Name it `listing-photos` (or any name you prefer—just update `local.properties`)
3. Choose **Public** if listing thumbnails should be world-readable; choose **Private** if you plan to serve signed URLs
4. Leave "File size limit" blank unless you want to cap uploads (the Android uploader compresses to <1 MB by default)

### Recommended Storage Policies (SQL)
Run these in the SQL Editor to keep ownership controls in place:

```sql
-- Authenticated users can upload to their own folder (ownerId/listingId/...)
create policy "listing photos upload"
on storage.objects for insert to authenticated
with check (
   bucket_id = 'listing-photos'
   and split_part(name, '/', 1) = auth.uid()::text
);

-- Owners can delete photos under their folder
create policy "listing photos delete"
on storage.objects for delete to authenticated
using (
   bucket_id = 'listing-photos'
   and split_part(name, '/', 1) = auth.uid()::text
);

-- Everyone can view listing photos (make this authenticated if you prefer)
create policy "listing photos read"
on storage.objects for select to public
using (bucket_id = 'listing-photos');
```

Adjust the last policy to `to authenticated` if you only want signed-in users to load listing images.

### Hooking Up the App
- Add `SUPABASE_LISTINGS_BUCKET=listing-photos` to `local.properties`
- The Android client now exposes `BuildConfig.SUPABASE_LISTINGS_BUCKET`
- Use `ListingImageUploader.upload(...)` when wiring up the create-listing flow; it automatically targets the listings bucket and nests files under `ownerId/listingId/`

---

## 🎯 What Happens Next

After you provide the credentials:

1. ✅ App will connect to your Supabase database
2. ✅ User authentication will work
3. ✅ Data will be stored securely in the cloud
4. ✅ Real-time updates enabled
5. ✅ Image uploads ready

---

## 🔒 Security Notes

- ✅ Your credentials are stored in `local.properties` which is **NOT** committed to Git
- ✅ The **anon key** is safe to use in client apps (it's public)
- ✅ Row-level security policies protect your data
- ⚠️ Never share your **service_role** key (we don't need it for this app)

---

## 📸 Example Screenshot

Your Supabase API page should look like this:

```
┌─────────────────────────────────────┐
│ Project API                         │
├─────────────────────────────────────┤
│ Project URL                         │
│ https://xxxxx.supabase.co          │
│ [Copy]                              │
├─────────────────────────────────────┤
│ API Keys                            │
│                                     │
│ anon public                         │
│ eyJhbGci...                        │
│ [Copy]                              │
│                                     │
│ service_role secret                 │
│ eyJhbGci...                        │
│ [Copy]                              │
└─────────────────────────────────────┘
```

**Copy the top two values** (Project URL and anon key)

---

## 🆘 Need Help?

If you run into any issues:

1. **Can't find Supabase credentials?**
   - Make sure you're logged into Supabase
   - Ensure your project is created
   - Check Settings → API section

2. **Build errors after adding credentials?**
   - Sync Gradle files in Android Studio
   - Clean and rebuild project
   - Check that there are no extra spaces in the values

3. **Connection errors when running app?**
   - Verify URL format is correct (starts with https://)
   - Verify key starts with "eyJ"
   - Check internet connection

---

## ✅ Quick Checklist

Before running the app, make sure:

- [ ] Created Supabase project
- [ ] Copied Project URL
- [ ] Copied anon key
- [ ] Updated `local.properties` file
- [ ] Ran `database/schema.sql` in Supabase SQL Editor
- [ ] Synced Gradle in Android Studio
- [ ] Built project successfully

---

**Ready to provide your credentials? Just paste them here!** 🎉
