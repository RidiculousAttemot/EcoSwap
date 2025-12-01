-- Fix for RLS Policy Recursion Error on Profiles Table
-- Execute this in Supabase SQL Editor

-- Drop the problematic policies that reference admins table
DROP POLICY IF EXISTS "Users can update their own profile" ON public.profiles;

-- Recreate the policy with simple check (no recursion)
CREATE POLICY "Users can update their own profile"
    ON public.profiles FOR UPDATE
    USING (auth.uid() = id)
    WITH CHECK (auth.uid() = id);

-- Verify the policy is working
-- Test by running: SELECT * FROM public.profiles WHERE id = auth.uid();
