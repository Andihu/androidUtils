package com.example.imageloader;

import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Rect;
import android.net.Uri;
import android.os.Parcel;
import android.os.Parcelable;
import android.provider.ContactsContract;
import android.text.TextUtils;
import android.util.LruCache;


import java.io.InputStream;

/**
 * Copyright (C), 2015-2019
 * FileName: ContactRepository
 * Author: hujian
 * Date: 2019/12/30 12:23
 * History:
 * <author> <time> <version> <desc>
 */
public class ContactRepository {

    private static final String TAG = "ContactRepository";

    private static final String[] CONTACT_ID_PROJECTION = new String[]{

            ContactsContract.PhoneLookup.DISPLAY_NAME,

            ContactsContract.PhoneLookup.TYPE,

            ContactsContract.PhoneLookup.LABEL,

            ContactsContract.PhoneLookup._ID};

    private static LruCache<String, Bitmap> sContactPhotoNumberCache;

    private static LruCache<String, ContactBean> uContactPhotoNumbercache;

    public static Bitmap getContactPhotoFromNumber(ContentResolver contentResolver, String number) {

        if (number == null) {

            return null;
        }

        Bitmap photo = getCachedContactPhotoFromNumber(number);

        if (photo != null) {

            return photo;
        }

        int id = getContactIdFromNumber(contentResolver, number);

        if (id == 0) {

            return null;
        }

        photo = getContactPhotoFromId(contentResolver, id);

        if (photo != null) {

            sContactPhotoNumberCache.put(number, photo);

        }

        return photo;
    }


    public static Bitmap getContactPhotoFromId(ContentResolver contentResolver, long id) {

        Uri photoUri = ContentUris.withAppendedId(ContactsContract.Contacts.CONTENT_URI, id);

        InputStream photoDataStream = ContactsContract.Contacts.openContactPhotoInputStream(contentResolver, photoUri, true);

        BitmapFactory.Options options = new BitmapFactory.Options();

        options.inPreferQualityOverSpeed = true;
        // Scaling will be handled by later. We shouldn't scale multiple times to avoid
        // quality lost due to multiple potential scaling up and down.
        options.inScaled = false;

        Rect nullPadding = null;

        Bitmap photo = BitmapFactory.decodeStream(photoDataStream, nullPadding, options);

        if (photo != null) {

            photo.setDensity(Bitmap.DENSITY_NONE);
        }

        return photo;
    }


    private static String getContactNameFromNumber(ContentResolver cr, String number) {

        Uri uri = Uri.withAppendedPath(ContactsContract.PhoneLookup.CONTENT_FILTER_URI, Uri.encode(number));

        Cursor cursor = null;

        String name = null;

        try {
            cursor = cr.query(uri, new String[]{ContactsContract.PhoneLookup.DISPLAY_NAME}, null, null, null);

            if (cursor != null && cursor.moveToFirst()) {

                name = cursor.getString(0);
            }

        } finally {

            if (cursor != null) {

                cursor.close();

            }
        }

        return name;
    }


    public static int getContactIdFromNumber(ContentResolver cr, String number) {

        if (number == null || number.isEmpty()) {

            return 0;
        }

        Uri uri = Uri.withAppendedPath(ContactsContract.PhoneLookup.CONTENT_FILTER_URI, Uri.encode(number));

        Cursor cursor = cr.query(uri, CONTACT_ID_PROJECTION, null, null, null);

        try {
            if (cursor != null && cursor.moveToFirst()) {

                int id = cursor.getInt(cursor.getColumnIndex(ContactsContract.PhoneLookup._ID));

                return id;
            }
        } finally {

            if (cursor != null) {

                cursor.close();

            }
        }
        return 0;
    }

    public static Bitmap getCachedContactPhotoFromNumber(String number) {

        if (number == null) {

            return null;
        }

        if (sContactPhotoNumberCache == null) {

            sContactPhotoNumberCache = new LruCache<String, Bitmap>(4194304 /** 4MB **/) {

                @Override protected int sizeOf(String key, Bitmap value) {

                    return value.getByteCount();

                }

            };

        } else if (sContactPhotoNumberCache.get(number) != null) {

            return sContactPhotoNumberCache.get(number);

        }

        return null;
    }

    public static ContactBean getCachedContactFromNumber(String number) {

        if (number == null || TextUtils.isEmpty(number)) {

            return null;

        }

        if (uContactPhotoNumbercache == null) {

            uContactPhotoNumbercache = new LruCache<String, ContactBean>(4194304 /** 4MB **/) {

                @Override protected int sizeOf(String key, ContactBean value) {

                    return value.bitmap.getByteCount();

                }

            };

        } else if (uContactPhotoNumbercache.get(number) != null) {

            return uContactPhotoNumbercache.get(number);

        }

        return null;

    }

