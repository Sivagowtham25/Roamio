package com.example.roamio.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.roamio.R;
import com.example.roamio.models.ChatMessage;

import java.util.List;

public class ChatAdapter extends RecyclerView.Adapter<ChatAdapter.ChatViewHolder> {

    private final Context           context;
    private final List<ChatMessage> messages;

    public ChatAdapter(Context context, List<ChatMessage> messages) {
        this.context  = context;
        this.messages = messages;
    }

    @NonNull
    @Override
    public ChatViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context)
                .inflate(R.layout.item_chat_message, parent, false);
        return new ChatViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ChatViewHolder holder, int position) {
        ChatMessage msg = messages.get(position);

        if (msg.isUser()) {
            holder.layoutUser.setVisibility(View.VISIBLE);
            holder.layoutAi.setVisibility(View.GONE);
            holder.tvUserMessage.setText(msg.getText());
        } else {
            holder.layoutUser.setVisibility(View.GONE);
            holder.layoutAi.setVisibility(View.VISIBLE);
            holder.tvAiMessage.setText(msg.getText());
        }
    }

    @Override
    public int getItemCount() { return messages.size(); }

    static class ChatViewHolder extends RecyclerView.ViewHolder {
        LinearLayout layoutUser, layoutAi;
        TextView     tvUserMessage, tvAiMessage;

        ChatViewHolder(@NonNull View itemView) {
            super(itemView);
            layoutUser    = itemView.findViewById(R.id.layoutUser);
            layoutAi      = itemView.findViewById(R.id.layoutAi);
            tvUserMessage = itemView.findViewById(R.id.tvUserMessage);
            tvAiMessage   = itemView.findViewById(R.id.tvAiMessage);
        }
    }
}
