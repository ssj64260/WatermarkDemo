package com.android.watermarkdemo.ui.activity;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.ImageFormat;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.TotalCaptureResult;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.media.ImageReader;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.util.Size;
import android.util.SparseIntArray;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.android.watermarkdemo.R;
import com.android.watermarkdemo.app.BaseActivity;
import com.android.watermarkdemo.config.Constants;
import com.android.watermarkdemo.utils.CameraUtils;
import com.android.watermarkdemo.utils.DateTimeUtils;
import com.android.watermarkdemo.utils.ImageUtils;
import com.android.watermarkdemo.utils.SDCardUtils;
import com.android.watermarkdemo.utils.ThreadPoolUtils;
import com.android.watermarkdemo.utils.ToastMaster;
import com.orhanobut.logger.Logger;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * 相机页面
 */

public class CameraActivity extends BaseActivity {

    public static final String KEY_ADDRESS = "key_address";

    private static final int FLASH_OFF = 0;//禁止闪光灯
    //    private static final int FLASH_AUTO = 1;//自动闪光灯
    private static final int FLASH_ON = 2;//开启闪光灯

    private static final SparseIntArray ORIENTATIONS = new SparseIntArray();

    static {
        ORIENTATIONS.append(Surface.ROTATION_0, 90);
        ORIENTATIONS.append(Surface.ROTATION_90, 0);
        ORIENTATIONS.append(Surface.ROTATION_180, 270);
        ORIENTATIONS.append(Surface.ROTATION_270, 180);
    }

    private ImageView ivFlash;
    private TextureView mTextureView;
    private TextView tvDatetime;
    private TextView tvAddress;
    private TextView tvCancel;
    private ImageView ivTakePhoto;
    private ImageView ivChangeCamera;

    private CameraCaptureSession mCaptureSession;
    private CameraDevice mCameraDevice;
    private CaptureRequest.Builder mPreviewRequestBuilder;
    private CaptureRequest mPreviewRequest;
    private ImageReader mImageReader;
    private HandlerThread mBackgroundThread;
    private Handler mBackgroundHandler;
    private ScheduledExecutorService mThreadPool;

