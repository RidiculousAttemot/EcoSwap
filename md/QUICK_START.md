# 🚀 EcoSwap Quick Start Guide

## ✅ COMPLETED
- [x] Supabase credentials configured in `local.properties`
- [x] 25 Java classes created
- [x] 15 layout XML files created
- [x] Build configuration updated
- [x] All dependencies added
- [x] Enhanced database schema ready

---

## 📝 STEP 1: Set Up Database in Supabase (5 minutes)

### 1.1 Open Supabase Dashboard
**Direct Link:** https://app.supabase.com/project/dhtrnbejkmbhsiqsoeru

**OR Navigate Manually:**
1. Go to https://supabase.com/dashboard
2. Click on your **"EcoSwap"** project (or the project you created)
3. You should see your project dashboard

### 1.2 Open SQL Editor
**In Supabase Dashboard:**
1. Look at the **left sidebar** (dark sidebar on the left)
2. Click the **"SQL Editor"** icon (📝 looks like a page with code)
3. Click the **"+ New query"** button (top right, green button)
4. You'll see an empty SQL editor

### 1.3 Run the Complete Schema
**In VS Code / Your Editor:**
1. Open file: **`database/schema.sql`** (in your project folder)
2. Press **Ctrl+A** (Windows) to select all content
3. Press **Ctrl+C** to copy

**Back in Supabase SQL Editor:**
1. Click in the SQL editor panel
2. Press **Ctrl+V** to paste all the schema code
3. Click the **"Run"** button (bottom right corner, or press **Ctrl+Enter**)
4. Wait 3-5 seconds for execution

**✅ Expected Success Message:**
```
Success. No rows returned
```

**🎉 What This Creates:**
- 12 tables: profiles, posts, swaps, donations, purchases, bids, comments, chats, eco_savings, transactions, admins, admin_dashboard
- Row Level Security (RLS) policies for all tables
- Automatic triggers for gamification (5 eco levels: 🌱→♻️→🌍→🦋→🌞)
- Auto profile creation on user signup
- Real-time admin dashboard statistics

---

## 📝 STEP 2: Set Up Storage for Images (3 minutes)

### 2.1 Create Storage Bucket
**In Supabase Dashboard:**
1. Look at the **left sidebar**
2. Click **"Storage"** (📦 icon, looks like a box)
3. Click the **"New bucket"** button (green button, top right)
4. A dialog will appear:
   - **Name:** Type `   `
   - **Public bucket:** Check the ✅ checkbox (important!)
   - Click **"Create bucket"**

### 2.2 Set Storage Policies (Create 3 Policies)

**Still in Storage section:**
1. Click on your newly created **`ecoswap-images`** bucket (in the list)
2. Click the **"Policies"** tab (top navigation)

You need to create 3 separate policies. For each policy:

---

#### **Policy 1:  **

**📋 Template to Use:** `Allow access to a bucket`

1. Click **"New policy"** button
2. Look for and select the template: **"Allow access to a bucket"**
   - OR click **"For full customization"** (if template not available)
3. Fill in the form:
   - **Policy name:** `Public Read Access`
   - **Allowed operation:** SELECT ✅ (check this box only)
   - **Target roles:** `public` (or leave blank for public access)
   - **Policy definition (USING expression):**
     ```sql
     bucket_id = 'ecoswap-images'
     ```
4. Click **"Review"** → **"Save policy"**

**✅ What this does:** Allows ANYONE (even not logged in) to VIEW/download images

**OR use SQL Editor (easier):**
```sql
CREATE POLICY "Public Read Access"
ON storage.objects FOR SELECT
USING (bucket_id = 'ecoswap-images');
```

---

#### **Policy 2: Authenticated Upload**

**📋 Template to Use:** `Give users access to only their own top-level folder` (Recommended)
**OR:** `Give users access to a folder only to authenticated users` (Simpler)

