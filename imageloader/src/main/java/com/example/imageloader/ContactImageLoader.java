package com.example.imageloader;

import android.content.ContentResolver;
import android.content.ContentUris;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.AsyncTask;
import android.provider.ContactsContract;
import android.text.TextUtils;
import android.util.Log;
import android.util.LruCache;
import android.widget.ImageView;

import androidx.core.graphics.BitmapCompat;

import java.io.InputStream;

public class ContactImageLoader {

    /**
     * A LruCache used to store images which has a maximum size of 10% of the maximum heap size.
     */
    private static final BitmapCache CACHE = new BitmapCache(
            Math.round(Runtime.getRuntime().maxMemory() / 10));

    private ContactImageLoader() {
    }

    interface Listener {
        void onImageLoaded(Bitmap bitmap);
    }

    public static void loadMediaStoreThumbnail(final ContentResolver context,
                                        final ImageView imageView,
                                        final String id,
                                        final Listener listener) {

        final Bitmap cachedValue = CACHE.get(id);

        imageView.setTag(id);

        if (cachedValue != null) {
            // If the image is already in the cache, display the image,
            // call the listener now and return
            imageView.setImageBitmap(cachedValue);
            if (listener != null) {
                listener.onImageLoaded(cachedValue);
            }
            return;
        }

        new AsyncTask<Void, Void, Bitmap>() {
            @Override
            protected Bitmap doInBackground(Void... params) {

                String s = fetchContactIdFromPhoneNumber(context, id);

                if (!TextUtils.isEmpty(s)){

                    Uri photoUri = getPhotoUri(context, Long.valueOf(s));

                    Log.e("hujian", "doInBackground: "+photoUri );

                    Bitmap bitmap=null;
                    if (photoUri!=null){
                        InputStream input = ContactsContract.Contacts.openContactPhotoInputStream(context, photoUri);
                        bitmap = BitmapFactory.decodeStream(input);
                    }

                    return bitmap;

                }

                return null;
            }

            @Override
            protected void onPostExecute(Bitmap bitmap) {

                if (bitmap!=null){
                    if (imageView.getTag() != null && imageView.getTag().equals(id)) {
                        imageView.setImageBitmap(bitmap);
                    }
                    if (bitmap != null) {
                        // Add the image to the memory cache first
                        CACHE.put(id, bitmap);

                        if (listener != null) {
                            listener.onImageLoaded(bitmap);
                        }
                    }
                }
            }
        }.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }


    public static String fetchContactIdFromPhoneNumber(ContentResolver contentResolver,String phoneNumber) {
        Uri uri = Uri.withAppendedPath(ContactsContract.PhoneLookup.CONTENT_FILTER_URI,
                Uri.encode(phoneNumber));
        Cursor cursor =contentResolver.query(uri,
                new String[] { ContactsContract.PhoneLookup.DISPLAY_NAME, ContactsContract.PhoneLookup._ID },
                null, null, null);

        String contactId = "";

        if (cursor.moveToFirst()) {
            do {
                contactId = cursor.getString(cursor
                        .getColumnIndex(ContactsContract.PhoneLookup._ID));
            } while (cursor.moveToNext());
        }

        return contactId;
    }

    public static Uri getPhotoUri(ContentResolver contentResolver,long contactId) {
        try {
            Cursor cursor = contentResolver
                    .query(ContactsContract.Data.CONTENT_URI,
                            null,
                            ContactsContract.Data.CONTACT_ID
                                    + "="
                                    + contactId
                                    + " AND "

                                    + ContactsContract.Data.MIMETYPE
                                    + "='"
                                    + ContactsContract.CommonDataKinds.Photo.CONTENT_ITEM_TYPE
                                    + "'", null, null);

            if (cursor != null) {
                if (!cursor.moveToFirst()) {
                    return null; // no photo
                }
            } else {
                return null; // error in cursor process
            }

        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }

        Uri person = ContentUris.withAppendedId(ContactsContract.Contacts.CONTENT_URI, contactId);
        return person;
    }


    public static Bitmap decodeSampleBitmapFromInputStream(InputStream inputStream,int reqWidth,int reqHeight){

        final BitmapFactory.Options options=new BitmapFactory.Options();

        options.inJustDecodeBounds=true;

        BitmapFactory.decodeStream(inputStream,null,options);

        options.inSampleSize =calculateSampleSize(options,reqWidth,reqHeight);

        options.inJustDecodeBounds=false;

        return BitmapFactory.decodeStream(inputStream,null,options);

    }

    private static int calculateSampleSize(BitmapFactory.Options options, int reqwidth, int reqHight) {

        if (reqHight == 0 || reqHight == 0) {

            return 1;

        }

        final int height = options.outHeight;

        final int width = options.outWidth;

        int inSampleSize = 1;

        if (height > reqHight || width > reqHight) {

            final int halfHeight = height / 2;

            final int halfWidth = width / 2;

            while (halfHeight / inSampleSize >= reqHight && halfWidth / inSampleSize >= reqwidth) {

                inSampleSize *= 2;

            }

        }

        return inSampleSize;

    }

    /**
     * A simple cache implementation for {@link android.graphics.Bitmap} instances which uses
     * {@link android.support.v4.util.LruCache}.
     */
    private static class BitmapCache extends LruCache<String, Bitmap> {
        BitmapCache(int maxSize) {
            super(maxSize);
        }

        @Override
        protected int sizeOf(String key, Bitmap value) {
            return BitmapCompat.getAllocationByteCount(value);
        }
    }
}