    public static String getContactNameByPhoneNumber(Context context, String phoneNum) {

        String phone = formatNumber(phoneNum);

        String phone1 = new StringBuffer(phone.subSequence(0, 3)).append(" ").append(phone.substring(3, 7)).append(" ").append(phone.substring(7, 11)).toString();

        String phone2 = new StringBuffer(phone.subSequence(0, 3)).append("-").append(phone.substring(3, 7)).append("-").append(phone.substring(7, 11)).toString();

        String phone3 = "+86 " + phone1;

        String phone4 = "+86 " + phone2;

        String[] projection = {ContactsContract.PhoneLookup.DISPLAY_NAME, ContactsContract.CommonDataKinds.Phone.NUMBER, ContactsContract.CommonDataKinds.Phone.CONTACT_ID};

        String selection = ContactsContract.CommonDataKinds.Phone.NUMBER + " in(?,?,?,?,?) ";

        String[] selectionArg = new String[]{phone, phone1, phone2, phone3, phone4};

        Cursor cursor = context.getContentResolver().query(ContactsContract.CommonDataKinds.Phone.CONTENT_URI, projection, selection, selectionArg, null, null);

        if (cursor != null && cursor.moveToFirst()) {

            String disPlayName = null;

            do {

                int nameFieldColumnIndex = cursor.getColumnIndex(ContactsContract.PhoneLookup.DISPLAY_NAME);

                if (nameFieldColumnIndex != -1) {

                    String name = cursor.getString(nameFieldColumnIndex);

                    if (!TextUtils.isEmpty(name)) {

                        disPlayName = name;
                    }

                }

            } while (cursor.moveToNext());

            cursor.close();


            return disPlayName;
        }
        return null;
    }

    private static String formatNumber(String phoneNumber) {

        return phoneNumber.replace("+86", "").replace(" ", "").replace("-", "");

    }

    public static ContactBean getContactNameAndPhotoByPhoneNum(Context context, String phoneNum) {


        if (phoneNum == null || TextUtils.isEmpty(phoneNum)) {

            return null;
        }

        ContactBean contact = getCachedContactFromNumber(phoneNum);

        if (contact != null) {

            return contact;
        }

        String phone = phoneNum;

        String phone1 = new StringBuffer(phone.subSequence(0, 3)).append(" ").append(phone.substring(3, 7)).append(" ").append(phone.substring(7, 11)).toString();

        String phone2 = new StringBuffer(phone.subSequence(0, 3)).append("-").append(phone.substring(3, 7)).append("-").append(phone.substring(7, 11)).toString();

        String phone3 = "+86 " + phone1;

        String phone4 = "+86 " + phone2;

        String[] projection = {ContactsContract.PhoneLookup.DISPLAY_NAME, ContactsContract.CommonDataKinds.Phone.NUMBER, ContactsContract.CommonDataKinds.Phone.CONTACT_ID};

        String selection = ContactsContract.CommonDataKinds.Phone.NUMBER + " in(?,?,?,?,?) ";

        String[] selectionArg = new String[]{phone, phone1, phone2, phone3, phone4};

        Cursor cursor = context.getContentResolver().query(ContactsContract.CommonDataKinds.Phone.CONTENT_URI, projection, selection, selectionArg, null, null);

        String name = null;

        Bitmap bitmap = null;

        if (cursor != null&&cursor.moveToFirst()&&cursor.getCount()>0) {

                int nameFieldColumnIndex = cursor.getColumnIndex(ContactsContract.PhoneLookup.DISPLAY_NAME);

                int contactIdColumnIndex = cursor.getColumnIndex(ContactsContract.PhoneLookup.CONTACT_ID);

                if (nameFieldColumnIndex != -1 && contactIdColumnIndex != -1) {

                    String id = cursor.getString(contactIdColumnIndex);

                    name = cursor.getString(nameFieldColumnIndex);

                    if (!TextUtils.isEmpty(id)) {

                        bitmap = getBitmap(context, id);

                    }
                }

            cursor.close();

        }

        ContactBean contactBean = new ContactBean(phone, name, bitmap);

        if (bitmap!=null){

            uContactPhotoNumbercache.put(phoneNum, contactBean);

        }

        return contactBean;
    }

    public static Bitmap getBitmap(Context context, String id) {

        ContentResolver cr = context.getContentResolver();

        Uri uri = Uri.withAppendedPath(ContactsContract.Contacts.CONTENT_URI, id);

        InputStream is = ContactsContract.Contacts.openContactPhotoInputStream(cr, uri);

        return BitmapFactory.decodeStream(is);
    }

    public static Uri getPhotoUri(Context context, String id) {

        ContentResolver cr = context.getContentResolver();

        return Uri.withAppendedPath(ContactsContract.Contacts.CONTENT_URI, id);
    }

    public static class ContactBean implements Parcelable {

        public ContactBean(String phoneNumber, String disPlayName, Bitmap bitmap) {


            this.phoneNumber = phoneNumber;

            this.disPlayName = disPlayName;

            this.bitmap = bitmap;

        }


        public String phoneNumber;

        public String disPlayName;

        public Bitmap bitmap;

        @Override public int describeContents() {
            return 0;
        }

        @Override public void writeToParcel(Parcel dest, int flags) {
            dest.writeString(this.phoneNumber);
            dest.writeString(this.disPlayName);
            dest.writeParcelable(this.bitmap, flags);
        }

        protected ContactBean(Parcel in) {
            this.phoneNumber = in.readString();
            this.disPlayName = in.readString();
            this.bitmap = in.readParcelable(Bitmap.class.getClassLoader());
        }

        public static final Creator<ContactBean> CREATOR = new Creator<ContactBean>() {
            @Override public ContactBean createFromParcel(Parcel source) {
                return new ContactBean(source);
            }

            @Override public ContactBean[] newArray(int size) {
                return new ContactBean[size];
            }
        };
    }
}
