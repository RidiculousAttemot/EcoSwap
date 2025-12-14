package com.example.ecoswap.dashboard.trades;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * Represents either a swap or donation record pulled from Supabase along with
 * enough metadata to render the proof card UI.
 */
public class TradeRecord {

    public enum TradeType {
        SWAP,
        DONATION
    }

    public static class TradeItem {
        private final String postId;
        private final String title;
        private final String imageUrl;
        private final String ownerId;

        public TradeItem(String postId, String title, String imageUrl, @Nullable String ownerId) {
            this.postId = postId;
            this.title = title;
            this.imageUrl = imageUrl;
            this.ownerId = ownerId;
        }

        public String getPostId() {
            return postId;
        }

        public String getTitle() {
            return title;
        }

        public String getImageUrl() {
            return imageUrl;
        }

        @Nullable
        public String getOwnerId() {
            return ownerId;
        }
    }

    private final String id;
    private final TradeType type;
    private final long createdAtEpochMs;
    private String status;
    private Long completedAtEpochMs;
    private TradeItem primaryItem;
    private TradeItem secondaryItem;
    private String counterpartyId;
    private String counterpartyName;
    private String receiverName;
    private String pickupLocation;
    private String proofPhotoUrl;

    public TradeRecord(String id, TradeType type, long createdAtEpochMs) {
        this.id = id;
        this.type = type;
        this.createdAtEpochMs = createdAtEpochMs;
    }

    public String getId() {
        return id;
    }

    public TradeType getType() {
        return type;
    }

    public long getCreatedAtEpochMs() {
        return createdAtEpochMs;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    @Nullable
    public Long getCompletedAtEpochMs() {
        return completedAtEpochMs;
    }

    public void setCompletedAtEpochMs(@Nullable Long completedAtEpochMs) {
        this.completedAtEpochMs = completedAtEpochMs;
    }

    public TradeItem getPrimaryItem() {
        return primaryItem;
    }

    public void setPrimaryItem(TradeItem primaryItem) {
        this.primaryItem = primaryItem;
    }

    @Nullable
    public TradeItem getSecondaryItem() {
        return secondaryItem;
    }

    public void setSecondaryItem(@Nullable TradeItem secondaryItem) {
        this.secondaryItem = secondaryItem;
    }

    @Nullable
    public String getCounterpartyId() {
        return counterpartyId;
    }

    public void setCounterpartyId(@Nullable String counterpartyId) {
        this.counterpartyId = counterpartyId;
    }

    @Nullable
    public String getCounterpartyName() {
        return counterpartyName;
    }

    public void setCounterpartyName(@Nullable String counterpartyName) {
        this.counterpartyName = counterpartyName;
    }

    @Nullable
    public String getReceiverName() {
        return receiverName;
    }

    public void setReceiverName(@Nullable String receiverName) {
        this.receiverName = receiverName;
    }

    @Nullable
    public String getPickupLocation() {
        return pickupLocation;
    }

    public void setPickupLocation(@Nullable String pickupLocation) {
        this.pickupLocation = pickupLocation;
    }

    @Nullable
    public String getProofPhotoUrl() {
        return proofPhotoUrl;
    }

    public void setProofPhotoUrl(@Nullable String proofPhotoUrl) {
        this.proofPhotoUrl = proofPhotoUrl;
    }

    public boolean isCompleted() {
        return status != null && isCompletedStatus(status);
    }

    public boolean canConfirm() {
        return status != null && !isCompleted() && !status.equalsIgnoreCase("cancelled");
    }

    public boolean canUploadProof() {
        return isCompleted();
    }

    private boolean isCompletedStatus(@NonNull String rawStatus) {
        switch (rawStatus.toLowerCase()) {
            case "completed":
            case "swapped":
            case "donated":
                return true;
            default:
                return false;
        }
    }
}