**Option A - User Folders (More Secure):**
1. Click **"New policy"** button again
2. Select template: **"Give users access to only their own top-level folder"**
3. Fill in the form:
   - **Policy name:** `User Upload to Own Folder`
   - **Allowed operation:** INSERT ✅ (check this box only)
   - **Target roles:** `authenticated`
   - **Policy definition (WITH CHECK expression):**
     ```sql
     bucket_id = 'ecoswap-images' AND (storage.foldername(name))[1] = auth.uid()::text
     ```
4. Click **"Review"** → **"Save policy"**

**✅ What this does:** Users can only upload to `{their_user_id}/` folder

**Option B - Simple (Less Secure):**
```sql
bucket_id = 'ecoswap-images' AND auth.role() = 'authenticated'
```
**✅ What this does:** Any logged-in user can upload anywhere

**OR use SQL Editor:**
```sql
-- Option A: User folders (recommended)
CREATE POLICY "User Upload to Own Folder"
ON storage.objects FOR INSERT
WITH CHECK (
  bucket_id = 'ecoswap-images' 
  AND (storage.foldername(name))[1] = auth.uid()::text
);

-- Option B: Simple (less secure)
CREATE POLICY "Authenticated Upload"
ON storage.objects FOR INSERT
WITH CHECK (
  bucket_id = 'ecoswap-images' 
  AND auth.role() = 'authenticated'
);
```

---

#### **Policy 3: Authenticated Delete**

**📋 Template to Use:** `Give users access to only their own top-level folder` (if using Option A)
**OR:** Use `owner` field check (if using Option B)

**Option A - User Folders (Matches Policy 2 Option A):**
1. Click **"New policy"** button one more time
2. Select template: **"Give users access to only their own top-level folder"**
3. Fill in the form:
   - **Policy name:** `User Delete Own Files`
   - **Allowed operation:** DELETE ✅ (check this box only)
   - **Target roles:** `authenticated`
   - **Policy definition (USING expression):**
     ```sql
     bucket_id = 'ecoswap-images' AND (storage.foldername(name))[1] = auth.uid()::text
     ```
4. Click **"Review"** → **"Save policy"**

**✅ What this does:** Users can only delete files in their own `{user_id}/` folder

**Option B - Owner-based (Matches Policy 2 Option B):**
```sql
bucket_id = 'ecoswap-images' AND auth.uid() = owner
```
**✅ What this does:** Users can only delete files they uploaded

**OR use SQL Editor:**
```sql
-- Option A: User folders (recommended)
CREATE POLICY "User Delete Own Files"
ON storage.objects FOR DELETE
USING (
  bucket_id = 'ecoswap-images' 
  AND (storage.foldername(name))[1] = auth.uid()::text
);

-- Option B: Owner-based (simpler)
CREATE POLICY "User Delete Own Files"
ON storage.objects FOR DELETE
USING (
  bucket_id = 'ecoswap-images' 
  AND auth.uid() = owner
);
```

---

---

## 📋 Supabase Storage Policy Templates - Complete Reference

Supabase provides these 5 built-in templates:

| # | Template Name | What It Does | Use in EcoSwap |
|---|--------------|--------------|----------------|
| 1️⃣ | **Allow access to JPG images in a public folder to anonymous users** | Public access to `.jpg` files only in a specific folder | ❌ Not needed (we want all file types) |
| 2️⃣ | **Give users access to only their own top-level folder named as uid** | Users access only `{user_id}/` folder | ✅ **USE THIS for Policy 2 & 3** |
| 3️⃣ | **Give users access to a folder only to authenticated users** | Any logged-in user can access a specific folder | ⚠️ Alternative (less secure) |
| 4️⃣ | **Give access to a nested folder called admin/assets only to a specific user** | Only specific user can access `admin/assets/` | ❌ Not needed (no admin uploads) |
| 5️⃣ | **Give access to a file to a user** | Individual file-level permissions | ❌ Not needed (too granular) |

---

## 📋 Which Templates to Use for EcoSwap

### **RECOMMENDED SETUP (Most Secure):**

