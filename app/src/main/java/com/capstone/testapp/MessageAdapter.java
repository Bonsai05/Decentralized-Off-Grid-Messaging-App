package com.capstone.testapp;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RelativeLayout; // Import RelativeLayout
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;

public class MessageAdapter extends RecyclerView.Adapter<MessageAdapter.MessageViewHolder> {

    private final List<Message> messageList;

    public MessageAdapter(List<Message> messageList) {
        this.messageList = messageList;
    }

    @NonNull
    @Override
    public MessageViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // Inflate the updated message_item.xml layout
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.message_item, parent, false);
        return new MessageViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull MessageViewHolder holder, int position) {
        Message message = messageList.get(position);
        holder.messageTextView.setText(message.textContent);

        // --- THIS IS THE CHAT BUBBLE LOGIC ---
        // Create layout parameters for the TextView within the RelativeLayout
        RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(
                RelativeLayout.LayoutParams.WRAP_CONTENT,
                RelativeLayout.LayoutParams.WRAP_CONTENT
        );

        // Add some margin between messages
        params.topMargin = 4;
        params.bottomMargin = 4;

        if (message.isSentByMe) {
            // If the message is sent by me:
            // Set the background to the green bubble
            holder.messageTextView.setBackgroundResource(R.drawable.bg_bubble_sent);
            // Align the bubble to the right side of the screen
            params.addRule(RelativeLayout.ALIGN_PARENT_END);
            params.leftMargin = 100; // Add margin to the left to prevent full width
        } else {
            // If the message is received:
            // Set the background to the white bubble
            holder.messageTextView.setBackgroundResource(R.drawable.bg_bubble_received);
            // Align the bubble to the left side of the screen
            params.addRule(RelativeLayout.ALIGN_PARENT_START);
            params.rightMargin = 100; // Add margin to the right to prevent full width
        }

        // Apply the background and alignment rules to the TextView
        holder.messageTextView.setLayoutParams(params);
        // --- END OF CHAT BUBBLE LOGIC ---
    }

    @Override
    public int getItemCount() {
        return messageList.size();
    }

    public static class MessageViewHolder extends RecyclerView.ViewHolder {
        TextView messageTextView;

        public MessageViewHolder(@NonNull View itemView) {
            super(itemView);
            messageTextView = itemView.findViewById(R.id.messageTextView);
        }
    }
}
