package com.capstone.testapp;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;

public class ContactAdapter extends RecyclerView.Adapter<ContactAdapter.ContactViewHolder> {

    private final List<Contact> contactList;
    private final OnContactClickListener listener; // --- NEW: Add a listener variable ---

    // --- NEW: Define the listener interface ---
    // This creates a "contract" that MainActivity will follow.
    public interface OnContactClickListener {
        void onContactClick(Contact contact);
    }

    // --- UPDATED: The constructor now accepts a listener ---
    public ContactAdapter(List<Contact> contactList, OnContactClickListener listener) {
        this.contactList = contactList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ContactViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.contact_item, parent, false);
        return new ContactViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ContactViewHolder holder, int position) {
        Contact contact = contactList.get(position);
        // --- UPDATED: Pass the contact and listener to the ViewHolder ---
        holder.bind(contact, listener);
    }

    @Override
    public int getItemCount() {
        return contactList.size();
    }

    public static class ContactViewHolder extends RecyclerView.ViewHolder {
        TextView contactNameTextView;

        public ContactViewHolder(@NonNull View itemView) {
            super(itemView);
            contactNameTextView = itemView.findViewById(R.id.contactNameTextView);
        }

        // --- NEW: A method to set the data and the click listener for a single row ---
        public void bind(final Contact contact, final OnContactClickListener listener) {
            contactNameTextView.setText(contact.name);
            itemView.setOnClickListener(v -> listener.onContactClick(contact));
        }
    }
}