| Your Policy | Supabase Template to Use | Why |
|-------------|-------------------------|-----|
| **Policy 1: Public Read** | Custom (no exact template) | Allow everyone to VIEW all images |
| **Policy 2: User Upload** | 2️⃣ **Give users access to only their own top-level folder** | Users upload to their own `{user_id}/` folder |
| **Policy 3: User Delete** | 2️⃣ **Give users access to only their own top-level folder** | Users delete only from their own `{user_id}/` folder |

### **ALTERNATIVE SETUP (Simpler, Less Secure):**

| Your Policy | Supabase Template to Use | Why |
|-------------|-------------------------|-----|
| **Policy 1: Public Read** | Custom (no exact template) | Allow everyone to VIEW all images |
| **Policy 2: User Upload** | 3️⃣ **Give users access to a folder only to authenticated users** | Any logged-in user can upload anywhere |
| **Policy 3: User Delete** | Custom (owner-based) | Users delete only files they uploaded |

---

**✅ Verify All 3 Policies Created:**
- Go to **Storage** → **`ecoswap-images`** → **"Policies"** tab
- Should see 3 policies listed:
  - ✅ Public Read Access (SELECT)
  - ✅ User Upload to Own Folder (INSERT) *or* Authenticated Upload
  - ✅ User Delete Own Files (DELETE)

**💡 Recommended Method - Use SQL Editor (Best for EcoSwap):**

Paste these 3 policies in **SQL Editor** for proper user isolation:

```sql
-- Policy 1: Allow everyone to VIEW images (for marketplace/profiles)
CREATE POLICY "Public Read Access"
ON storage.objects FOR SELECT
USING (bucket_id = 'ecoswap-images');

-- Policy 2: Allow users to UPLOAD only to their own folder (uid-based)
CREATE POLICY "User Upload to Own Folder"
ON storage.objects FOR INSERT
WITH CHECK (
  bucket_id = 'ecoswap-images' 
  AND (storage.foldername(name))[1] = auth.uid()::text
);

-- Policy 3: Allow users to DELETE only their own files
CREATE POLICY "User Delete Own Files"
ON storage.objects FOR DELETE
USING (
  bucket_id = 'ecoswap-images' 
  AND (storage.foldername(name))[1] = auth.uid()::text
);
```

**📁 What This Creates:**
- Users upload to: `ecoswap-images/{user_id}/profile.jpg`
- Everyone can VIEW all images (for marketplace)
- Users can only UPLOAD to their own `{user_id}/` folder
- Users can only DELETE from their own `{user_id}/` folder
- Prevents users from deleting others' images! 🔒

Click **"Run"** and all policies will be created! ✅

---

**Alternative: Simple Policies (Less Secure)**

If you want simpler policies without user folders:

```sql
-- Allow anyone to view images
CREATE POLICY "Public Read Access"
ON storage.objects FOR SELECT
USING (bucket_id = 'ecoswap-images');

-- Allow authenticated users to upload
CREATE POLICY "Authenticated Upload"
ON storage.objects FOR INSERT
WITH CHECK (
  bucket_id = 'ecoswap-images' 
  AND auth.role() = 'authenticated'
);

-- Allow users to delete their own uploads
CREATE POLICY "User Delete Own Files"
ON storage.objects FOR DELETE
USING (
  bucket_id = 'ecoswap-images' 
  AND auth.uid() = owner
);
```

⚠️ **Note:** This allows users to upload anywhere, but is simpler to use.

---

## 📝 STEP 3: Enable Email Authentication (1 minute)

**In Supabase Dashboard:**
1. Click **"Authentication"** in the **left sidebar** (🔐 icon, looks like a lock)
2. Click the **"Providers"** tab (second tab at top)
3. Scroll down and find **"Email"** in the provider list
4. Click on **"Email"** row to expand it
5. Toggle the switch to **ON** (it will turn green ✅)
6. Scroll down and click **"Save"** button

