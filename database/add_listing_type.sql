-- Add listing_type column to posts table safely
ALTER TABLE public.posts 
ADD COLUMN IF NOT EXISTS listing_type TEXT DEFAULT 'swap' CHECK (listing_type IN ('swap', 'donation'));

-- Update existing posts to have a listing_type based on their category
UPDATE public.posts 
SET listing_type = 'donation' 
WHERE category = 'donation';

UPDATE public.posts 
SET listing_type = 'swap' 
WHERE category = 'swap';
