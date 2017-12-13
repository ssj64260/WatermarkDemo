package com.android.watermarkdemo.ui.activity;

import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.FileProvider;
import android.view.View;
import android.widget.ImageView;
import android.widget.RadioButton;
import android.widget.TextView;

import com.android.watermarkdemo.R;
import com.android.watermarkdemo.app.BaseActivity;
import com.android.watermarkdemo.config.Constants;
import com.android.watermarkdemo.utils.DateTimeUtils;
import com.android.watermarkdemo.utils.ImageUtils;
import com.android.watermarkdemo.utils.SDCardUtils;
import com.android.watermarkdemo.utils.ThreadPoolUtil;
import com.android.watermarkdemo.utils.ToastMaster;
import com.android.watermarkdemo.widget.imageloader.ImageLoaderFactory;

import java.io.File;

import top.zibin.luban.Luban;
import top.zibin.luban.OnCompressListener;

/**
 * 拍照
 */

public class TakePhotoActivity extends BaseActivity {

    public static final String KEY_LOCATION_ADDRESS = "key_location_address";//定位地址
    public static final String RETURN_FILE_PATH = "return_file_path";//返回文件路径
    private static final int REQUEST_TAKE_PHOTO = 1001;// 相机拍照标记
    private static final int QUALITY_ORIGINAL = 100;//原图
    private static final int QUALITY_HD = 40;//高清
    private static final int QUALITY_ORDINARY = 10;//普清

    private ImageView ivPicture;
    private TextView tvRetake;
    private TextView tvSave;
    private RadioButton rbOriginal;
    private RadioButton rbHD;
    private RadioButton rbOrdinary;

    private File mTempFile;
    private String mSavePath;
    private File mSaveFile;
    private String mDatetime;
    private String mAddress;
    private int mQuality;

    private View.OnClickListener mClick = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            switch (v.getId()) {
                case R.id.tv_retake:
                    startActivity(new Intent(TakePhotoActivity.this, TakePhotoActivity.class));
                    finish();
                    break;
                case R.id.tv_save:
                    doSavePhoto();
                    break;
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_take_photo);

        initData();
        initView();
        doTakePhoto();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (REQUEST_TAKE_PHOTO == requestCode) {
            if (mTempFile.exists()) {
                CompressPicture();
                return;
            }
        }
        finish();
        overridePendingTransition(0, 0);
    }

    private void initData() {
        mSavePath = SDCardUtils.getExternalFilesDir(this) + Constants.PATH_TEMP;
        mTempFile = new File(mSavePath, System.currentTimeMillis() + ".jpg");

        final String date = DateTimeUtils.getCnDate();
        final String week = DateTimeUtils.getCnWeek();
        final String time = DateTimeUtils.getEnShortTime();
        mDatetime = date + "\t" + week + "\t" + time;
        mAddress = getIntent().getStringExtra(KEY_LOCATION_ADDRESS);
    }

    private void initView() {
        ivPicture = (ImageView) findViewById(R.id.iv_picture);
        tvRetake = (TextView) findViewById(R.id.tv_retake);
        tvSave = (TextView) findViewById(R.id.tv_save);
        rbOriginal = (RadioButton) findViewById(R.id.rb_original);
        rbHD = (RadioButton) findViewById(R.id.rb_hd);
        rbOrdinary = (RadioButton) findViewById(R.id.rb_ordinary);

        rbHD.setChecked(true);
        mQuality = QUALITY_HD;
    }

    private void doTakePhoto() {
        final Uri uri;
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) {
            uri = Uri.fromFile(mTempFile);
        } else {
            uri = FileProvider.getUriForFile(this, getPackageName() + ".myprovider", mTempFile);
        }

        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        intent.putExtra(MediaStore.EXTRA_OUTPUT, uri);
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
        if (intent.resolveActivity(getPackageManager()) != null) {
            startActivityForResult(intent, REQUEST_TAKE_PHOTO);
            overridePendingTransition(0, 0);
        }
    }

    private void CompressPicture() {
        Luban.with(this)
                .load(mTempFile)
                .setTargetDir(mTempFile.getParent())
                .ignoreBy(100)
                .setCompressListener(new OnCompressListener() {
                    @Override
                    public void onStart() {
                        showProgress("处理图片中...");
                    }

                    @Override
                    public void onSuccess(File file) {
                        mTempFile = file;
                        drawWaterMark(true);
                    }

                    @Override
                    public void onError(Throwable e) {
                        ToastMaster.toast("图片处理失败，请重试");
                        hideProgress();
                        finish();
                    }
                }).launch();
    }

    private void drawWaterMark(final boolean isShowPhoto) {
        showProgress("图片处理中...");
        ThreadPoolUtil.getInstache().cachedExecute(new Runnable() {
            @Override
            public void run() {

                final String filePath = mTempFile.getAbsolutePath();
                final Drawable timeDrawable = ContextCompat.getDrawable(TakePhotoActivity.this, R.drawable.ic_time);
                final Drawable addressDrawable = ContextCompat.getDrawable(TakePhotoActivity.this, R.drawable.ic_location);

                mSaveFile = ImageUtils.drawWaterMark(filePath, mSavePath, timeDrawable, addressDrawable, mDatetime, mAddress, mQuality);

                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (mSaveFile != null && mSaveFile.exists()) {
                            if (isShowPhoto) {
                                ImageLoaderFactory.getLoader().loadImage(TakePhotoActivity.this, ivPicture, mSaveFile.getAbsolutePath());
                                tvRetake.setOnClickListener(mClick);
                                tvSave.setOnClickListener(mClick);
                            } else {
                                doReturnPhotoPath();
                            }
                        } else {
                            ToastMaster.toast("图片不存在");
                            finish();
                        }
                        hideProgress();
                    }
                });
            }
        });
    }

    private void doSavePhoto() {
        if (rbOriginal.isChecked()) {
            mQuality = QUALITY_ORIGINAL;
        } else if (rbHD.isChecked()) {
            mQuality = QUALITY_HD;
            doReturnPhotoPath();
            return;
        } else {
            mQuality = QUALITY_ORDINARY;
        }
        drawWaterMark(false);
    }

    private void doReturnPhotoPath() {
        if (mSaveFile != null && mSaveFile.exists()) {
            final String filePath = mSaveFile.getAbsolutePath();
            Intent intent = new Intent();
            intent.putExtra(RETURN_FILE_PATH, filePath);
            setResult(RESULT_OK, intent);
        }
        finish();
    }
}
