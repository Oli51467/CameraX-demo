package com.sdu.camerax.utils;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageFormat;
import android.graphics.Rect;
import android.graphics.YuvImage;
import android.media.Image;
import android.os.Environment;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.camera.core.ImageProxy;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.nio.ByteBuffer;

public class ImgHelper {
    public static String TAG = "rfDevImg";

    // 获取到YuvImage对象 然后存文件
    public static void useYuvImgSaveFile(ImageProxy imageProxy) {
        final int wid = imageProxy.getWidth();
        final int height = imageProxy.getHeight();
        Log.d(TAG, "宽高: " + wid + ", " + height);

        YuvImage yImg = ImgHelper.toYuvImage(imageProxy);
        File yFile = new File(Environment.getExternalStorageDirectory() + "/recoder");
        if (!yFile.exists()) yFile.mkdir();
        yFile = new File(yFile, "y_" + System.currentTimeMillis() + ".png");
        saveYuvToFile(yFile, wid, height, yImg);
        Log.d(TAG, "rustfisher.com 存储了" + yFile);
    }

    public static YuvImage toYuvImage(ImageProxy image) {
        if (image.getFormat() != ImageFormat.YUV_420_888) {
            throw new IllegalArgumentException("Invalid image format");
        }
        int width = image.getWidth();
        int height = image.getHeight();

        // 拿到YUV数据
        ByteBuffer yBuffer = image.getPlanes()[0].getBuffer();
        ByteBuffer uBuffer = image.getPlanes()[1].getBuffer();
        ByteBuffer vBuffer = image.getPlanes()[2].getBuffer();

        int numPixels = (int) (width * height * 1.5f);
        byte[] nv21 = new byte[numPixels]; // 转换后的数据
        int index = 0;

        // 复制Y的数据
        int yRowStride = image.getPlanes()[0].getRowStride();
        int yPixelStride = image.getPlanes()[0].getPixelStride();
        for (int y = 0; y < height; ++y) {
            for (int x = 0; x < width; ++x) {
                nv21[index++] = yBuffer.get(y * yRowStride + x * yPixelStride);
            }
        }

        // 复制U/V数据
        int uvRowStride = image.getPlanes()[1].getRowStride();
        int uvPixelStride = image.getPlanes()[1].getPixelStride();
        int uvWidth = width / 2;
        int uvHeight = height / 2;

        for (int y = 0; y < uvHeight; ++y) {
            for (int x = 0; x < uvWidth; ++x) {
                int bufferIndex = (y * uvRowStride) + (x * uvPixelStride);
                nv21[index++] = vBuffer.get(bufferIndex);
                nv21[index++] = uBuffer.get(bufferIndex);
            }
        }
        return new YuvImage(nv21, ImageFormat.NV21, width, height, null);
    }

    public static void saveYuvToFile(File file, int wid, int height, YuvImage yuvImage) {
        try {
            boolean c = file.createNewFile();
            Log.d(TAG, file + " created: " + c);
            FileOutputStream fos = new FileOutputStream(file);
            yuvImage.compressToJpeg(new Rect(0, 0, wid, height), 100, fos);
            fos.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static Bitmap toBitMap(Image image) {
        Image.Plane[] planes = image.getPlanes();
        ByteBuffer yBuffer = planes[0].getBuffer();
        ByteBuffer uBuffer = planes[1].getBuffer();
        ByteBuffer vBuffer = planes[2].getBuffer();

        int ySize = yBuffer.remaining();
        int uSize = uBuffer.remaining();
        int vSize = vBuffer.remaining();

        byte[] nv21 = new byte[ySize + uSize + vSize];
        //转换UV
        yBuffer.get(nv21, 0, ySize);
        vBuffer.get(nv21, ySize, vSize);
        uBuffer.get(nv21, ySize + vSize, uSize);

        YuvImage yuvImage = new YuvImage(nv21, ImageFormat.NV21, image.getWidth(), image.getHeight(), null);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        yuvImage.compressToJpeg(new Rect(0, 0, yuvImage.getWidth(), yuvImage.getHeight()), 75, out);

        byte[] imageBytes = out.toByteArray();
        return BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.length);
    }

    public static byte[] JPEGImageToByteArray(Image image) {
        ByteBuffer buffer = image.getPlanes()[0].getBuffer();
        byte[] bytes = new byte[buffer.capacity()];
        buffer.get(bytes);
        return bytes;
    }
}
