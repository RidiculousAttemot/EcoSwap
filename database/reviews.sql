-- Reviews table for trade ratings
create table if not exists public.reviews (
    id uuid primary key default gen_random_uuid(),
    trade_id uuid not null,
    trade_type text not null check (trade_type in ('swap','donation')),
    rater_id uuid not null,
    ratee_id uuid not null,
    rating integer not null check (rating between 1 and 5),
    comment text,
    created_at timestamptz not null default now(),
    unique (trade_id, rater_id)
);

-- Helpful indexes
create index if not exists idx_reviews_ratee on public.reviews(ratee_id);
create index if not exists idx_reviews_trade on public.reviews(trade_id);

-- RLS: only participants can read/write their trade reviews
alter table public.reviews enable row level security;

create policy "reviews_select_participants" on public.reviews
for select
using (auth.uid() = rater_id or auth.uid() = ratee_id);

create policy "reviews_insert_participants" on public.reviews
for insert
with check (auth.uid() = rater_id);

create policy "reviews_update_owner" on public.reviews
for update
using (auth.uid() = rater_id)
with check (auth.uid() = rater_id);

-- Aggregate helpers on profiles
-- Add columns if missing
alter table public.profiles
    add column if not exists rating double precision,
    add column if not exists review_count integer default 0;

-- Recompute aggregates (run once after migration if you already have data)
-- update public.profiles p
-- set rating = sub.avg_rating,
--     review_count = coalesce(sub.review_count, 0)
-- from (
--   select ratee_id, avg(rating)::double precision as avg_rating, count(*) as review_count
--   from public.reviews
--   group by ratee_id
-- ) sub
-- where p.id = sub.ratee_id;
