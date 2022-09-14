package com.sdu.camerax.utils;

import android.graphics.Bitmap;
import android.os.Environment;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

public class FileUtil {
    /**
     * 保存图像信息
     * @param bitmap 要保存的Bitmap
     * @param fileName 文件名
     */
    public static void savePNG_After(Bitmap bitmap, String fileName) {
        File file = new File(Environment.getExternalStorageDirectory() + "/recoder");
        if (!file.exists()) file.mkdirs();
        file = new File(file + File.separator, fileName + ".png");
        try {
            FileOutputStream out = new FileOutputStream(file);
            if (bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)) {
                out.flush();
                out.close();
            }
        }
        catch (IOException e) {
            e.printStackTrace();
        }
    }
}
