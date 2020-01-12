package com.example.imageloader;

import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.provider.ContactsContract;
import android.text.TextUtils;
import android.util.Log;
import android.util.LruCache;
import android.widget.ImageView;

import androidx.annotation.NonNull;

import java.io.InputStream;
import java.util.concurrent.Executor;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public class ContactLoader {

    private static final String TAG = "ContactLoader";

    private Context mContext;

    LruCache<String, Bitmap> mContactImageMemCache;

    public static final int CPU_COUNT = Runtime.getRuntime().availableProcessors();

    private static final int TAG_KEY_URI = 11;

    private static final int MESSAGE_POST_RESULT = 1;

    private static final long KEEP_ALIVE = 10L;

    private static final int CORE_POOL_SIZE = CPU_COUNT + 1;

    private static final int MAXIMUM_POOL_SIZE = CPU_COUNT * 2 + 1;

    private static final ThreadFactory sThreadFactory = new ThreadFactory() {

        private final AtomicInteger mCount = new AtomicInteger(1);

        @Override
        public Thread newThread(Runnable r) {

            return new Thread(r, "ImageLoader#" + mCount.getAndIncrement());
        }
    };

    public static final Executor THREAD_POOL_EXEXUTOR = new ThreadPoolExecutor(CORE_POOL_SIZE, MAXIMUM_POOL_SIZE, KEEP_ALIVE, TimeUnit.SECONDS, new LinkedBlockingDeque<Runnable>(), sThreadFactory);


    private Handler mMainHandler = new Handler(Looper.getMainLooper()) {

        @Override
        public void handleMessage(@NonNull Message msg) {

            super.handleMessage(msg);

            LoaderResult result = (LoaderResult) msg.obj;

            ImageView imageView = result.imageView;

            String phone = (String) imageView.getTag(TAG_KEY_URI);

            if (phone.equals(result.phone)) {

                imageView.setImageBitmap(result.bitmap);

            } else {

                Log.w(TAG, "handleMessage: set imge bitmap ,but uri has change ,ignored!");

            }

        }
    };

    private ContactLoader(Context context) {

        mContext = context.getApplicationContext();

        int maxMemory = (int) (Runtime.getRuntime().maxMemory() / 1024);

        int cacheSize = maxMemory / 8;

        mContactImageMemCache = new LruCache<String, Bitmap>(cacheSize) {

            @Override
            protected int sizeOf(String key, Bitmap value) {

                return value.getRowBytes() * value.getHeight() / 1024;
            }
        };

    }

    public void bindBitmap(final String uri, final ImageView imageView) {

        bindBitmap(uri, imageView, 0, 0);
    }

    public void bindBitmap(final String uri, final ImageView imageView, final int reqWidth, final int reqHeight) {

        Bitmap bitmap = getBitmapFromMemChache(uri);

        if (bitmap != null) {

            imageView.setImageBitmap(bitmap);

            return;

        }

        Runnable loadBitmapTask = new Runnable() {
            @Override
            public void run() {

                Bitmap bitmap = loadBitmap(uri, reqWidth, reqHeight);

                if (bitmap != null) {

                    LoaderResult result = new LoaderResult(imageView, uri, bitmap);

                    mMainHandler.obtainMessage(MESSAGE_POST_RESULT, result).sendToTarget();

                }
            }
        };

        THREAD_POOL_EXEXUTOR.execute(loadBitmapTask);

    }

    private Bitmap loadBitmap(String phone, int reqWidth, int reqHeight) {

        Bitmap bitmap = getBitmapFromMemChache(phone);

        if (bitmap != null) {

            return bitmap;

        }

        bitmap = loadBitmapFromDatabase(phone, reqWidth, reqHeight);

        return bitmap;


    }

    private Bitmap loadBitmapFromDatabase(String phoneNumber, int reqWidth, int reqHeight) {

        Bitmap bitmap=null;

        String phone = phoneNumber;

        String phone1 = new StringBuffer(phone.subSequence(0, 3)).append(" ").append(phone.substring(3, 7)).append(" ").append(phone.substring(7, 11)).toString();

        String phone2 = new StringBuffer(phone.subSequence(0, 3)).append("-").append(phone.substring(3, 7)).append("-").append(phone.substring(7, 11)).toString();

        String phone3 = "+86 " + phone1;

        String phone4 = "+86 " + phone2;

        String[] projection = {ContactsContract.CommonDataKinds.Phone.CONTACT_ID};

        String selection = ContactsContract.CommonDataKinds.Phone.NUMBER + " in(?,?,?,?,?) ";

        String[] selectionArg = new String[]{phone, phone1, phone2, phone3, phone4};

        Cursor cursor = mContext.getContentResolver().query(ContactsContract.CommonDataKinds.Phone.CONTENT_URI, projection, selection, selectionArg, null, null);

        if (cursor != null&&cursor.moveToFirst()&&cursor.getCount()>0) {

            int contactIdColumnIndex = cursor.getColumnIndex(ContactsContract.PhoneLookup.CONTACT_ID);

            if (contactIdColumnIndex != -1) {

                String id = cursor.getString(contactIdColumnIndex);

                if (!TextUtils.isEmpty(id)) {

                    Uri uri = Uri.withAppendedPath(ContactsContract.Contacts.CONTENT_URI, id);

                    InputStream is = ContactsContract.Contacts.openContactPhotoInputStream(mContext.getContentResolver(), uri);

                    bitmap = decodeSampleBitmapFromInputStream(is,reqWidth,reqHeight);

                    if (bitmap != null) {

                        addBitmaToMemoryChache(phone, bitmap);
                    }

                }
            }

            cursor.close();

        }

        return bitmap;

    }


    public static ContactLoader builder(Context context) {

        return new ContactLoader(context);
    }

    public Bitmap getBitmapFromMemChache(String key) {

        return mContactImageMemCache.get(key);
    }

    private void addBitmaToMemoryChache(String key, Bitmap bitmap) {

        if (getBitmapFromMemChache(key) == null) {

            mContactImageMemCache.put(key, bitmap);

        }

    }

    public Bitmap decodeSampleBitmapFromInputStream(InputStream inputStream,int reqWidth,int reqHeight){

        final BitmapFactory.Options options=new BitmapFactory.Options();

        options.inJustDecodeBounds=true;

        BitmapFactory.decodeStream(inputStream,null,options);

        options.inSampleSize =calculateSampleSize(options,reqWidth,reqHeight);

        options.inJustDecodeBounds=false;

        return BitmapFactory.decodeStream(inputStream,null,options);

    }

    private int calculateSampleSize(BitmapFactory.Options options, int reqwidth, int reqHight) {

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

    private static class LoaderResult {

        public ImageView imageView;

        public String phone;

        public Bitmap bitmap;

        public LoaderResult(ImageView imageView, String phoneNumber, Bitmap bitmap) {

            this.imageView = imageView;

            this.phone = phoneNumber;

            this.bitmap = bitmap;
        }
    }


}