**What this does:**
- Allows users to register with email and password
- Enables login functionality in your app

---

## 📝 STEP 4: Set Up Android Studio & Build (5 minutes)

### 4.1 Open Project in Android Studio

**If Android Studio is NOT open:**
1. Open **Android Studio**
2. Click **"Open"** (not "New Project")
3. Navigate to: **`C:\TCU-ACTS-DOCS\EcoSwap`**
4. Click **"OK"**
5. Wait for Android Studio to index files (progress bar at bottom)

**If project is already open:**
- You're good to go! ✅

### 4.2 Sync Gradle Dependencies

**Why?** This downloads all Supabase libraries and generates BuildConfig with your credentials.

**Method 1 - Using Menu:**
1. Click **File** menu (top left)
2. Click **Sync Project with Gradle Files**
3. Wait for sync (watch status bar at bottom: "Gradle Sync" → "Gradle Sync finished")

**Method 2 - Using Toolbar:**
1. Look for the ��🔄 icon in the toolbar (elephant with refresh)
2. Click it
3. Wait for completion

**⏱️ Expected Time:** 30 seconds - 2 minutes (first time may download dependencies)

**✅ Success Signs:**
- Bottom status bar shows: "Gradle sync finished in X.XXs"
- No red errors in "Build" tab (bottom panel)
- `BuildConfig` class is generated with `SUPABASE_URL` and `SUPABASE_ANON_KEY`

### 4.3 Clean & Build Project

**Why?** Ensures a fresh build without cached errors.

**Using Terminal (Recommended):**
1. Click **View** → **Tool Windows** → **Terminal** (or press **Alt+F12**)
2. In terminal, type:
   ```powershell
   .\gradlew clean build
   ```
3. Press **Enter**
4. Wait for "BUILD SUCCESSFUL" message

**Using Menu (Alternative):**
1. Click **Build** menu
2. Click **Clean Project**
3. Wait for it to finish
4. Click **Build** → **Rebuild Project**
5. Wait for completion

**⏱️ Expected Time:** 1-3 minutes

**✅ Success Message:**
```
BUILD SUCCESSFUL in 2m 30s
45 actionable tasks: 45 executed
```

### 4.4 Run App on Device/Emulator

**Prepare Device:**

**Option A - Use Emulator (Easier):**
1. Click **Device Manager** icon in right toolbar (📱 icon)
2. If no devices exist, click **"Create Device"**
   - Choose **Pixel 5** or any phone
   - Choose **API 30** or higher
   - Click **Finish**
3. Click ▶️ **Play** button next to your virtual device
4. Wait for emulator to boot (1-2 minutes)

**Option B - Use Physical Device:**
1. Enable **Developer Options** on your Android phone
2. Enable **USB Debugging**
3. Connect phone via USB
4. Allow USB debugging when prompted on phone

**Run the App:**
1. Make sure device/emulator appears in device dropdown (top toolbar, next to app name)
2. Click the green **▶️ Run** button (top toolbar) or press **Shift+F10**
3. Select your device from list
4. Click **OK**
5. Wait for app to build, install, and launch (1-2 minutes first time)

**✅ Success Signs:**
- App icon appears on device
- App opens automatically
- You see the Login/Register screen

---

## 🧪 STEP 5: Test Your App (3 minutes)

### Test 1: User Registration

**On Your Device/Emulator:**
1. App should open to Login screen
2. Click **"Register"** or **"Sign Up"** button
3. Fill in the form:
   - **Name:** Test User
   - **Email:** test@example.com
   - **Password:** password123 (minimum 6 characters)
4. Click **"Register"** button
5. Wait for success message

**Verify in Supabase:**
1. Go back to **Supabase Dashboard** (in browser)
2. Click **"Authentication"** in left sidebar
3. Click **"Users"** tab
4. You should see **test@example.com** in the user list ✅

### Test 2: Check Auto-Created Profile

