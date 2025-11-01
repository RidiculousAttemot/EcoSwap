# EcoSwap Supabase Setup Guide

## 📝 Step-by-Step Setup Instructions

### 1. Create Supabase Project
1. Go to [https://supabase.com](https://supabase.com)
2. Sign up or log in
3. Click **"New Project"**
4. Fill in:
   - **Project Name**: EcoSwap
   - **Database Password**: (Choose a strong password and save it)
   - **Region**: (Select closest to your location)
5. Click **"Create new project"** and wait for setup to complete

### 2. Get Your Credentials
1. In your Supabase project, go to **Settings** → **API**
2. Copy these values:
   - **Project URL** (e.g., `https://xxxxxxxxxxxxx.supabase.co`)
   - **anon/public key** (starts with `eyJ...`)

### 3. Add Credentials to Your App
1. Open `local.properties` file in your EcoSwap project root
2. Replace the placeholder values:
   ```properties
   SUPABASE_URL=https://your-project.supabase.co
   SUPABASE_ANON_KEY=your_actual_anon_key_here
   ```

### 4. Set Up Database Schema
1. In Supabase dashboard, go to **SQL Editor**
2. Click **"New Query"**
3. Copy the entire content from `database/schema.sql`
4. Paste it into the SQL Editor
5. Click **"Run"** or press `Ctrl+Enter`
6. Wait for all tables to be created

### 5. Enable Storage (For Images)
1. Go to **Storage** in Supabase dashboard
2. Create a new bucket named: `ecoswap-images`
3. Set it to **Public** (so users can view images)
4. In the bucket policies, add:
   ```sql
   -- Allow public read access
   CREATE POLICY "Public Access"
   ON storage.objects FOR SELECT
   USING (bucket_id = 'ecoswap-images');
   
   -- Allow authenticated users to upload
   CREATE POLICY "Authenticated users can upload"
   ON storage.objects FOR INSERT
   WITH CHECK (bucket_id = 'ecoswap-images' AND auth.role() = 'authenticated');
   ```

### 6. Enable Authentication Methods
1. Go to **Authentication** → **Providers**
2. Enable **Email** provider
3. (Optional) Enable other providers like Google, GitHub if needed
4. Configure email templates in **Authentication** → **Email Templates**

### 7. Test Connection
1. Sync your Gradle files in Android Studio
2. Build the project
3. Check for any errors in the Build Output

---

## 📊 Database Tables Created

Your database will have these tables:

1. **profiles** - User profiles (name, bio, profile image)
2. **items** - Items for swap, donation, or bidding
3. **posts** - Forum posts
4. **comments** - Comments on posts
5. **bids** - Bids on items
6. **eco_savings** - Environmental impact tracking per user
7. **transactions** - History of swaps and donations

---

## 🔐 Security Features

- **Row Level Security (RLS)** enabled on all tables
- Users can only modify their own data
- Public data is viewable by everyone
- Authenticated actions require valid user session

---

## 🧪 Test Data (Optional)

You can add some test data in SQL Editor:

```sql
-- After creating a test user through the app, you can add test items:
INSERT INTO public.items (name, description, category, owner_id, type, status)
VALUES 
  ('Vintage Books', 'Collection of classic novels', 'Books', 'your-user-id-here', 'swap', 'available'),
  ('Bicycle', 'Mountain bike in good condition', 'Sports', 'your-user-id-here', 'donation', 'available');
```

---

## 🔄 Next Steps After Setup

1. **Update local.properties** with your credentials
2. **Run the schema.sql** in Supabase
3. **Sync Gradle** in Android Studio
4. **Build and run** the app
5. **Test registration** and login

---

## 🆘 Troubleshooting

### Build Config errors
- Make sure you've added the credentials to `local.properties`
- Sync Gradle files
- Clean and rebuild project

### Connection errors
- Verify your Supabase URL and key are correct
- Check internet connection
- Ensure Supabase project is not paused

### Permission errors
- Verify RLS policies were created correctly
- Check user is authenticated before operations
- Review Supabase logs in dashboard

---

## 📚 Useful Supabase Features

- **Realtime**: Subscribe to database changes
- **Storage**: File/image uploads
- **Edge Functions**: Server-side logic
- **Auth**: Built-in authentication system

---

## 🔗 Resources

- [Supabase Documentation](https://supabase.com/docs)
- [Supabase Android Guide](https://github.com/supabase-community/supabase-kt)
- [Row Level Security Guide](https://supabase.com/docs/guides/auth/row-level-security)
