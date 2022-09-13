package com.sdu.camerax;

import android.annotation.SuppressLint;
import android.app.Application;
import android.os.AsyncTask;
import android.util.Log;

import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class MyApplication extends Application {
    public static final String TAG = MyApplication.class.getName();

    public static SqueezeNcnn squeezencnn;
    public MyTask initNcnn;
    public static boolean initNet = false;
    public static ThreadPoolExecutor threadPool;
    public static final int THREAD_NUM = 19;
    public static final int STONE_NUM = 361;

    @Override
    public void onCreate() {
        super.onCreate();
        // 初始化线程池 可复用
        threadPool = new ThreadPoolExecutor(THREAD_NUM, THREAD_NUM + 2, 10, TimeUnit.SECONDS,
                new LinkedBlockingQueue<>(STONE_NUM));
        // 初始化Ncnn
        initNcnn = new MyTask();
        //initNcnn.execute(squeezencnn);
        Log.d(TAG, "onCreate");
    }

    @SuppressLint("StaticFieldLeak")
    public class MyTask extends AsyncTask<SqueezeNcnn, Integer, Boolean> {

        @Override
        protected Boolean doInBackground(SqueezeNcnn... squeezeNcnns) {
            squeezencnn = new SqueezeNcnn();
            boolean ret_init = squeezencnn.Init(getAssets());
            if (!ret_init) {
                Log.e(TAG, "squeezencnn Init failed");
            } else {
                initNet = true;
            }
            return ret_init;
        }
    }
}