    private Size mPreviewSize;
    private int currentFlashMode = FLASH_OFF;
    private String mCameraId = "0";// 摄像头ID（通常0代表后置摄像头，1代表前置摄像头）
    private int cameraNumber = 1;
    private String mDatetime;
    private String mAddress;
    private String mTempPath;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camera);

        initData();
        initView();

    }

    @Override
    protected void onResume() {
        super.onResume();
        initDatetime();
        startBackgroundThread();
        if (mTextureView.isAvailable()) {
            openCamera();
        } else {
            mTextureView.setSurfaceTextureListener(textureListener);
        }
    }

    @Override
    public void onPause() {
        ThreadPoolUtils.getInstache().setShutDown(mThreadPool, 0);
        closeCamera();
        stopBackgroundThread();
        super.onPause();
    }

    private void initData() {
        mAddress = getIntent().getStringExtra(KEY_ADDRESS);
        mTempPath = SDCardUtils.getExternalFilesDir(this) + Constants.PATH_TEMP;
    }

    private void initView() {
        ivFlash = (ImageView) findViewById(R.id.iv_flash);
        mTextureView = (TextureView) findViewById(R.id.textureview);
        tvDatetime = (TextView) findViewById(R.id.tv_datetime);
        tvAddress = (TextView) findViewById(R.id.tv_address);
        tvCancel = (TextView) findViewById(R.id.tv_cancel);
        ivTakePhoto = (ImageView) findViewById(R.id.iv_take_photo);
        ivChangeCamera = (ImageView) findViewById(R.id.iv_change_camera);

        ivFlash.setOnClickListener(mClick);
        tvCancel.setOnClickListener(mClick);
        ivTakePhoto.setOnClickListener(mClick);
        ivChangeCamera.setOnClickListener(mClick);

        tvAddress.setText(mAddress);
    }

    private void initDatetime() {
        mThreadPool = Executors.newScheduledThreadPool(3);
        mThreadPool.scheduleAtFixedRate(new Runnable() {
            @Override
            public void run() {
                final String date = DateTimeUtils.getCnDate();
                final String week = DateTimeUtils.getCnWeek();
                final String time = DateTimeUtils.getEnShortTime();
                mDatetime = date + "\t" + week + "\t" + time;

                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        tvDatetime.setText(mDatetime);
                    }
                });
            }
        }, 100, 1000, TimeUnit.MILLISECONDS);
    }

    ///////////////////////////////////////////////////////////////////////////
    // 拍照相关方法： 拍照，切换摄像头，切换闪光灯模式
    ///////////////////////////////////////////////////////////////////////////
    private void takePicture() {
        captureStillPicture();
    }

    private void changeCameraFacing() {
        if (cameraNumber > 1) {
            mCameraId = "0".equals(mCameraId) ? "1" : "0";
        }

        closeCamera();
        openCamera();
    }

    private void changeFlashMode() {
        if (FLASH_OFF == currentFlashMode) {
            ivFlash.setImageResource(R.drawable.ic_flash_on);
            currentFlashMode = FLASH_ON;
        } else {
            ivFlash.setImageResource(R.drawable.ic_flash_off);
            currentFlashMode = FLASH_OFF;
        }

        try {
            if (currentFlashMode == FLASH_ON) {
                mPreviewRequestBuilder.set(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_ON_ALWAYS_FLASH);
            } else {
                mPreviewRequestBuilder.set(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_ON);
            }

            mPreviewRequest = mPreviewRequestBuilder.build();
            mCaptureSession.setRepeatingRequest(mPreviewRequest, null, null);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    ///////////////////////////////////////////////////////////////////////////
    // 回调监听
    ///////////////////////////////////////////////////////////////////////////
    private View.OnClickListener mClick = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            switch (v.getId()) {
                case R.id.iv_flash:
                    changeFlashMode();
                    break;
                case R.id.tv_cancel:
                    onBackPressed();
                    break;
                case R.id.iv_take_photo:
                    takePicture();
                    break;
                case R.id.iv_change_camera:
                    changeCameraFacing();
                    break;
            }
        }
    };

    //显示图像回调
    private TextureView.SurfaceTextureListener textureListener = new TextureView.SurfaceTextureListener() {
        @Override
        public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
            openCamera();
        }

        @Override
        public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {

        }

        @Override
        public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
            return true;
        }

        @Override
        public void onSurfaceTextureUpdated(SurfaceTexture surface) {

        }
    };

    private CameraDevice.StateCallback deviceStateCallback = new CameraDevice.StateCallback() {
        @Override
        public void onOpened(@NonNull CameraDevice camera) {
            mCameraDevice = camera;
            createCameraPreviewSession();
        }

        @Override
        public void onDisconnected(@NonNull CameraDevice camera) {
            camera.close();
            mCameraDevice = null;
        }

        @Override
        public void onError(@NonNull CameraDevice camera, int error) {
            camera.close();
            mCameraDevice = null;
            finish();
        }
    };

    //设置相机属性
    private CameraCaptureSession.StateCallback cameraStateCallback = new CameraCaptureSession.StateCallback() {
        @Override
        public void onConfigured(@NonNull CameraCaptureSession cameraCaptureSession) {
            mCaptureSession = cameraCaptureSession;
            try {
                // 自动对焦
                mPreviewRequestBuilder.set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE);

                if (currentFlashMode == FLASH_ON) {
                    mPreviewRequestBuilder.set(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_ON_ALWAYS_FLASH);
                } else {
                    mPreviewRequestBuilder.set(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_ON);
                }

                // 构建上述的请求
                mPreviewRequest = mPreviewRequestBuilder.build();
                // 重复进行上面构建的请求, 以便显示预览
                mCaptureSession.setRepeatingRequest(mPreviewRequest, null, null);
            } catch (CameraAccessException e) {
                e.printStackTrace();
            }
        }

        @Override
        public void onConfigureFailed(@NonNull CameraCaptureSession cameraCaptureSession) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    ToastMaster.toast("Failed");
                }
            });
        }
    };

    //拍照后图片返回监听
    private final ImageReader.OnImageAvailableListener mOnImageAvailableListener = new ImageReader.OnImageAvailableListener() {

        @Override
        public void onImageAvailable(ImageReader reader) {
            long dataTake = System.currentTimeMillis();
            final File file = new File(mTempPath, "Camera2_" + dataTake + ".jpg");

            ImageUtils.saveToJpg(reader, file);

            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if (file.exists()) {
                        Intent intent = new Intent();
                        intent.setClass(CameraActivity.this, CompressPhotoActivity.class);
                        intent.putExtra(CompressPhotoActivity.KEY_PHOTO_FILE_PATH, file.getAbsolutePath());
                        intent.putExtra(CompressPhotoActivity.KEY_DATETIME, mDatetime);
                        intent.putExtra(CompressPhotoActivity.KEY_LOCATION_ADDRESS, mAddress);
                        startActivity(intent);
                    } else {
                        ToastMaster.toast("拍照失败");
                    }
                }
            });
        }
    };

    ///////////////////////////////////////////////////////////////////////////
    // camera2 开启，关闭相关方法
    ///////////////////////////////////////////////////////////////////////////
    //通过cameraId打开特定的相机
    private void openCamera() {
        // 设置相机输出
        setUpCameraOutputs();

        CameraManager manager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
        try {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                return;
            }
            // 打开相机, 参数是: 相机id, 相机状态回调, 子线程处理器
            manager.openCamera(mCameraId, deviceStateCallback, null);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    //关闭正在使用的相机
    private void closeCamera() {
        // 关闭捕获会话
        if (null != mCaptureSession) {
            mCaptureSession.close();
            mCaptureSession = null;
        }
        // 关闭当前相机
        if (null != mCameraDevice) {
            mCameraDevice.close();
            mCameraDevice = null;
        }
        // 关闭拍照处理器
        if (null != mImageReader) {
            mImageReader.close();
            mImageReader = null;
        }
    }

    /**
     * 设置相机的输出, 包括预览和拍照
     * <p>
     * 处理流程如下:
     * 1. 获取当前的摄像头, 并将拍照输出设置为最高画质
     * 2. 判断显示方向和摄像头传感器方向是否一致, 是否需要旋转画面
     * 3. 获取当前显示尺寸和相机的输出尺寸, 选择最合适的预览尺寸
     */
    private void setUpCameraOutputs() {
        CameraManager manager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
        try {
            String[] cameraIds = manager.getCameraIdList();
            cameraNumber = cameraIds.length;
            CameraCharacteristics characteristics = manager.getCameraCharacteristics(mCameraId);
            StreamConfigurationMap map = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
            if (map == null) {
                return;
            }

            // 选用最高画质
            List<Size> pictureSizes = Arrays.asList(map.getOutputSizes(ImageFormat.JPEG));
            Size largest = CameraUtils.getMaxSize(pictureSizes, 4160, 16f / 9f);

            Logger.d("largest.width: " + largest.getWidth() + "\nlargest.height: " + largest.getHeight());

            mImageReader = ImageReader.newInstance(largest.getWidth(), largest.getHeight(), ImageFormat.JPEG, 2);
            mImageReader.setOnImageAvailableListener(mOnImageAvailableListener, mBackgroundHandler);

            // 自动计算出最适合的预览尺寸
            List<Size> previewSizes = new ArrayList<>();
            Collections.addAll(previewSizes, map.getOutputSizes(SurfaceTexture.class));
            mPreviewSize = CameraUtils.getMaxSize(previewSizes, 1920, 16f / 9f);

            Logger.d("mPreviewSize.getWidth: " + mPreviewSize.getWidth() + "\nmPreviewSize.getHeight: " + mPreviewSize.getHeight());

        } catch (CameraAccessException e) {
            e.printStackTrace();
        } catch (NullPointerException e) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    ToastMaster.toast("camera error");
                }
            });
        }
    }

    //开启子线程
    private void startBackgroundThread() {
        mBackgroundThread = new HandlerThread("CameraBackground");
        mBackgroundThread.start();
        mBackgroundHandler = new Handler(mBackgroundThread.getLooper());
    }

    //停止子线程
    private void stopBackgroundThread() {
        mBackgroundThread.quitSafely();
        try {
            mBackgroundThread.join();
            mBackgroundThread = null;
            mBackgroundHandler = null;
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    //创建camera session
    private void createCameraPreviewSession() {
        try {
            SurfaceTexture texture = mTextureView.getSurfaceTexture();
            texture.setDefaultBufferSize(mPreviewSize.getWidth(), mPreviewSize.getHeight());
            Surface surface = new Surface(texture);
            mPreviewRequestBuilder = mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
            mPreviewRequestBuilder.addTarget(surface);
            mCameraDevice.createCaptureSession(Arrays.asList(surface, mImageReader.getSurface()), cameraStateCallback, mBackgroundHandler);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    //拍照操作
    private void captureStillPicture() {
        try {
            if (null == mCameraDevice) {
                return;
            }
            final CaptureRequest.Builder captureBuilder = mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE);
            captureBuilder.addTarget(mImageReader.getSurface());
            int rotation = getWindowManager().getDefaultDisplay().getRotation();
            captureBuilder.set(CaptureRequest.JPEG_ORIENTATION, ORIENTATIONS.get(rotation));

            final CameraCaptureSession.CaptureCallback CaptureCallback = new CameraCaptureSession.CaptureCallback() {

                @Override
                public void onCaptureCompleted(@NonNull CameraCaptureSession session,
                                               @NonNull CaptureRequest request,
                                               @NonNull TotalCaptureResult result) {
                    try {
                        // 拍完照后, 设置成预览状态, 并重复预览请求
                        mCaptureSession.setRepeatingRequest(mPreviewRequest, null, null);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            };

            mCaptureSession.stopRepeating();
            mCaptureSession.capture(captureBuilder.build(), CaptureCallback, null);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }
}
