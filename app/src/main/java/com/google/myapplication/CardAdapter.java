package com.google.myapplication;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;

public class CardAdapter extends RecyclerView.Adapter<CardAdapter.ViewHolder> {

    private ArrayList<String> arrCardTime;
    private int type;

    public CardAdapter(ArrayList<String> data, int type) {
        this.arrCardTime = data;
        this.type = type;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        View view = inflater.inflate(R.layout.item_row, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.tvTimeCard.setText(arrCardTime.get(position));
        if (type == 0) {
            holder.imgCard.setImageResource(R.drawable.rfid);
        } else if (type == 1) {
            holder.imgCard.setImageResource(R.drawable.ic_cup);
        }
    }

    @Override
    public int getItemCount() {
        return arrCardTime.size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder{
        private TextView tvTimeCard;
        private ImageView imgCard;
        public ViewHolder(View itemView) {
            super(itemView);
            tvTimeCard = itemView.findViewById(R.id.tv_time);
            imgCard = itemView.findViewById(R.id.img_item);
        }
    }
}
