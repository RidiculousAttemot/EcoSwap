# CRITICAL FIX NEEDED - READ THIS FIRST!

## 🚨 Current Issues

Based on your logs at 16:18:39 and 16:19:32:

### 1. Profile Shows "New User" 
**Why**: Your database has `name="New User"` 
**Logs show**:
```
"name":"New User","email":"arwindante02@gmail.com"
```

### 2. Edit Profile Failing
**Error at 16:18:48**:
```
Error updating profile: Update failed: {"code":"42P17","details":null,"hint":null,"message":"infinite recursion detected in policy for relation \"admins\""}
```

### 3. UI Changes Not Visible
**Why**: You haven't rebuilt the app with the new code yet

### 4. "Coming Soon" Toast
**From log at 16:18:57**: This is the profile picture feature (not the edit name feature)

---

## ✅ SOLUTION - DO THESE IN ORDER!

### STEP 1: Fix Database Policy (MOST IMPORTANT!)

**Open Supabase Dashboard NOW:**

1. Go to: https://supabase.com/dashboard
2. Select your EcoSwap project
3. Click **"SQL Editor"** (left sidebar)
4. Click **"New query"**
5. **Copy and paste this EXACT code**:

```sql
-- Fix RLS Policy for Profile Updates
DROP POLICY IF EXISTS "Users can update their own profile" ON public.profiles;

CREATE POLICY "Users can update their own profile"
    ON public.profiles FOR UPDATE
    USING (auth.uid() = id)
    WITH CHECK (auth.uid() = id);
```

6. Click **"Run"** or press Ctrl+Enter
7. You should see: `Success. No rows returned`

**❌ WITHOUT THIS FIX, PROFILE EDITS WILL FAIL!**

---

### STEP 2: Update Your Profile Name (While in Supabase)

Since you're already in Supabase, let's also fix your "New User" name:

**In the same SQL Editor, run this:**

```sql
-- Update your profile name to your actual name
UPDATE public.profiles 
SET name = 'YOUR_ACTUAL_NAME_HERE', 
    updated_at = NOW()
WHERE email = 'arwindante02@gmail.com';

-- Verify it worked
SELECT name, email, bio, eco_level 
FROM public.profiles 
WHERE email = 'arwindante02@gmail.com';
```

**Replace `'YOUR_ACTUAL_NAME_HERE'` with your real name!**

Example:
```sql
UPDATE public.profiles 
SET name = 'Arwin Dante', 
    updated_at = NOW()
WHERE email = 'arwindante02@gmail.com';
```

---

### STEP 3: Rebuild the App with UI Changes

**Now that database is fixed, let's get the new UI:**

1. **Open Android Studio**

2. **Sync Gradle**:
   - Click the sync icon (🔄) at the top
   - Or: File > Sync Project with Gradle Files
   - Wait for "BUILD SUCCESSFUL"

3. **Clean & Rebuild**:
   - Build > Clean Project (wait to finish)
   - Build > Rebuild Project (wait to finish)

4. **Uninstall Old App** (IMPORTANT!):
   - On your phone: Settings > Apps > EcoSwap > Uninstall
   - OR use ADB: `adb uninstall com.example.ecoswap`

5. **Install Fresh Build**:
   - Click Run (▶️) button in Android Studio
   - OR: Build > Build Bundle(s) / APK(s) > Build APK(s)

---

### STEP 4: Test Everything

After installing the fresh build:

#### ✅ Check Profile Display:
- Open app and go to Profile tab
- You should now see:
  - **Your actual name** (not "New User")
  - **🌱 Beginner EcoSaver** (dynamic from database)
  - **Bio section** (hidden since yours is empty)
  - **Larger avatar** (100dp, modern styling)

#### ✅ Test Edit Profile:
1. Click the **pencil icon** next to your name
2. Fill in the form:
   - Name: Change to something else
   - Bio: "I love sustainable living!"
   - Location: "Your city"
   - Phone: Your number (optional)
3. Click **"Save"**
4. Should see: **"Profile updated successfully!"** ✅
5. Bio should now appear under your name
6. Location should show with 📍 icon

---

## 🎯 Expected Results After All Steps

### Profile Display Will Show:
```
┌─────────────────────────────────┐
│         [YOUR AVATAR]            │
│       Your Actual Name           │
│  "I love sustainable living!"    │ ← Bio (if added)
│    📍 Your City                  │ ← Location (if added)
│                                  │
│  [0 Swaps] [0 Donated] [0 Impact]│
│                                  │
│  🌱 Beginner EcoSaver            │ ← Dynamic from DB!
│  Keep swapping to level up!      │
│                                  │
│  ⭐ 4.8 Rating                   │
└─────────────────────────────────┘
```

### Edit Profile Will:
- ✅ Open dialog with current values
- ✅ Save changes successfully (no RLS error!)
- ✅ Update display immediately
- ✅ Bio shows/hides automatically
- ✅ Location shows/hides automatically

---

## 🐛 Troubleshooting

### "Still shows New User"
- ❌ You didn't run STEP 2 (update name in database)
- ❌ You didn't rebuild the app (STEP 3)
- ❌ You didn't uninstall old version before installing

### "Edit profile still fails"
- ❌ You didn't run STEP 1 (fix RLS policy)
- Verify in Supabase: Authentication > Policies > profiles table
- Should see: "Users can update their own profile" with `auth.uid() = id`

### "UI looks the same"
- ❌ You didn't rebuild the app
- ❌ You didn't uninstall old version
- Do: Clean Project > Rebuild > Uninstall > Install

### "Bio doesn't show"
- ✅ This is CORRECT if bio is empty
- Add a bio through edit profile
- It will appear automatically

---

## 📋 Quick Checklist

- [ ] Ran SQL fix in Supabase (STEP 1)
- [ ] Updated name in database (STEP 2)
- [ ] Synced Gradle in Android Studio
- [ ] Cleaned & Rebuilt project
- [ ] Uninstalled old app from phone
- [ ] Installed fresh build
- [ ] Tested profile display (shows actual name)
- [ ] Tested edit profile (saves successfully)
- [ ] Verified bio displays when added
- [ ] Verified eco level shows "🌱 Beginner EcoSaver"

---

## ⚡ TLDR (Too Long Didn't Read)

1. **Go to Supabase SQL Editor** → Run the RLS fix SQL
2. **Update your name** in database with the UPDATE SQL
3. **Android Studio** → Sync → Clean → Rebuild
4. **Phone** → Uninstall old app
5. **Android Studio** → Run app (install fresh build)
6. **Test** → Profile shows your name, edit works!

**ALL 3 ISSUES ARE FIXED IN CODE - YOU JUST NEED TO DEPLOY!**

---

**Need help? The SQL code is in:**
- `database/fix_rls_policy.sql`
- `SUPABASE_RLS_FIX_GUIDE.md`