**In Supabase Dashboard:**
1. Click **"Table Editor"** in left sidebar (🗂️ icon)
2. Select **"profiles"** table from dropdown
3. You should see your test user with:
   - ✅ Name: Test User
   - ✅ Email: test@example.com
   - ✅ Eco Level: "Beginner EcoSaver"
   - ✅ Eco Icon: 🌱
   - ✅ Impact Score: 0

**This confirms the auto-profile trigger is working!**

### Test 3: Login Test

**Back in App:**
1. If not logged in already, click **"Login"**
2. Enter:
   - **Email:** test@example.com
   - **Password:** password123
3. Click **"Login"** button
4. Should navigate to **Dashboard** screen with bottom navigation ✅

### Test 4: Explore Features

**Try these tabs in the app:**
- 🏠 **Home/Dashboard** - Should show welcome message
- 💬 **Forum** - Community posts area
- 🔄 **Swap/Market** - Marketplace for items
- 👤 **Profile** - Your profile info

---

## 📊 STEP 6: Verify Complete Setup

### 6.1 Verify All Database Tables

**In Supabase Dashboard:**
1. Click **"SQL Editor"** (left sidebar)
2. Click **"+ New query"**
3. Paste this verification query:

```sql
SELECT tablename 
FROM pg_catalog.pg_tables 
WHERE schemaname = 'public' 
  AND tablename IN (
    'profiles', 'posts', 'swaps', 'donations', 
    'purchases', 'bids', 'comments', 'chats', 
    'eco_savings', 'transactions', 'admins', 'admin_dashboard'
  )
ORDER BY tablename;
```

4. Click **"Run"**

**✅ Expected Output:** 12 rows with table names:
- admin_dashboard
- admins
- bids
- chats
- comments
- donations
- eco_savings
- posts
- profiles
- purchases
- swaps
- transactions

### 6.2 Verify Storage Bucket

**In Supabase Dashboard:**
1. Click **"Storage"** (left sidebar)
2. You should see **`ecoswap-images`** bucket with "Public" badge
3. Click on it → **"Policies"** tab
4. Should show 3 policies: Read, Upload, Delete ✅

### 6.3 Test App Connection

**In Android Studio:**
1. Open **Logcat** (click **View** → **Tool Windows** → **Logcat** or press **Alt+6**)
2. Filter by: **"Supabase"** or **"EcoSwap"**
3. Run the app
4. Look for connection logs - should see successful API calls
5. No errors about "connection refused" or "invalid credentials" ✅

---

## ⚡ Quick Reference - Commands & Shortcuts

### 📂 Important File Locations
```
c:\TCU-ACTS-DOCS\EcoSwap\
├── app\src\main\java\com\example\ecoswap\    ← Java source code
├── app\src\main\res\layout\                   ← XML layouts
├── database\schema.sql                        ← Database schema
├── local.properties                           ← Your Supabase credentials
└── app\build.gradle.kts                       ← Build configuration
```

### 💻 Gradle Commands (PowerShell in Android Studio Terminal)

```powershell
# Clean build cache
.\gradlew clean

# Build entire project
.\gradlew build

# Build and install on connected device
.\gradlew installDebug

# View all available tasks
.\gradlew tasks

# Run with logs
.\gradlew installDebug --info
```

### ⌨️ Android Studio Keyboard Shortcuts

| Action | Windows Shortcut | Menu Location |
|--------|-----------------|---------------|
| **Sync Gradle** | Ctrl+Shift+O | File → Sync Project with Gradle Files |
| **Build Project** | Ctrl+F9 | Build → Make Project |
| **Run App** | Shift+F10 | Run → Run 'app' |
| **Open Terminal** | Alt+F12 | View → Tool Windows → Terminal |
| **Open Logcat** | Alt+6 | View → Tool Windows → Logcat |
| **Device Manager** | - | View → Tool Windows → Device Manager |
| **Find File** | Ctrl+Shift+N | Navigate → File |
| **Search Everywhere** | Double Shift | - |

### 🔍 Where to Find Things in Android Studio

