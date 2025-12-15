-- EcoSwap Enhanced Database Schema for Supabase
-- Execute these SQL commands in your Supabase SQL Editor

-- Enable UUID extension
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- ============================================
-- USERS/PROFILES TABLE (Enhanced)
-- ============================================
-- Extends Supabase auth.users with additional fields
CREATE TABLE public.profiles (
    id UUID REFERENCES auth.users(id) PRIMARY KEY,
    name TEXT NOT NULL,
    email TEXT UNIQUE NOT NULL,
    bio TEXT,
    profile_image_url TEXT,
    contact_number TEXT,
    location TEXT,
    
    -- EcoSwap Statistics
    total_swaps INTEGER DEFAULT 0,
    total_donations INTEGER DEFAULT 0,
    total_purchases INTEGER DEFAULT 0,
    impact_score INTEGER DEFAULT 0,
    
    -- Gamification
    eco_level TEXT DEFAULT 'Beginner EcoSaver',
    eco_icon TEXT DEFAULT '🌱',
    
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

-- Enable Row Level Security
ALTER TABLE public.profiles ENABLE ROW LEVEL SECURITY;

-- Profiles policies
CREATE POLICY "Public profiles are viewable by everyone"
    ON public.profiles FOR SELECT
    USING (true);

CREATE POLICY "Users can insert their own profile"
    ON public.profiles FOR INSERT
    WITH CHECK (auth.uid() = id);

CREATE POLICY "Users can update their own profile"
    ON public.profiles FOR UPDATE
    USING (auth.uid() = id);

-- ============================================
-- POSTS/ITEMS TABLE (Enhanced)
-- ============================================
-- Combined posts and items into a single versatile table
CREATE TABLE public.posts (
    id UUID DEFAULT uuid_generate_v4() PRIMARY KEY,
    user_id UUID REFERENCES public.profiles(id) ON DELETE CASCADE,
    
    -- Content fields
    title TEXT NOT NULL,
    description TEXT,
    
    -- Classification
    category TEXT NOT NULL CHECK (category IN ('swap', 'donation', 'bidding', 'community', 'electronics', 'clothing', 'furniture', 'books', 'sports', 'other')),
    listing_type TEXT DEFAULT 'swap' CHECK (listing_type IN ('swap', 'donation')),
    condition TEXT CHECK (condition IN ('new', 'like_new', 'good', 'fair', 'poor')),
    
    -- Location & Media
    location TEXT,
    latitude DOUBLE PRECISION,
    longitude DOUBLE PRECISION,
    image_url TEXT,
    
    -- Status tracking
    status TEXT DEFAULT 'available' CHECK (status IN ('available', 'pending', 'completed', 'cancelled', 'swapped', 'donated')),
    
    -- Bidding specific fields
    current_bid DECIMAL(10, 2) DEFAULT 0,
    starting_bid DECIMAL(10, 2) DEFAULT 0,
    bid_end_date TIMESTAMP WITH TIME ZONE,
    
    -- Engagement metrics
    views INTEGER DEFAULT 0,
    likes INTEGER DEFAULT 0,
    
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

-- Enable RLS for posts
ALTER TABLE public.posts ENABLE ROW LEVEL SECURITY;

-- Posts policies
CREATE POLICY "Posts are viewable by everyone"
    ON public.posts FOR SELECT
    USING (true);

CREATE POLICY "Authenticated users can create posts"
    ON public.posts FOR INSERT
    WITH CHECK (auth.uid() = user_id);

CREATE POLICY "Users can update their own posts"
    ON public.posts FOR UPDATE
    USING (auth.uid() = user_id);

CREATE POLICY "Users can delete their own posts"
    ON public.posts FOR DELETE
    USING (auth.uid() = user_id);

-- ============================================
-- SWAPS TABLE (New)
-- ============================================
CREATE TABLE public.swaps (
    id UUID DEFAULT uuid_generate_v4() PRIMARY KEY,
    user1_id UUID REFERENCES public.profiles(id) ON DELETE CASCADE,
    user2_id UUID REFERENCES public.profiles(id) ON DELETE CASCADE,
    post1_id UUID REFERENCES public.posts(id) ON DELETE SET NULL,
    post2_id UUID REFERENCES public.posts(id) ON DELETE SET NULL,
    status TEXT DEFAULT 'pending' CHECK (status IN ('pending', 'accepted', 'rejected', 'completed', 'cancelled')),
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    completed_at TIMESTAMP WITH TIME ZONE,
    proof_photo_url TEXT
);

-- Enable RLS for swaps
ALTER TABLE public.swaps ENABLE ROW LEVEL SECURITY;

CREATE POLICY "Users can view their own swaps"
    ON public.swaps FOR SELECT
    USING (auth.uid() = user1_id OR auth.uid() = user2_id);

CREATE POLICY "Authenticated users can create swaps"
    ON public.swaps FOR INSERT
    WITH CHECK (auth.uid() = user1_id);

CREATE POLICY "Users can update their own swaps"
    ON public.swaps FOR UPDATE
    USING (auth.uid() = user1_id OR auth.uid() = user2_id);

-- ============================================
-- DONATIONS TABLE (New)
-- ============================================
CREATE TABLE public.donations (
    id UUID DEFAULT uuid_generate_v4() PRIMARY KEY,
    donor_id UUID REFERENCES public.profiles(id) ON DELETE CASCADE,
    receiver_id UUID REFERENCES public.profiles(id) ON DELETE SET NULL,
    post_id UUID REFERENCES public.posts(id) ON DELETE SET NULL,
    receiver_name TEXT,
    receiver_contact TEXT,
    pickup_location TEXT,
    status TEXT DEFAULT 'pending' CHECK (status IN ('pending', 'accepted', 'completed', 'cancelled')),
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    completed_at TIMESTAMP WITH TIME ZONE,
    proof_photo_url TEXT
);

ALTER TABLE public.donations ENABLE ROW LEVEL SECURITY;

CREATE POLICY "Users can view donations they're involved in"
    ON public.donations FOR SELECT
    USING (auth.uid() = donor_id);

-- Allow the listing owner (post.user_id) to view related donations
CREATE POLICY "Listing owner can view donations"
    ON public.donations FOR SELECT
    USING (
        EXISTS (
            SELECT 1 FROM public.posts p
            WHERE p.id = post_id AND p.user_id = auth.uid()
        )
    );

-- Allow the receiver to view donations assigned to them
CREATE POLICY "Receiver can view donations"
    ON public.donations FOR SELECT
    USING (auth.uid() = receiver_id);

CREATE POLICY "Authenticated users can create donations"
    ON public.donations FOR INSERT
    WITH CHECK (auth.uid() = donor_id);

CREATE POLICY "Users can update their own donations"
    ON public.donations FOR UPDATE
    USING (auth.uid() = donor_id);

-- ============================================
-- PURCHASES/BIDS TABLE (Enhanced)
-- ============================================
CREATE TABLE public.purchases (
    id UUID DEFAULT uuid_generate_v4() PRIMARY KEY,
    buyer_id UUID REFERENCES public.profiles(id) ON DELETE CASCADE,
    post_id UUID REFERENCES public.posts(id) ON DELETE SET NULL,
    amount DECIMAL(10, 2) NOT NULL,
    payment_type TEXT CHECK (payment_type IN ('money', 'item', 'bid')),
    payment_status TEXT DEFAULT 'pending' CHECK (payment_status IN ('pending', 'completed', 'failed', 'refunded')),
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

ALTER TABLE public.purchases ENABLE ROW LEVEL SECURITY;

CREATE POLICY "Users can view their own purchases"
    ON public.purchases FOR SELECT
    USING (auth.uid() = buyer_id);

CREATE POLICY "Authenticated users can create purchases"
    ON public.purchases FOR INSERT
    WITH CHECK (auth.uid() = buyer_id);

-- ============================================
-- COMMENTS TABLE (Enhanced)
-- ============================================
CREATE TABLE public.comments (
    id UUID DEFAULT uuid_generate_v4() PRIMARY KEY,
    post_id UUID REFERENCES public.posts(id) ON DELETE CASCADE,
    author_id UUID REFERENCES public.profiles(id) ON DELETE CASCADE,
    content TEXT NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

ALTER TABLE public.comments ENABLE ROW LEVEL SECURITY;

-- Comments policies
CREATE POLICY "Comments are viewable by everyone"
    ON public.comments FOR SELECT
    USING (true);

CREATE POLICY "Authenticated users can create comments"
    ON public.comments FOR INSERT
    WITH CHECK (auth.uid() = author_id);

CREATE POLICY "Users can delete their own comments"
    ON public.comments FOR DELETE
    USING (auth.uid() = author_id);

-- ============================================
-- NOTIFICATIONS TABLE (New)
-- ============================================
CREATE TABLE public.notifications (
    id UUID DEFAULT uuid_generate_v4() PRIMARY KEY,
    user_id UUID REFERENCES public.profiles(id) ON DELETE CASCADE, -- recipient
    actor_id UUID REFERENCES public.profiles(id) ON DELETE SET NULL,
    post_id UUID REFERENCES public.posts(id) ON DELETE SET NULL,
    comment_id UUID REFERENCES public.comments(id) ON DELETE SET NULL,
    type TEXT NOT NULL CHECK (type IN ('like', 'comment')),
    message TEXT,
    is_read BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

ALTER TABLE public.notifications ENABLE ROW LEVEL SECURITY;

CREATE POLICY "Notifications visible to recipient"
    ON public.notifications FOR SELECT
    USING (auth.uid() = user_id);

CREATE POLICY "Actors can insert notifications"
    ON public.notifications FOR INSERT
    WITH CHECK (auth.uid() = actor_id);

CREATE POLICY "Recipients can update read state"
    ON public.notifications FOR UPDATE
    USING (auth.uid() = user_id)
    WITH CHECK (auth.uid() = user_id);

-- ============================================
-- BIDS TABLE (Enhanced)
-- ============================================
CREATE TABLE public.bids (
    id UUID DEFAULT uuid_generate_v4() PRIMARY KEY,
    post_id UUID REFERENCES public.posts(id) ON DELETE CASCADE,
    bidder_id UUID REFERENCES public.profiles(id) ON DELETE CASCADE,
    amount DECIMAL(10, 2) NOT NULL,
    status TEXT DEFAULT 'active' CHECK (status IN ('active', 'won', 'lost', 'withdrawn')),
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

ALTER TABLE public.bids ENABLE ROW LEVEL SECURITY;

-- Bids policies
CREATE POLICY "Bids are viewable by everyone"
    ON public.bids FOR SELECT
    USING (true);

CREATE POLICY "Authenticated users can create bids"
    ON public.bids FOR INSERT
    WITH CHECK (auth.uid() = bidder_id);

CREATE POLICY "Users can update their own bids"
    ON public.bids FOR UPDATE
    USING (auth.uid() = bidder_id);

-- ============================================
-- ECO SAVINGS TABLE (Enhanced)
-- ============================================
CREATE TABLE public.eco_savings (
    id UUID DEFAULT uuid_generate_v4() PRIMARY KEY,
    user_id UUID REFERENCES public.profiles(id) ON DELETE CASCADE,
    
    -- Environmental metrics
    co2_saved DECIMAL(10, 2) DEFAULT 0, -- in kg
    water_saved DECIMAL(10, 2) DEFAULT 0, -- in liters
    waste_diverted DECIMAL(10, 2) DEFAULT 0, -- in kg
    energy_saved DECIMAL(10, 2) DEFAULT 0, -- in kWh
    
    -- Activity counts (synced with profiles table)
    items_swapped INTEGER DEFAULT 0,
    items_donated INTEGER DEFAULT 0,
    items_purchased INTEGER DEFAULT 0,
    
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    UNIQUE(user_id)
);

ALTER TABLE public.eco_savings ENABLE ROW LEVEL SECURITY;

-- Eco savings policies
CREATE POLICY "Users can view their own eco savings"
    ON public.eco_savings FOR SELECT
    USING (auth.uid() = user_id);

CREATE POLICY "Users can insert their own eco savings"
    ON public.eco_savings FOR INSERT
    WITH CHECK (auth.uid() = user_id);

CREATE POLICY "Users can update their own eco savings"
    ON public.eco_savings FOR UPDATE
    USING (auth.uid() = user_id);

-- ============================================
-- CHATS/MESSAGES TABLE (New)
-- ============================================
CREATE TABLE public.chats (
    id UUID DEFAULT uuid_generate_v4() PRIMARY KEY,
    sender_id UUID REFERENCES public.profiles(id) ON DELETE CASCADE,
    receiver_id UUID REFERENCES public.profiles(id) ON DELETE CASCADE,
    message TEXT NOT NULL,
    listing_id UUID REFERENCES public.posts(id) ON DELETE SET NULL,
    listing_title_snapshot TEXT,
    listing_image_url_snapshot TEXT,
    is_read BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

ALTER TABLE public.chats ENABLE ROW LEVEL SECURITY;

CREATE POLICY "Users can view their own messages"
    ON public.chats FOR SELECT
    USING (auth.uid() = sender_id OR auth.uid() = receiver_id);

CREATE POLICY "Authenticated users can send messages"
    ON public.chats FOR INSERT
    WITH CHECK (auth.uid() = sender_id);

CREATE POLICY "Users can update their received messages"
    ON public.chats FOR UPDATE
    USING (auth.uid() = receiver_id);

-- ============================================
-- TRANSACTIONS TABLE (Enhanced)
-- ============================================
CREATE TABLE public.transactions (
    id UUID DEFAULT uuid_generate_v4() PRIMARY KEY,
    post_id UUID REFERENCES public.posts(id) ON DELETE SET NULL,
    from_user_id UUID REFERENCES public.profiles(id) ON DELETE CASCADE,
    to_user_id UUID REFERENCES public.profiles(id) ON DELETE CASCADE,
    transaction_type TEXT NOT NULL CHECK (transaction_type IN ('swap', 'donation', 'purchase', 'bid_won')),
    amount DECIMAL(10, 2) DEFAULT 0,
    status TEXT DEFAULT 'completed' CHECK (status IN ('pending', 'completed', 'cancelled')),
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

ALTER TABLE public.transactions ENABLE ROW LEVEL SECURITY;

-- Transactions policies
CREATE POLICY "Users can view their own transactions"
    ON public.transactions FOR SELECT
    USING (auth.uid() = from_user_id OR auth.uid() = to_user_id);

CREATE POLICY "Authenticated users can create transactions"
    ON public.transactions FOR INSERT
    WITH CHECK (auth.uid() = from_user_id OR auth.uid() = to_user_id);

-- ============================================
-- ADMIN TABLES (New)
-- ============================================
CREATE TABLE public.admins (
    id UUID DEFAULT uuid_generate_v4() PRIMARY KEY,
    user_id UUID REFERENCES auth.users(id) ON DELETE CASCADE UNIQUE,
    name TEXT NOT NULL,
    email TEXT UNIQUE NOT NULL,
    role TEXT DEFAULT 'Admin' CHECK (role IN ('Admin', 'Super Admin', 'Moderator')),
    is_active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

-- Admin RLS policies
ALTER TABLE public.admins ENABLE ROW LEVEL SECURITY;

CREATE POLICY "Admins can view all admin accounts"
    ON public.admins FOR SELECT
    USING (
        EXISTS (
            SELECT 1 FROM public.admins 
            WHERE user_id = auth.uid() AND is_active = TRUE
        )
    );

CREATE POLICY "Super Admins can manage admins"
    ON public.admins FOR ALL
    USING (
        EXISTS (
            SELECT 1 FROM public.admins 
            WHERE user_id = auth.uid() 
            AND role = 'Super Admin' 
            AND is_active = TRUE
        )
    );

CREATE TABLE public.admin_dashboard (
    id INTEGER PRIMARY KEY DEFAULT 1,
    total_users INTEGER DEFAULT 0,
    total_posts INTEGER DEFAULT 0,
    total_swaps INTEGER DEFAULT 0,
    total_donations INTEGER DEFAULT 0,
    total_purchases INTEGER DEFAULT 0,
    total_bids INTEGER DEFAULT 0,
    total_impact_score INTEGER DEFAULT 0,
    total_co2_saved DECIMAL(10, 2) DEFAULT 0,
    total_water_saved DECIMAL(10, 2) DEFAULT 0,
    total_waste_diverted DECIMAL(10, 2) DEFAULT 0,
    last_updated TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    CHECK (id = 1) -- Ensure only one row exists
);

-- Admin dashboard RLS
ALTER TABLE public.admin_dashboard ENABLE ROW LEVEL SECURITY;

CREATE POLICY "Admins can view dashboard"
    ON public.admin_dashboard FOR SELECT
    USING (
        EXISTS (
            SELECT 1 FROM public.admins 
            WHERE user_id = auth.uid() AND is_active = TRUE
        )
    );

-- Insert initial dashboard row
INSERT INTO public.admin_dashboard (id) VALUES (1)
ON CONFLICT (id) DO NOTHING;

-- ============================================
-- INDEXES FOR PERFORMANCE
-- ============================================
CREATE INDEX idx_posts_user ON public.posts(user_id);
CREATE INDEX idx_posts_category ON public.posts(category);
CREATE INDEX idx_posts_status ON public.posts(status);
CREATE INDEX idx_posts_created ON public.posts(created_at DESC);
CREATE INDEX idx_comments_post ON public.comments(post_id);
CREATE INDEX idx_comments_author ON public.comments(author_id);
CREATE INDEX idx_bids_post ON public.bids(post_id);
CREATE INDEX idx_bids_bidder ON public.bids(bidder_id);
CREATE INDEX idx_swaps_users ON public.swaps(user1_id, user2_id);
CREATE INDEX idx_donations_donor ON public.donations(donor_id);
CREATE INDEX idx_notifications_user ON public.notifications(user_id, is_read);
CREATE INDEX idx_purchases_buyer ON public.purchases(buyer_id);
CREATE INDEX idx_transactions_users ON public.transactions(from_user_id, to_user_id);
CREATE INDEX idx_chats_users ON public.chats(sender_id, receiver_id);
CREATE INDEX idx_chats_created ON public.chats(created_at DESC);

-- ============================================
-- UTILITY FUNCTIONS
-- ============================================

-- Function to update updated_at timestamp
CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = NOW();
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- Triggers for updated_at
CREATE TRIGGER update_profiles_updated_at BEFORE UPDATE ON public.profiles
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_posts_updated_at BEFORE UPDATE ON public.posts
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_eco_savings_updated_at BEFORE UPDATE ON public.eco_savings
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

-- ============================================
-- TRIGGER FUNCTIONS
-- ============================================

-- Function to automatically create profile on user signup
CREATE OR REPLACE FUNCTION public.handle_new_user()
RETURNS TRIGGER AS $$
BEGIN
    -- Create profile with email from auth
    INSERT INTO public.profiles (id, name, email, bio)
    VALUES (
        NEW.id, 
        COALESCE(NEW.raw_user_meta_data->>'name', 'New User'),
        NEW.email,
        ''
    );
    
    -- Create eco_savings entry
    INSERT INTO public.eco_savings (user_id)
    VALUES (NEW.id);
    
    RETURN NEW;
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;

-- Trigger to create profile on signup
CREATE TRIGGER on_auth_user_created
    AFTER INSERT ON auth.users
    FOR EACH ROW EXECUTE FUNCTION public.handle_new_user();

-- Function to update user impact scores and eco savings
CREATE OR REPLACE FUNCTION public.update_user_impact()
RETURNS TRIGGER AS $$
DECLARE
    affected_user_id UUID;
    swap_count INTEGER;
    donation_count INTEGER;
    purchase_count INTEGER;
    new_impact_score INTEGER;
    new_level TEXT;
    new_icon TEXT;
BEGIN
    -- Determine which user(s) to update based on the table
    IF TG_TABLE_NAME = 'swaps' THEN
        -- Update both users in a swap
        FOR affected_user_id IN SELECT unnest(ARRAY[NEW.user1_id, NEW.user2_id])
        LOOP
            -- Count activities
            SELECT COUNT(*) INTO swap_count FROM public.swaps 
            WHERE (user1_id = affected_user_id OR user2_id = affected_user_id) AND status = 'completed';
            
            SELECT COUNT(*) INTO donation_count FROM public.donations 
            WHERE donor_id = affected_user_id AND status = 'completed';
            
            SELECT COUNT(*) INTO purchase_count FROM public.purchases 
            WHERE buyer_id = affected_user_id;
            
            -- Calculate impact score
            new_impact_score := (swap_count * 2) + (donation_count * 3) + (purchase_count * 1);
            
            -- Determine level and icon
            IF new_impact_score >= 100 THEN
                new_level := 'Planet Pioneer';
                new_icon := '🌞';
            ELSIF new_impact_score >= 50 THEN
                new_level := 'Eco Guardian';
                new_icon := '🦋';
            ELSIF new_impact_score >= 25 THEN
                new_level := 'Sustainable Hero';
                new_icon := '🌍';
            ELSIF new_impact_score >= 10 THEN
                new_level := 'Rising Recycler';
                new_icon := '♻️';
            ELSE
                new_level := 'Beginner EcoSaver';
                new_icon := '🌱';
            END IF;
            
            -- Update profile
            UPDATE public.profiles
            SET 
                total_swaps = swap_count,
                total_donations = donation_count,
                total_purchases = purchase_count,
                impact_score = new_impact_score,
                eco_level = new_level,
                eco_icon = new_icon
            WHERE id = affected_user_id;
            
            -- Update eco_savings for swaps (5kg CO2, 100L water, 2kg waste per swap)
            UPDATE public.eco_savings
            SET 
                co2_saved = co2_saved + 5,
                water_saved = water_saved + 100,
                waste_diverted = waste_diverted + 2,
                energy_saved = energy_saved + 10,
                items_swapped = swap_count
            WHERE user_id = affected_user_id;
        END LOOP;
        
    ELSIF TG_TABLE_NAME = 'donations' THEN
        affected_user_id := NEW.donor_id;
        
        -- Count activities
        SELECT COUNT(*) INTO swap_count FROM public.swaps 
        WHERE (user1_id = affected_user_id OR user2_id = affected_user_id) AND status = 'completed';
        
        SELECT COUNT(*) INTO donation_count FROM public.donations 
        WHERE donor_id = affected_user_id AND status = 'completed';
        
        SELECT COUNT(*) INTO purchase_count FROM public.purchases 
        WHERE buyer_id = affected_user_id;
        
        -- Calculate impact score
        new_impact_score := (swap_count * 2) + (donation_count * 3) + (purchase_count * 1);
        
        -- Determine level and icon
        IF new_impact_score >= 100 THEN
            new_level := 'Planet Pioneer';
            new_icon := '🌞';
        ELSIF new_impact_score >= 50 THEN
            new_level := 'Eco Guardian';
            new_icon := '🦋';
        ELSIF new_impact_score >= 25 THEN
            new_level := 'Sustainable Hero';
            new_icon := '🌍';
        ELSIF new_impact_score >= 10 THEN
            new_level := 'Rising Recycler';
            new_icon := '♻️';
        ELSE
            new_level := 'Beginner EcoSaver';
            new_icon := '🌱';
        END IF;
        
        -- Update profile
        UPDATE public.profiles
        SET 
            total_swaps = swap_count,
            total_donations = donation_count,
            total_purchases = purchase_count,
            impact_score = new_impact_score,
            eco_level = new_level,
            eco_icon = new_icon
        WHERE id = affected_user_id;
        
        -- Update eco_savings for donations (7kg CO2, 150L water, 3kg waste per donation)
        UPDATE public.eco_savings
        SET 
            co2_saved = co2_saved + 7,
            water_saved = water_saved + 150,
            waste_diverted = waste_diverted + 3,
            energy_saved = energy_saved + 15,
            items_donated = donation_count
        WHERE user_id = affected_user_id;
        
    ELSIF TG_TABLE_NAME = 'purchases' THEN
        affected_user_id := NEW.buyer_id;
        
        -- Count activities
        SELECT COUNT(*) INTO swap_count FROM public.swaps 
        WHERE (user1_id = affected_user_id OR user2_id = affected_user_id) AND status = 'completed';
        
        SELECT COUNT(*) INTO donation_count FROM public.donations 
        WHERE donor_id = affected_user_id AND status = 'completed';
        
        SELECT COUNT(*) INTO purchase_count FROM public.purchases 
        WHERE buyer_id = affected_user_id;
        
        -- Calculate impact score
        new_impact_score := (swap_count * 2) + (donation_count * 3) + (purchase_count * 1);
        
        -- Determine level and icon
        IF new_impact_score >= 100 THEN
            new_level := 'Planet Pioneer';
            new_icon := '🌞';
        ELSIF new_impact_score >= 50 THEN
            new_level := 'Eco Guardian';
            new_icon := '🦋';
        ELSIF new_impact_score >= 25 THEN
            new_level := 'Sustainable Hero';
            new_icon := '🌍';
        ELSIF new_impact_score >= 10 THEN
            new_level := 'Rising Recycler';
            new_icon := '♻️';
        ELSE
            new_level := 'Beginner EcoSaver';
            new_icon := '🌱';
        END IF;
        
        -- Update profile
        UPDATE public.profiles
        SET 
            total_swaps = swap_count,
            total_donations = donation_count,
            total_purchases = purchase_count,
            impact_score = new_impact_score,
            eco_level = new_level,
            eco_icon = new_icon
        WHERE id = affected_user_id;
        
        -- Update eco_savings for purchases (3kg CO2, 50L water, 1kg waste per purchase)
        UPDATE public.eco_savings
        SET 
            co2_saved = co2_saved + 3,
            water_saved = water_saved + 50,
            waste_diverted = waste_diverted + 1,
            energy_saved = energy_saved + 5,
            items_purchased = purchase_count
        WHERE user_id = affected_user_id;
    END IF;
    
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- Triggers for user impact updates
DROP TRIGGER IF EXISTS update_user_impact_swaps ON public.swaps;
CREATE TRIGGER update_user_impact_swaps
    AFTER INSERT OR UPDATE ON public.swaps
    FOR EACH ROW EXECUTE FUNCTION public.update_user_impact();

DROP TRIGGER IF EXISTS update_user_impact_donations ON public.donations;
CREATE TRIGGER update_user_impact_donations
    AFTER INSERT OR UPDATE ON public.donations
    FOR EACH ROW EXECUTE FUNCTION public.update_user_impact();

DROP TRIGGER IF EXISTS update_user_impact_purchases ON public.purchases;
CREATE TRIGGER update_user_impact_purchases
    AFTER INSERT OR UPDATE ON public.purchases
    FOR EACH ROW EXECUTE FUNCTION public.update_user_impact();

-- ============================================
-- ADMIN DASHBOARD UPDATE FUNCTION
-- ============================================
CREATE OR REPLACE FUNCTION public.update_admin_dashboard()
RETURNS TRIGGER AS $$
BEGIN
    UPDATE public.admin_dashboard
    SET
        total_users = (SELECT COUNT(*) FROM public.profiles),
        total_posts = (SELECT COUNT(*) FROM public.posts),
        total_swaps = (SELECT COUNT(*) FROM public.swaps WHERE status = 'completed'),
        total_donations = (SELECT COUNT(*) FROM public.donations WHERE status = 'completed'),
        total_purchases = (SELECT COUNT(*) FROM public.purchases),
        total_bids = (SELECT COUNT(*) FROM public.bids),
        total_impact_score = (SELECT COALESCE(SUM(impact_score), 0) FROM public.profiles),
        total_co2_saved = (SELECT COALESCE(SUM(co2_saved), 0) FROM public.eco_savings),
        total_water_saved = (SELECT COALESCE(SUM(water_saved), 0) FROM public.eco_savings),
        total_waste_diverted = (SELECT COALESCE(SUM(waste_diverted), 0) FROM public.eco_savings),
        last_updated = NOW()
    WHERE id = 1;
    
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- Triggers for admin dashboard
DROP TRIGGER IF EXISTS update_dashboard_on_profiles ON public.profiles;
CREATE TRIGGER update_dashboard_on_profiles
    AFTER INSERT OR UPDATE OR DELETE ON public.profiles
    FOR EACH STATEMENT EXECUTE FUNCTION public.update_admin_dashboard();

DROP TRIGGER IF EXISTS update_dashboard_on_posts ON public.posts;
CREATE TRIGGER update_dashboard_on_posts
    AFTER INSERT OR UPDATE OR DELETE ON public.posts
    FOR EACH STATEMENT EXECUTE FUNCTION public.update_admin_dashboard();

DROP TRIGGER IF EXISTS update_dashboard_on_swaps ON public.swaps;
CREATE TRIGGER update_dashboard_on_swaps
    AFTER INSERT OR UPDATE OR DELETE ON public.swaps
    FOR EACH STATEMENT EXECUTE FUNCTION public.update_admin_dashboard();

DROP TRIGGER IF EXISTS update_dashboard_on_donations ON public.donations;
CREATE TRIGGER update_dashboard_on_donations
    AFTER INSERT OR UPDATE OR DELETE ON public.donations
    FOR EACH STATEMENT EXECUTE FUNCTION public.update_admin_dashboard();

DROP TRIGGER IF EXISTS update_dashboard_on_purchases ON public.purchases;
CREATE TRIGGER update_dashboard_on_purchases
    AFTER INSERT OR UPDATE OR DELETE ON public.purchases
    FOR EACH STATEMENT EXECUTE FUNCTION public.update_admin_dashboard();

DROP TRIGGER IF EXISTS update_dashboard_on_bids ON public.bids;
CREATE TRIGGER update_dashboard_on_bids
    AFTER INSERT OR UPDATE OR DELETE ON public.bids
    FOR EACH STATEMENT EXECUTE FUNCTION public.update_admin_dashboard();

DROP TRIGGER IF EXISTS update_dashboard_on_eco_savings ON public.eco_savings;
CREATE TRIGGER update_dashboard_on_eco_savings
    AFTER INSERT OR UPDATE OR DELETE ON public.eco_savings
    FOR EACH STATEMENT EXECUTE FUNCTION public.update_admin_dashboard();
