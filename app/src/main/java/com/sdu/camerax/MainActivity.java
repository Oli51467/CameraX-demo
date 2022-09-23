package com.sdu.camerax;

import com.chaquo.python.Kwarg;
import com.chaquo.python.PyObject;
import com.chaquo.python.android.AndroidPlatform;
import com.chaquo.python.Python;

import static com.sdu.camerax.utils.ImgHelper.JPEGImageToByteArray;

import android.annotation.SuppressLint;
import android.content.pm.PackageManager;
import android.media.Image;
import android.os.Bundle;
import android.util.Log;
import android.util.Pair;
import android.util.Size;
import android.view.Surface;
import android.view.View;
import android.view.WindowManager;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.AspectRatio;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.ImageCaptureException;
import androidx.camera.core.ImageProxy;
import androidx.camera.core.Preview;
import androidx.camera.core.impl.utils.executor.CameraXExecutors;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.common.util.concurrent.ListenableFuture;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    private final int REQUEST_CODE_PERMISSIONS = 101;
    private final String[] REQUIRED_PERMISSIONS = new String[]{"android.permission.CAMERA", "android.permission.WRITE_EXTERNAL_STORAGE"};

    public static final String Logger = "Gobot";
    public static List<Pair<Double, Double>> corners = new ArrayList<>();

    private PreviewView previewView;
    private ProcessCameraProvider cameraProvider;
    private ExecutorService mExecutorService; // 声明一个线程池对象
    private ListenableFuture<ProcessCameraProvider> cameraProviderFuture;
    private Python py;

    private final ImageCapture imageCapture = new ImageCapture.Builder()
            .setTargetAspectRatio(AspectRatio.RATIO_4_3)
            .setCaptureMode(ImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY)
            .setTargetRotation(Surface.ROTATION_0)
            .build();

    // 构建ImageAnalysis用例 可将分析器(图像使用方)连接到 CameraX(图像生成方)
    private final ImageAnalysis imageAnalysis =
            new ImageAnalysis.Builder()
                    .setTargetRotation(Surface.ROTATION_0) // 设置旋转角度
                    .setOutputImageRotationEnabled(true)   // 这里必须要加true 否则图片不会旋转
                    .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_YUV_420_888)  // 设置图像输出参数 RGBA_8888或YUY_420_888
                    .setTargetResolution(new Size(1280, 720))   // 设置宽高
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST) // 背压策略 生产者生产过快时的策略
                    .setTargetName("board")  // 目标名称 使用该参数调试
                    .build();
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initPython();
        initWindow();
        initViews();
        if (requestPermissions()) {
            startCamera();
        } else {
            ActivityCompat.requestPermissions(this, REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS);
        }
    }

    private void initWindow() {
        Objects.requireNonNull(getSupportActionBar()).hide();   // 去掉导航栏
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);   // 透明状态栏
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION);   // 透明导航栏
    }

    private void initViews() {
        previewView = findViewById(R.id.previewView);
        findViewById(R.id.btn_takePicture).setOnClickListener(this);
        findViewById(R.id.btn_release).setOnClickListener(this);
        mExecutorService = Executors.newSingleThreadExecutor(); // 创建一个单线程线程池
    }

    private boolean requestPermissions() {
        for (String permission : REQUIRED_PERMISSIONS) {
            if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                return false;
            }
        }
        return true;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_CODE_PERMISSIONS) {
            if (requestPermissions()) {
                startCamera();
            } else {
                Toast.makeText(this, "Permissions not granted by the user.", Toast.LENGTH_SHORT).show();
                finish();
            }
        }
    }

    @SuppressLint("RestrictedApi")
    public void startCamera() {
        cameraProviderFuture = ProcessCameraProvider.getInstance(this);
        // 检查CameraProvider的可用性
        cameraProviderFuture.addListener(() -> {
            try {
                // 创建CameraProvider
                cameraProvider = cameraProviderFuture.get();
                // 选择相机并绑定生命周期和用例
                bindPreview(cameraProvider);
            } catch (ExecutionException | InterruptedException e) {
                Log.e(Logger, e.getMessage());
            }
        }, ContextCompat.getMainExecutor(this));
    }

    @SuppressLint({"RestrictedApi", "UnsafeOptInUsageError"})
    void bindPreview(@NonNull ProcessCameraProvider cameraProvider) {
        cameraProvider.unbindAll();
        // 绑定Preview
        Preview preview = new Preview.Builder().build();
        // 指定所需的相机 LensFacing 选项
        CameraSelector cameraSelector = new CameraSelector.Builder()
                .requireLensFacing(CameraSelector.LENS_FACING_BACK)
                .build();
        // 将 Preview 连接到 PreviewView
        preview.setSurfaceProvider(previewView.getSurfaceProvider());

        /*将预览流渲染到目标 View 上
        PERFORMANCE 是默认模式。PreviewView 会使用 SurfaceView 显示视频串流，但在某些情况下会回退为使用 TextureView。
        SurfaceView 具有专用的绘图界面，该对象更有可能通过内部硬件合成器实现硬件叠加层，尤其是当预览视频上面没有其他界面元素（如按钮）时。
        通过使用硬件叠加层进行渲染，视频帧会避开 GPU 路径，从而能降低平台功耗并缩短延迟时间。*/
        previewView.setImplementationMode(PreviewView.ImplementationMode.PERFORMANCE);
        // 缩放类型
        previewView.setScaleType(PreviewView.ScaleType.FILL_CENTER);
        Executor executor = CameraXExecutors.highPriorityExecutor();
        // 创建分析器
        imageAnalysis.setAnalyzer(executor, imageProxy -> {
            Image image = imageProxy.getImage();
            assert image != null;
            // 在这里处理image帧
            imageProxy.close();
        });
        // 将所选相机和任意用例绑定到生命周期。
        cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageCapture, imageAnalysis);

    }

    /**
     * CameraX拍照
     */
    @Override
    @SuppressLint("UnsafeOptInUsageError")
    public void onClick(View v) {
        int vid = v.getId();
        if (vid == R.id.btn_takePicture) {
            imageCapture.takePicture(mExecutorService, new ImageCapture.OnImageCapturedCallback() {
                        @Override
                        public void onCaptureSuccess(@NonNull ImageProxy imageProxy) {
                            Image image = imageProxy.getImage();    // ImageProxy 转 Bitmap
                            assert image != null;
                            byte[] byteArray = JPEGImageToByteArray(image);   // 注意这里Image的格式是JPEG 不是YUV
                            // 以下是python处理的部分
                            PyObject obj = py.getModule("CCTProcess").callAttr("main", new Kwarg("byte_array", byteArray));
                            double[][] result = obj.toJava(double[][].class);
                            corners.clear();
                            // 处理CCTCode四个角
                            for (double[] doubles : result) {
                                for (int j = 0; j < doubles.length; j++) {
                                    if (j > 0) {
                                        Pair<Double, Double> pair = new Pair<>(doubles[j], doubles[j + 1]);
                                        corners.add(pair);
                                        break;
                                    } else {
                                        Log.d("djnxyxy", doubles[j] + "\n");
                                    }
                                }
                            }
                            // 使用完关闭
                            imageProxy.close();
                        }

                        @Override
                        public void onError(@NonNull ImageCaptureException exception) {
                            super.onError(exception);
                        }
                    }
            );
        } else if (vid == R.id.btn_release) {
            imageAnalysis.clearAnalyzer();
        }
    }

    // 初始化Python环境
    void initPython() {
        if (!Python.isStarted()) {
            Python.start(new AndroidPlatform(this));
        }
        py = Python.getInstance();
    }
}