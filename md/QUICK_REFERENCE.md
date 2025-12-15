# 🎯 Quick Database Reference

## 📊 Table Relationships

```
auth.users (Supabase)
    ↓
profiles (your users)
    ↓
├── posts (items & community posts)
│   ├── comments
│   └── bids
├── swaps (between user1 & user2)
├── donations (from donor)
├── purchases (by buyer)
├── chats (sender ↔ receiver)
├── transactions (from_user → to_user)
└── eco_savings (environmental tracking)

admins
    ↓
admin_dashboard (statistics)
```

---

## 🔑 Key Fields

### profiles
- `id` - UUID (from auth.users)
- `email` - Unique email
- `impact_score` - Gamification points
- `eco_level` - Text rank
- `eco_icon` - Emoji badge
- `total_swaps/donations/purchases` - Activity counters

### posts
- `category` - 'swap', 'donation', 'bidding', 'community', etc.
- `status` - 'available', 'pending', 'completed', 'cancelled'
- `condition` - 'new', 'like_new', 'good', 'fair', 'poor'

### swaps
- `user1_id`, `user2_id` - Both users
- `post1_id`, `post2_id` - Their items
- `status` - Swap progress

### eco_savings
- `co2_saved` - Kilograms
- `water_saved` - Liters
- `waste_diverted` - Kilograms
- `energy_saved` - Kilowatt-hours

---

## 🎮 Gamification Queries

### Check User Level
```sql
SELECT name, eco_level, eco_icon, impact_score
FROM profiles
WHERE id = 'user-uuid';
```

### View Leaderboard
```sql
SELECT name, eco_level, eco_icon, impact_score
FROM profiles
ORDER BY impact_score DESC
LIMIT 10;
```

### User's Environmental Impact
```sql
SELECT 
    p.name,
    e.co2_saved,
    e.water_saved,
    e.waste_diverted,
    e.items_swapped,
    e.items_donated
FROM profiles p
JOIN eco_savings e ON p.id = e.user_id
WHERE p.id = 'user-uuid';
```

---

## 📈 Common Queries

### Get Available Swap Items
```sql
SELECT * FROM posts
WHERE category = 'swap'
AND status = 'available'
ORDER BY created_at DESC;
```

### Get User's Active Swaps
```sql
SELECT * FROM swaps
WHERE (user1_id = 'user-uuid' OR user2_id = 'user-uuid')
AND status IN ('pending', 'accepted')
ORDER BY created_at DESC;
```

### Get Donation History
```sql
SELECT d.*, p.title, p.description
FROM donations d
JOIN posts p ON d.post_id = p.id
WHERE d.donor_id = 'user-uuid'
ORDER BY d.created_at DESC;
```

### Get Unread Messages
```sql
SELECT * FROM chats
WHERE receiver_id = 'user-uuid'
AND is_read = FALSE
ORDER BY created_at DESC;
```

---

## 🔧 Admin Queries

### Dashboard Stats
```sql
SELECT * FROM admin_dashboard;
```

### Top Contributors
```sql
SELECT name, total_donations, impact_score, eco_level
FROM profiles
ORDER BY total_donations DESC
LIMIT 10;
```

### Recent Activity
```sql
SELECT 
    'swap' as type,
    created_at
FROM swaps
UNION ALL
SELECT 
    'donation' as type,
    created_at
FROM donations
UNION ALL
SELECT 
    'purchase' as type,
    created_at
FROM purchases
ORDER BY created_at DESC
LIMIT 20;
```

---

## 🚀 Next Steps

1. ✅ Schema improved and ready
2. ⏳ Run SQL in Supabase Dashboard
3. ⏳ Build Android app
4. ⏳ Test gamification system
5. ⏳ Monitor environmental impact tracking

**Ready to create an eco-friendly community! 🌱**
