# How to Fix RLS Policy Error in Supabase

## Quick Guide

### Step 1: Access Supabase Dashboard
1. Go to: https://supabase.com/dashboard
2. Sign in if needed
3. Select your **EcoSwap** project

### Step 2: Open SQL Editor
1. Look at the left sidebar
2. Click on **"SQL Editor"** icon (looks like a database with code)
3. Click **"New query"**

### Step 3: Run the Fix
1. Copy this SQL code:

```sql
-- Fix for RLS Policy Recursion Error on Profiles Table
-- This removes the problematic policy and recreates it correctly

DROP POLICY IF EXISTS "Users can update their own profile" ON public.profiles;

CREATE POLICY "Users can update their own profile"
    ON public.profiles FOR UPDATE
    USING (auth.uid() = id)
    WITH CHECK (auth.uid() = id);
```

2. Paste it into the SQL editor
3. Click **"Run"** button (or press Ctrl+Enter)

### Step 4: Verify Success
You should see:
```
Success. No rows returned
```

This means the policy was updated successfully!

## What This Does

- **Removes** the old policy that was causing infinite recursion
- **Creates** a new, simpler policy that says:
  - Users can only update their own profile (where their auth ID matches the profile ID)
  - No complex checks that could cause recursion

## After Running This

1. Your profile edits will work! ✅
2. No more "infinite recursion" error ✅
3. You can update:
   - Name
   - Email
   - Bio
   - Contact number
   - Location

## Test It

After running the SQL fix:
1. Open your EcoSwap app
2. Go to Profile
3. Click the edit icon (pencil) next to your name
4. Make some changes
5. Click Save
6. Should see: "Profile updated successfully!" 🎉

---

**This is the most important step - do this before testing your app!**
