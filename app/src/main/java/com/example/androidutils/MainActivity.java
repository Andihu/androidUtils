package com.example.androidutils;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.os.Bundle;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    RecyclerView list;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        list=findViewById(R.id.list);

        Adapter adapter=new Adapter(this);

        list.setLayoutManager(new LinearLayoutManager(this));

        list.setAdapter(adapter);

        List<String> list =new ArrayList<>();

        list.add("13121375238");

        list.add("13121375238");
        list.add("15155671108");
        list.add("15010470847");
        list.add("15650727839");
        list.add("15210855829");
        list.add("13121375238");
        list.add("15155671108");
        list.add("15010470847");
        list.add("15650727839");
        list.add("15210855829");

        adapter.setEntrys(list);


    }
}