| What | Where | How to Open |
|------|-------|------------|
| **Java files** | Project view → app/java/com/example/ecoswap | Alt+1 (Project panel) |
| **Layout XML** | Project view → app/res/layout | Alt+1 → Navigate |
| **Build errors** | Build panel (bottom) | Appears automatically on build |
| **Runtime logs** | Logcat panel (bottom) | Alt+6 |
| **Gradle console** | Build panel → Sync tab | Appears during Gradle sync |
| **Device list** | Top toolbar dropdown | Next to Run button |
| **Supabase credentials** | local.properties file | Ctrl+Shift+N → type "local" |

---

## 🔧 Troubleshooting Common Issues

### ❌ Build Error: "Cannot find BuildConfig"

**What it means:** Gradle hasn't generated the BuildConfig class with your Supabase credentials yet.

**Solution:**
1. **Clean the project:**
   - Terminal: `.\gradlew clean`
   - OR Menu: **Build** → **Clean Project**
2. **Sync Gradle:**
   - Click 🔄 icon in toolbar
   - OR **File** → **Sync Project with Gradle Files**
3. **Rebuild:**
   - Terminal: `.\gradlew build`
   - OR Menu: **Build** → **Rebuild Project**
4. If still not working: **Restart Android Studio**

**Verify it's fixed:**
- Open: `app\build\generated\source\buildConfig\debug\com\example\ecoswap\BuildConfig.java`
- Should contain: `SUPABASE_URL` and `SUPABASE_ANON_KEY`

---

### ❌ Connection Error: "Unable to connect to Supabase"

**Check these in order:**

1. **Internet Connection:**
   - Open browser, visit https://supabase.com (should load)
   
2. **Verify Credentials:**
   - Open `local.properties` file
   - Check `SUPABASE_URL` matches: `https://dhtrnbejkmbhsiqsoeru.supabase.co`
   - Check `SUPABASE_ANON_KEY` is complete (long string starting with `eyJ`)
   - No extra spaces or quotes around values
   
3. **Supabase Project Status:**
   - Go to Supabase Dashboard
   - Check project is not **PAUSED** (should be "Active")
   - If paused, click **"Restore"**
   
4. **Check Logs:**
   - Open **Logcat** in Android Studio (Alt+6)
   - Filter by: "error" or "exception"
   - Look for specific error messages

5. **Rebuild App:**
   - After fixing credentials: **Clean** → **Rebuild** → **Run**

---

### ❌ Authentication Error: "Email signup disabled"

**What it means:** Email authentication provider is not enabled in Supabase.

**Solution:**
1. Open **Supabase Dashboard**
2. Click **"Authentication"** (left sidebar, 🔐 icon)
3. Click **"Providers"** tab
4. Find **"Email"** provider
5. Toggle it **ON** (green switch)
6. Click **"Save"**
7. Try registering again in app

**Verify it worked:**
- Go back to app, try creating new account
- Should work without errors ✅

---

### ❌ Storage Error: "Bucket not found" or "Storage error"

**Solution:**
1. **Check bucket exists:**
   - Supabase Dashboard → **Storage**
   - Should see `ecoswap-images` bucket
   
2. **If bucket missing:**
   - Click **"New bucket"**
   - Name: `ecoswap-images`
   - Make it **Public** ✅
   - Click **"Create"**
   
3. **Check bucket policies:**
   - Click on `ecoswap-images` bucket
   - Click **"Policies"** tab
   - Should have 3 policies (Read, Upload, Delete)
   - If missing, add them using SQL from Step 2.2 above

---

### ❌ Database Error: "relation 'table_name' does not exist"

**What it means:** Database schema wasn't executed properly.

**Solution:**
1. Go to **Supabase Dashboard** → **SQL Editor**
2. Run this check query:
   ```sql
   SELECT count(*) 
   FROM pg_catalog.pg_tables 
   WHERE schemaname = 'public';
   ```
