package com.team8020.opencvapplication1;


import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

//this methods helps you to decrease images for better performance
public class ImageHelper {

    public static int calculateInSampleSize(BitmapFactory.Options options, int reqWidth, int reqHeight)
    {
        final int height = options.outHeight;
        final int width = options.outWidth;
        int inSampleSize = 1;

        if (height > reqWidth || width > reqWidth)
        {
            final int halfHeight = height / 2;
            final int halfWidth = width / 2;

            while ((halfHeight / inSampleSize) >= reqHeight && (halfWidth / inSampleSize) >= reqWidth)
            {
                inSampleSize *= 2;
            }
        }

        return  inSampleSize;
    }

    public static Bitmap decodeSampleBitmapFromPath(String pathName, int reqwidth, int reqHeight)
    {
        final BitmapFactory.Options options = new BitmapFactory.Options();
        BitmapFactory.decodeFile(pathName, options);

        options.inSampleSize = calculateInSampleSize(options, reqwidth, reqHeight);

        options.inJustDecodeBounds = false;
        return BitmapFactory.decodeFile(pathName, options);
    }


}
