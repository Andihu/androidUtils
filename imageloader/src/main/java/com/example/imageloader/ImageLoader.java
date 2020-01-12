package com.example.imageloader;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Build;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.StatFs;
import android.util.Log;
import android.util.LruCache;
import android.widget.ImageView;

import androidx.annotation.NonNull;

import com.jakewharton.disklrucache.DiskLruCache;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.concurrent.Executor;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public class ImageLoader {

    private static final String TAG = "ImageLoader";

    private static final int DISK_CACHE_SIZE = 1024 * 1024 * 50;

    public static final int DISD_CACHE_INDEX = 0;

    public static final int CPU_COUNT = Runtime.getRuntime().availableProcessors();

    private static final int CORE_POOL_SIZE = CPU_COUNT + 1;

    private static final int MAXIMUM_POOL_SIZE = CPU_COUNT * 2 + 1;

    private static final long KEEP_ALIVE = 10L;

    private static final int MESSAGE_POST_RESULT = 1;

    private static final int TAG_KEY_URI = R.id.image;

    private static final int IO_BUFFER_SIZE = 8 * 1024;

    private boolean mIsDiskLruCacheCreated = false;

    private Context mContext;

    private LruCache<String, Bitmap> mMemoryCache;

    private DiskLruCache mDisKLruCache;

    private ImageResizer mImageResizer;

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

            String uri = (String) imageView.getTag(TAG_KEY_URI);

            if (uri.equals(result.uri)) {

                imageView.setImageBitmap(result.bitmap);

            } else {

                Log.w(TAG, "handleMessage: set imge bitmap ,but uri has change ,ignored!");

            }

        }
    };


    private ImageLoader(Context context) {

        mContext = context.getApplicationContext();

        int maxMemory = (int) (Runtime.getRuntime().maxMemory() / 1024);

        int cacheSize = maxMemory / 8;

        mMemoryCache = new LruCache<String, Bitmap>(cacheSize) {

            @Override
            protected int sizeOf(String key, Bitmap value) {

                return value.getRowBytes() * value.getHeight() / 1024;
            }
        };

        File diskCacheDir = getDiskCacheDir(mContext, "bitmap");

        if (!diskCacheDir.exists()) {

            diskCacheDir.mkdirs();

        }

        if (getUsableSpace(diskCacheDir) > DISK_CACHE_SIZE) {

            try {

                mDisKLruCache = DiskLruCache.open(diskCacheDir, 1, 1, DISK_CACHE_SIZE);

                mIsDiskLruCacheCreated = true;

            } catch (IOException e) {

                e.printStackTrace();

            }

        }

    }

    public static ImageLoader builder(Context context) {

        return new ImageLoader(context);
    }

    private void addBitmaToMemoryChache(String key, Bitmap bitmap) {

        if (getBitmapFromMemChache(key) == null) {

            mMemoryCache.put(key, bitmap);

        }

    }

    public Bitmap getBitmapFromMemChache(String key) {

        return mMemoryCache.get(key);
    }

    public void bindBitmap(final String uri, final ImageView imageView) {

        bindBitmap(uri, imageView, 0, 0);
    }

    public void bindBitmap(final String uri, final ImageView imageView, final int reqWidth, final int reqHeight) {

        Bitmap bitmap = loadBitmapFromMemCache(uri);

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

    private Bitmap loadBitmap(String uri, int reqWidth, int reqHeight) {

        Bitmap bitmap = loadBitmapFromMemCache(uri);

        if (bitmap != null) {

            return bitmap;

        }

        try {

            bitmap = loadBitmapFromDiskCache(uri, reqWidth, reqHeight);

            if (bitmap != null) {

                return bitmap;
            }

            bitmap = loadBitmapFromHttp(uri, reqWidth, reqHeight);

        } catch (IOException e) {

            e.printStackTrace();

        }
        if (bitmap == null && !mIsDiskLruCacheCreated) {

            bitmap = downBitmapFromUri(uri);

        }

        return bitmap;


    }

    private Bitmap downBitmapFromUri(String uri) {

        Bitmap bitmap = null;

        HttpURLConnection urlConnection = null;

        BufferedInputStream in = null;

        try {
            final URL url = new URL(uri);

            urlConnection = (HttpURLConnection) url.openConnection();

            in = new BufferedInputStream(urlConnection.getInputStream(), IO_BUFFER_SIZE);

            bitmap = BitmapFactory.decodeStream(in);

        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (urlConnection != null) {

                urlConnection.disconnect();
            }

            try {
                in.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return bitmap;

    }

    private Bitmap loadBitmapFromMemCache(String uri) {

        final String key = hashKeyFromUrl(uri);

        Bitmap bitmap = getBitmapFromMemChache(uri);

        return bitmap;
    }

    private Bitmap loadBitmapFromHttp(String url, int reqWidth, int reqHeight) throws IOException {

        if (Looper.myLooper() == Looper.getMainLooper()) {

            throw new RuntimeException("can not visit networ from UI Thread");

        }

        if (mDisKLruCache == null) {

            return null;
        }

        String key = hashKeyFromUrl(url);

        DiskLruCache.Editor edit = mDisKLruCache.edit(key);

        if (edit != null) {

            OutputStream outputStream = edit.newOutputStream(DISD_CACHE_INDEX);

            if (downloadUrlToStream(url, outputStream)) {
                edit.commit();
            } else {

                edit.abort();

            }
            mDisKLruCache.flush();

        }

        return loadBitmapFromDiskCache(url, reqWidth, reqHeight);
    }

    private Bitmap loadBitmapFromDiskCache(String url, int reqWidth, int reqHeight) throws IOException {

        if (Looper.myLooper() == Looper.getMainLooper()) {

            throw new RuntimeException("can not visit networ from UI Thread");

        }

        if (mDisKLruCache == null) {

            return null;
        }

        Bitmap bitmap = null;

        String key = hashKeyFromUrl(url);

        DiskLruCache.Snapshot snapshot = mDisKLruCache.get(key);

        if (snapshot == null) {
            return null;
        }
        FileInputStream fileInputStream = (FileInputStream) snapshot.getInputStream(DISD_CACHE_INDEX);

        FileDescriptor fileDescriptor = fileInputStream.getFD();

        bitmap = mImageResizer.decodeSamlpeBitmapFromFileDescriptor(fileDescriptor, reqWidth, reqHeight);

        if (bitmap != null) {

            addBitmaToMemoryChache(key, bitmap);
        }

        return bitmap;

    }


    private boolean downloadUrlToStream(String urlString, OutputStream outputStream) {

        HttpURLConnection urlConnection = null;

        BufferedOutputStream out = null;

        BufferedInputStream in = null;

        try {

            final URL url = new URL(urlString);

            urlConnection = (HttpURLConnection) url.openConnection();

            in = new BufferedInputStream(urlConnection.getInputStream(), IO_BUFFER_SIZE);

            out = new BufferedOutputStream(outputStream, IO_BUFFER_SIZE);

            int b;

            while ((b = in.read()) != -1) {

                out.write(b);

            }
            return true;

        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (urlConnection != null) {
                urlConnection.disconnect();
            }
            try {
                out.close();
                in.close();
            } catch (IOException e) {
                e.printStackTrace();
            }

        }
        return false;

    }

    private String hashKeyFromUrl(String url) {
        String cacheKey;

        try {
            final MessageDigest messageDigest = MessageDigest.getInstance("MD5");

            messageDigest.update(url.getBytes());

            cacheKey = byteToHexString(messageDigest.digest());

        } catch (NoSuchAlgorithmException e) {

            cacheKey = String.valueOf(url.hashCode());

            e.printStackTrace();
        }

        return cacheKey;
    }

    private String byteToHexString(byte[] digest) {

        StringBuilder sb = new StringBuilder();

        for (int i = 0; i < digest.length; i++) {

            String hex = Integer.toHexString(0xFF & digest[i]);

            if (hex.length() == 1) {
                sb.append('0');
            }

            sb.append(hex);
        }

        return sb.toString();

    }

    @SuppressLint("ObsoleteSdkInt")
    private Long getUsableSpace(File diskCacheDir) {

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.GINGERBREAD) {
            return diskCacheDir.getUsableSpace();
        }
        final StatFs statFs = new StatFs(diskCacheDir.getPath());

        return (long) statFs.getBlockSize() * statFs.getAvailableBlocks();

    }

    private File getDiskCacheDir(Context mContext, String bitmap) {

        boolean available = Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED);

        final String cachePath;

        if (available) {

            cachePath = mContext.getExternalCacheDir().getPath();

        } else {
            cachePath = mContext.getCacheDir().getPath();
        }

        return new File(cachePath + File.separator + bitmap);
    }

    private static class LoaderResult {

        public ImageView imageView;

        public String uri;

        public Bitmap bitmap;

        public LoaderResult(ImageView imageView, String uri, Bitmap bitmap) {

            this.imageView = imageView;

            this.uri = uri;

            this.bitmap = bitmap;
        }
    }
}
