package com.example.androidutils;

import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.imageloader.ContactLoader;

import java.util.List;

public class Adapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    ContactLoader contactLoader;

    private List<String> entrys;

    public Adapter(Context context) {
        contactLoader=ContactLoader.builder(context);
    }

    public void setEntrys(List<String> entrys) {
        this.entrys = entrys;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {

        View view= LayoutInflater.from(parent.getContext()).inflate(R.layout.item_view,parent,false);

        return new Holder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {

        Holder itemView = (Holder) holder;


        ImageView imageView=  itemView.imageView;

        if (!entrys.get(position).equals(imageView.getTag())){


            imageView.setBackgroundColor(Color.BLACK);


        }


        imageView.setTag(entrys.get(position));

        contactLoader.bindBitmap(entrys.get(position),imageView);

    }

    @Override
    public int getItemCount() {
        return entrys==null?0:entrys.size();
    }

    private static class Holder extends RecyclerView.ViewHolder{

        public static ImageView imageView;

        public Holder(@NonNull View itemView) {
            super(itemView);

            imageView = itemView.findViewById(R.id.image);
        }

    }
}
