package com.example.ecoswap.dashboard.trades;

import android.content.Context;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.example.ecoswap.R;
import com.google.android.material.button.MaterialButton;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class TradeHistoryAdapter extends RecyclerView.Adapter<TradeHistoryAdapter.TradeViewHolder> {

    public interface TradeActionListener {
        void onConfirmTrade(@NonNull TradeRecord record);
        void onUploadProof(@NonNull TradeRecord record);
        void onTradeClicked(@NonNull TradeRecord record);
    }

    private final List<TradeRecord> trades;
    private final LayoutInflater inflater;
    private final TradeActionListener listener;
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("MMM d, yyyy", Locale.getDefault());

    public TradeHistoryAdapter(@NonNull Context context,
                               @NonNull List<TradeRecord> trades,
                               @NonNull TradeActionListener listener) {
        this.trades = trades;
        this.listener = listener;
        this.inflater = LayoutInflater.from(context);
    }

    @NonNull
    @Override
    public TradeViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = inflater.inflate(R.layout.item_trade_record, parent, false);
        return new TradeViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull TradeViewHolder holder, int position) {
        TradeRecord record = trades.get(position);
        holder.bind(record, listener, dateFormat);
    }

    @Override
    public int getItemCount() {
        return trades.size();
    }

    public void replaceData(@NonNull List<TradeRecord> newItems) {
        trades.clear();
        trades.addAll(newItems);
        notifyDataSetChanged();
    }

    static class TradeViewHolder extends RecyclerView.ViewHolder {
        private final TextView tvTradeType;
        private final TextView tvTradeStatus;
        private final TextView tvTradePartner;
        private final TextView tvTradeDescription;
        private final TextView tvTradeDate;
        private final ImageView ivPrimaryItem;
        private final ImageView ivSecondaryItem;
        private final MaterialButton btnConfirmTrade;
        private final ImageView ivProofPhoto;
        private final TextView tvProofStatus;
        private final MaterialButton btnUploadProof;

        TradeViewHolder(@NonNull View itemView) {
            super(itemView);
            tvTradeType = itemView.findViewById(R.id.tvTradeType);
            tvTradeStatus = itemView.findViewById(R.id.tvTradeStatus);
            tvTradePartner = itemView.findViewById(R.id.tvTradePartner);
            tvTradeDescription = itemView.findViewById(R.id.tvTradeDescription);
            tvTradeDate = itemView.findViewById(R.id.tvTradeDate);
            ivPrimaryItem = itemView.findViewById(R.id.ivPrimaryItem);
            ivSecondaryItem = itemView.findViewById(R.id.ivSecondaryItem);
            btnConfirmTrade = itemView.findViewById(R.id.btnConfirmTrade);
            ivProofPhoto = itemView.findViewById(R.id.ivProofPhoto);
            tvProofStatus = itemView.findViewById(R.id.tvProofStatus);
            btnUploadProof = itemView.findViewById(R.id.btnUploadProof);
        }

        void bind(TradeRecord record,
                  TradeActionListener listener,
                  SimpleDateFormat dateFormat) {
            boolean isSwap = record.getType() == TradeRecord.TradeType.SWAP;
            tvTradeType.setText(isSwap ? itemView.getContext().getString(R.string.listing_type_swap)
                : itemView.getContext().getString(R.string.listing_type_donation));

            boolean isCompleted = record.isCompleted();
                String statusLabel = isCompleted
                    ? itemView.getContext().getString(R.string.trade_history_confirmed_label)
                    : itemView.getContext().getString(R.string.trade_history_pending_label);
            tvTradeStatus.setText(statusLabel);
            tvTradeStatus.setBackgroundResource(isCompleted
                ? R.drawable.bg_status_chip_completed
                : R.drawable.bg_status_chip_pending);
            int statusColor = ContextCompat.getColor(itemView.getContext(), isCompleted
                ? R.color.success_green
                : R.color.primary_blue);
            tvTradeStatus.setTextColor(statusColor);

            if (isSwap) {
                String partnerName = !TextUtils.isEmpty(record.getCounterpartyName())
                        ? record.getCounterpartyName()
                        : itemView.getContext().getString(R.string.trade_history_partner_unknown);
                tvTradePartner.setText(itemView.getContext().getString(R.string.trade_history_swap_with, partnerName));
            } else {
                String receiverLabel = !TextUtils.isEmpty(record.getReceiverName())
                        ? record.getReceiverName()
                        : itemView.getContext().getString(R.string.trade_history_receiver_placeholder);
                tvTradePartner.setText(itemView.getContext().getString(R.string.trade_history_donation_with, receiverLabel));
            }

            String primaryTitle = record.getPrimaryItem() != null && !TextUtils.isEmpty(record.getPrimaryItem().getTitle())
                    ? record.getPrimaryItem().getTitle()
                    : itemView.getContext().getString(R.string.app_name);
            String displayText = primaryTitle;
            if (!isSwap && !TextUtils.isEmpty(record.getPickupLocation())) {
                displayText = primaryTitle + " \u2022 " + record.getPickupLocation();
            }
            tvTradeDescription.setText(displayText);

            if (record.getPrimaryItem() != null && !TextUtils.isEmpty(record.getPrimaryItem().getImageUrl())) {
                Glide.with(itemView.getContext())
                        .load(record.getPrimaryItem().getImageUrl())
                        .centerCrop()
                        .placeholder(R.drawable.ic_launcher_background)
                        .error(R.drawable.ic_launcher_background)
                        .into(ivPrimaryItem);
            } else {
                ivPrimaryItem.setImageResource(R.drawable.ic_launcher_background);
            }

            if (record.getType() == TradeRecord.TradeType.SWAP && record.getSecondaryItem() != null) {
                ivSecondaryItem.setVisibility(View.VISIBLE);
                if (!TextUtils.isEmpty(record.getSecondaryItem().getImageUrl())) {
                    Glide.with(itemView.getContext())
                            .load(record.getSecondaryItem().getImageUrl())
                            .centerCrop()
                            .placeholder(R.drawable.ic_launcher_background)
                            .error(R.drawable.ic_launcher_background)
                            .into(ivSecondaryItem);
                } else {
                    ivSecondaryItem.setImageResource(R.drawable.ic_launcher_background);
                }
            } else {
                ivSecondaryItem.setVisibility(View.GONE);
            }

            String dateLabel = dateFormat.format(new Date(record.getCreatedAtEpochMs()));
            tvTradeDate.setText(dateLabel);

            if (record.canConfirm()) {
                btnConfirmTrade.setVisibility(View.VISIBLE);
                btnConfirmTrade.setOnClickListener(v -> listener.onConfirmTrade(record));
            } else {
                btnConfirmTrade.setVisibility(View.GONE);
                btnConfirmTrade.setOnClickListener(null);
            }

            boolean hasProofPhoto = !TextUtils.isEmpty(record.getProofPhotoUrl());
            boolean canUploadProof = record.canUploadProof();

            if (hasProofPhoto && ivProofPhoto != null) {
                ivProofPhoto.setVisibility(View.VISIBLE);
                Glide.with(itemView.getContext())
                        .load(record.getProofPhotoUrl())
                        .centerCrop()
                        .placeholder(R.drawable.ic_launcher_background)
                        .into(ivProofPhoto);
            } else if (ivProofPhoto != null) {
                ivProofPhoto.setVisibility(View.GONE);
            }

            if (tvProofStatus != null) {
                if (hasProofPhoto) {
                    tvProofStatus.setText(R.string.trade_history_proof_uploaded);
                } else if (canUploadProof) {
                    tvProofStatus.setText(R.string.trade_history_proof_missing);
                } else {
                    tvProofStatus.setText(R.string.trade_history_proof_locked);
                }
            }

            if (btnUploadProof != null) {
                btnUploadProof.setText(hasProofPhoto
                        ? itemView.getContext().getString(R.string.trade_history_replace_proof)
                        : itemView.getContext().getString(R.string.trade_history_add_proof));
                btnUploadProof.setEnabled(canUploadProof);
                btnUploadProof.setAlpha(canUploadProof ? 1f : 0.5f);
                if (canUploadProof) {
                    btnUploadProof.setOnClickListener(v -> listener.onUploadProof(record));
                } else {
                    btnUploadProof.setOnClickListener(null);
                }
                btnUploadProof.setVisibility(View.VISIBLE);
            }

            itemView.setOnClickListener(v -> listener.onTradeClicked(record));
        }
    }
}
