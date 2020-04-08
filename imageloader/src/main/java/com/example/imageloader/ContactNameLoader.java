package com.example.imageloader;

import android.database.Cursor;
import android.os.AsyncTask;
import android.provider.ContactsContract;
import android.text.TextUtils;
import android.widget.TextView;

import java.util.HashMap;
import java.util.Map;

public class ContactNameLoader {
    /**
     * A LruCache used to store images which has a maximum size of 10% of the maximum heap size.
     */

    private static final Map<String ,String> CACHE =new HashMap();

    private ContactNameLoader() {
    }

    interface Listener {
        void onLoaded(String s);
    }

    public static void loadMediaStoreThumbnail(final TextView imageView,
                                        final String id,
                                        final Listener listener) {

        final String cachedValue = CACHE.get(id);
        imageView.setTag(id);
        if (cachedValue != null) {
            // If the image is already in the cache, display the image,
            // call the listener now and return
            imageView.setText(cachedValue);
            if (listener != null) {
                listener.onLoaded(cachedValue);
            }
            return;
        }

        new AsyncTask<Void, Void, String>() {
            @Override
            protected String doInBackground(Void... params) {
                String phoneSelection = ContactsContract.CommonDataKinds.Phone.CONTACT_ID
                        + "=?";
                Cursor cursor = imageView.getContext().getContentResolver().query(
                        ContactsContract.CommonDataKinds.Phone.CONTENT_URI, null,
                        phoneSelection,
                        new String[] { id },
                        null);
                while (cursor!=null&&cursor.moveToFirst()){
                    int columnIndex = cursor.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME);
                    String name = cursor.getString(columnIndex);
                    return name;
                }
                return null;
            }

            @Override
            protected void onPostExecute(String s) {
                if (!TextUtils.isEmpty(s)) {
                    // 通过 tag 来防止图片错位
                    if (imageView.getTag() != null && imageView.getTag().equals(id)) {
                        imageView.setText(s);
                    }
                    // Add the image to the memory cache first
                    CACHE.put(id, s);
                    if (listener != null) {
                        listener.onLoaded(s);
                    }
                }
            }
        }.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }

    /**
     * A simple cache implementation for {@link android.graphics.Bitmap} instances which uses
     * {@link android.support.v4.util.LruCache}.
     */
}
