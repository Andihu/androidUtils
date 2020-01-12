package com.example.imageloader;

import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import java.io.FileDescriptor;
import java.io.InputStream;

public class ImageResizer {

    private static final String TAG = "ImageResizer";

    public ImageResizer() {
    }

    public Bitmap decodeSampleBitmapFromResoure(Resources res, int resid, int reqwidth, int reqHight) {

        final BitmapFactory.Options options = new BitmapFactory.Options();

        options.inJustDecodeBounds = true;

        BitmapFactory.decodeResource(res, resid, options);

        options.inSampleSize = calculateSampleSize(options, reqwidth, reqHight);

        options.inJustDecodeBounds = false;

        return BitmapFactory.decodeResource(res, resid, options);

    }

    public Bitmap decodeSampleBitmapFromInputStream(InputStream inputStream,int reqWidth,int reqHeight){

        final BitmapFactory.Options options=new BitmapFactory.Options();

        options.inJustDecodeBounds=true;

        BitmapFactory.decodeStream(inputStream,null,options);

        options.inSampleSize =calculateSampleSize(options,reqWidth,reqHeight);

        options.inJustDecodeBounds=false;

        return BitmapFactory.decodeStream(inputStream,null,options);

    }

    public Bitmap decodeSamlpeBitmapFromFileDescriptor(FileDescriptor fileDescriptor,int reqWidth,int reqHeight){

        final BitmapFactory.Options options=new BitmapFactory.Options();

        options.inJustDecodeBounds=true;

        BitmapFactory.decodeFileDescriptor(fileDescriptor,null,options);

        options.inSampleSize =calculateSampleSize(options,reqWidth,reqHeight);

        options.inJustDecodeBounds=false;

        return BitmapFactory.decodeFileDescriptor(fileDescriptor,null,options);

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
}
