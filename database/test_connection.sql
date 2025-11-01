-- Quick Test Query for Supabase
-- Run this AFTER running the main schema.sql
-- This will verify all tables were created successfully

-- Check if all tables exist
SELECT 
    tablename 
FROM 
    pg_catalog.pg_tables 
WHERE 
    schemaname = 'public' 
    AND tablename IN ('profiles', 'items', 'posts', 'comments', 'bids', 'eco_savings', 'transactions')
ORDER BY 
    tablename;

-- Expected output: 7 rows showing all table names
-- If you see all 7 tables, the setup was successful!

-- Check if policies are enabled
SELECT 
    tablename, 
    policyname 
FROM 
    pg_policies 
WHERE 
    schemaname = 'public'
ORDER BY 
    tablename, policyname;

-- This should show multiple policies for each table

-- Test inserting a sample item category
-- (This will be used for testing once you have users)
-- Don't run this yet - run it after creating a user through the app
