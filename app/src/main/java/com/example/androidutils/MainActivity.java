package com.example.androidutils;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.database.Cursor;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.util.Log;

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

        list.add("18601031738");

        list.add("13641129517");
        list.add("13439801753");
        list.add("13718869566");

        list.add("15010470847");

        list.add("18601031738");

        list.add("13641129517");
        list.add("13439801753");
        list.add("13718869566");

        list.add("15010470847");
        list.add("18601031738");

        list.add("13641129517");
        list.add("13439801753");
        list.add("13718869566");

        list.add("15010470847");

        adapter.setEntrys(list);
        Cursor cursor = this.getContentResolver().query(
                ContactsContract.CommonDataKinds.Phone.CONTENT_URI, null,
                null,
                null,
                null);
        cursor.moveToFirst();
         do{
            int columnIndex = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.CONTACT_ID);
            String id = cursor.getString(columnIndex);
            Log.e("contactid",id);
//            list.add(id);
        }while (cursor!=null&&cursor.moveToNext());
//         adapter.setEntrys(list);


    }
}
