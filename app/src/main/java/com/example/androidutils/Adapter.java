package com.example.androidutils;

import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.imageloader.ContactImageLoader;
import com.example.imageloader.ContactLoader;
import com.example.imageloader.ContactNameLoader;

import java.util.ArrayList;
import java.util.List;

public class Adapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    ContactLoader contactLoader;

    private List<String> entrys=new ArrayList<>();

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

        Holder viewhodel = (Holder) holder;

//        ContactNameLoader.loadMediaStoreThumbnail(viewhodel.textView,entrys.get(position),null);

        ContactImageLoader.loadMediaStoreThumbnail(viewhodel.imageView.getContext().getContentResolver(),viewhodel.imageView,entrys.get(position),null);

    }

    @Override
    public int getItemCount() {
        return entrys==null?0:entrys.size();
    }

    private  class Holder extends RecyclerView.ViewHolder{

        public  ImageView imageView;
        public  TextView textView;

        public Holder(@NonNull View itemView) {
            super(itemView);
            imageView = itemView.findViewById(R.id.image);
            textView=itemView.findViewById(R.id.name);

        }

    }
}