3. **If count < 12:** Schema didn't run completely
   - Copy entire `database/schema.sql` again
   - Paste in SQL Editor
   - Click **"Run"**
   - Wait for "Success"
   
4. **Verify all tables created:**
   - Click **"Table Editor"** (left sidebar)
   - Should see: profiles, posts, swaps, donations, etc.

---

### ❌ Gradle Sync Failed

**Common causes and fixes:**

1. **Internet connection issue:**
   - Check your internet
   - Try sync again
   
2. **Gradle daemon issue:**
   ```powershell
   .\gradlew --stop
   .\gradlew clean build
   ```
   
3. **Cache corruption:**
   - **File** → **Invalidate Caches**
   - Check "Clear file system cache"
   - Click **"Invalidate and Restart"**
   
4. **Java/SDK issue:**
   - **File** → **Project Structure**
   - Check **SDK Location** is set
   - Should show Android SDK path

---

### ❌ App Crashes on Launch

**Check Logcat for errors:**
1. Press **Alt+6** to open Logcat
2. Filter: **"AndroidRuntime"** or **"FATAL"**
3. Look for exception message

**Common fixes:**
- **NullPointerException:** Supabase client not initialized
  - Check `SupabaseClient.java` has correct credentials
  - Rebuild project
  
- **ClassNotFoundException:** Missing dependencies
  - Sync Gradle again
  - Clean and rebuild

---

### 💡 General Debugging Tips

1. **Always check Logcat first** (Alt+6 in Android Studio)
2. **Clean before rebuild** to avoid cache issues
3. **Check Supabase Dashboard logs** for server-side errors
4. **Restart Android Studio** if weird things happen
5. **Restart emulator** if device acts strange

---

## 📱 App Features Ready to Use

Once setup is complete, your app includes:

### 🔐 User Authentication
- ✅ Email/password registration
- ✅ Login with session management
- ✅ Auto-profile creation on signup
- ✅ Logout functionality

### 💬 Community Forum
- ✅ Create community posts
- ✅ View all posts with RecyclerView
- ✅ Comment on posts
- ✅ Post detail view

### 🔄 Marketplace (3 Types)
- ✅ **Swap:** Exchange items with other users
- ✅ **Donation:** Give items away for free
- ✅ **Bidding:** Auction-style item listing
- ✅ Item listing with images
- ✅ Category filtering

### 🌍 Eco Tracker
- ✅ Track environmental impact
- ✅ View CO2 saved (in kg)
- ✅ Water saved (in liters)
- ✅ Waste diverted (in kg)
- ✅ Trees equivalent calculation
- ✅ Automatic stats from transactions

### 🎮 Gamification System
- ✅ 5 Eco Levels:
  - 🌱 **Beginner EcoSaver** (0-9 points)
  - ♻️ **Rising Recycler** (10-24 points)
  - 🌍 **Sustainable Hero** (25-49 points)
  - 🦋 **Eco Guardian** (50-99 points)
  - 🌞 **Planet Pioneer** (100+ points)
- ✅ Impact score calculation:
  - Swap = 2 points
  - Donation = 3 points
  - Purchase = 1 point
- ✅ Automatic level upgrades

### 👤 User Profile
- ✅ View/edit profile info
- ✅ Upload profile picture
- ✅ View activity statistics
- ✅ See eco level and icon
- ✅ Display impact score

### 👨‍💼 Admin Features
- ✅ Admin accounts table
- ✅ Real-time dashboard statistics
- ✅ User management capability
- ✅ 3 admin roles: Admin, Super Admin, Moderator

---

## 🎯 Setup Progress Checklist

Use this to track your setup progress:

```
📋 SETUP STATUS:

✅ Step 1: Supabase credentials configured
✅ Step 2: All code files created (25 Java classes)
✅ Step 3: All layouts created (15 XML files)
✅ Step 4: Dependencies added to build.gradle.kts
✅ Step 5: Enhanced database schema ready

⬜ Step 6: Run schema in Supabase SQL Editor
⬜ Step 7: Create ecoswap-images storage bucket
⬜ Step 8: Enable Email authentication
⬜ Step 9: Sync Gradle in Android Studio
⬜ Step 10: Build and run app
⬜ Step 11: Test registration
⬜ Step 12: Verify database connection

Current Status: 5/12 Complete (42%)
```

**Once all ✅ are checked, your app is fully operational!**

---

## 📊 What Happens After Each Transaction

The database automatically:

1. **On Swap Completed:**
   - +2 impact points for both users
   - +5kg CO2 saved
   - +100L water saved
   - +2kg waste diverted
   - Updates eco level if threshold reached
   - Updates admin dashboard stats

2. **On Donation Completed:**
   - +3 impact points for donor
   - +7kg CO2 saved
   - +150L water saved
   - +3kg waste diverted
   - Updates eco level if threshold reached

3. **On Purchase:**
   - +1 impact point for buyer
   - +3kg CO2 saved
   - +50L water saved
   - +1kg waste diverted
   - Updates eco level if threshold reached

**All automatic via PostgreSQL triggers!** 🎉

---

## 📁 Project Structure Overview

```
EcoSwap/
├── app/src/main/
│   ├── java/com/example/ecoswap/
│   │   ├── MainActivity.java              ← App entry point
│   │   ├── auth/                          ← Login, Register, Profile
│   │   ├── dashboard/                     ← Main screen + fragments
│   │   ├── market/                        ← Swap, Donation, Bidding
│   │   ├── tracker/                       ← Eco impact tracking
│   │   ├── settings/                      ← App settings
│   │   ├── adapters/                      ← RecyclerView adapters
│   │   ├── models/                        ← Data models
│   │   └── utils/                         ← Supabase client, helpers
│   └── res/
│       ├── layout/                        ← 15 XML layouts
│       ├── values/                        ← Styles, colors, strings
│       └── menu/                          ← Bottom navigation
├── database/
│   ├── schema.sql                         ← Complete database schema
│   ├── QUICK_REFERENCE.md                 ← SQL query examples
│   ├── SCHEMA_IMPROVEMENTS.md             ← Schema documentation
│   └── SETUP_GUIDE.md                     ← Detailed DB setup
├── build.gradle.kts                       ← Project build config
├── app/build.gradle.kts                   ← App build config
├── local.properties                       ← Your credentials (DON'T COMMIT!)
└── QUICK_START.md                         ← This file!
```

---

## � Useful Links

- **Your Supabase Dashboard:** https://app.supabase.com/project/dhtrnbejkmbhsiqsoeru
- **Supabase Docs:** https://supabase.com/docs
- **Android Studio:** https://developer.android.com/studio
- **Material Design:** https://material.io/components

---

## 📞 Getting Help

### Check Logs First:
1. **Android Studio Logcat** (Alt+6): For app crashes and errors
2. **Supabase Dashboard → Logs**: For database/auth errors
3. **Build panel**: For compilation errors

### Common Log Filters:
- `tag:EcoSwap` - Your app logs
- `tag:Supabase` - Supabase client logs
- `level:ERROR` - Only errors
- `package:com.example.ecoswap` - Your package only

### Test Database Connection:
Run in Supabase SQL Editor:
```sql
-- Test if schema is loaded
SELECT count(*) FROM public.profiles;

-- Test RLS policies
SELECT * FROM public.posts LIMIT 5;

-- Test admin dashboard
SELECT * FROM public.admin_dashboard;
```

---

## 🎉 Congratulations!

Once you complete all 6 steps above, you'll have a fully functional EcoSwap app with:
- ✅ User authentication
- ✅ Database with gamification
- ✅ Image storage
- ✅ Automatic environmental tracking
- ✅ Real-time admin dashboard

**Next Action:** Start with **STEP 1** (Database Setup) above! 🚀

---

**Questions or issues?** Check the troubleshooting section or review the detailed guides in the `database/` folder.
