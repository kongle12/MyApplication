package com.kong.greentea.application.util;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.media.ExifInterface;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by zione on 2015/12/14.
 */
public class BitmapUtil {

    public static void recycleImg(Bitmap map){
        if(map != null && !map.isRecycled()){
            map.recycle();
        }
    }
}
