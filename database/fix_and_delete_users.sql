-- Delete Test Users Script
-- This script fixes the foreign key constraint and then deletes test users

-- STEP 1: Fix the profiles table foreign key constraint to allow CASCADE delete
-- Drop the existing constraint
ALTER TABLE public.profiles 
DROP CONSTRAINT IF EXISTS profiles_id_fkey;

-- Re-add the constraint with ON DELETE CASCADE
ALTER TABLE public.profiles 
ADD CONSTRAINT profiles_id_fkey 
FOREIGN KEY (id) REFERENCES auth.users(id) ON DELETE CASCADE;

-- STEP 2: Now you can safely delete users
-- Delete specific test users
DELETE FROM auth.users 
WHERE email IN ('arwindante02@gmail.com', 'jobin71302@keevle.com');

-- STEP 3: Verify deletion
SELECT id, email, created_at, email_confirmed_at 
FROM auth.users 
WHERE email IN ('arwindante02@gmail.com', 'jobin71302@keevle.com');

-- If query returns no rows, deletion was successful!

-- Optional: Check what users remain
-- SELECT email, created_at FROM auth.users ORDER BY created_at DESC;
