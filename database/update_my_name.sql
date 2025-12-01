-- Quick Fix: Update Your Profile Name
-- Run this in Supabase SQL Editor after fixing the RLS policy

-- Step 1: Update your name (REPLACE 'Your Name Here' with your actual name!)
UPDATE public.profiles 
SET 
    name = 'Your Name Here',  -- ← CHANGE THIS to your actual name
    updated_at = NOW()
WHERE email = 'arwindante02@gmail.com';

-- Step 2: Verify the update worked
SELECT 
    name, 
    email, 
    bio, 
    eco_level, 
    eco_icon,
    total_swaps,
    total_donations,
    impact_score,
    updated_at
FROM public.profiles 
WHERE email = 'arwindante02@gmail.com';

-- You should see your new name in the results!

-- EXAMPLE:
-- UPDATE public.profiles 
-- SET 
--     name = 'Arwin Dante',
--     updated_at = NOW()
-- WHERE email = 'arwindante02@gmail.com';
