package com.android.watermarkdemo.ui.activity;

import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.view.View;
import android.widget.ImageView;
import android.widget.RadioButton;
import android.widget.TextView;

import com.android.watermarkdemo.R;
import com.android.watermarkdemo.app.BaseActivity;
import com.android.watermarkdemo.utils.ImageUtils;
import com.android.watermarkdemo.utils.ThreadPoolUtils;
import com.android.watermarkdemo.utils.ToastMaster;
import com.android.watermarkdemo.widget.imageloader.ImageLoaderFactory;

import java.io.File;

import top.zibin.luban.Luban;
import top.zibin.luban.OnCompressListener;

/**
 * 拍照
 */

public class CompressPhotoActivity extends BaseActivity {

    public static final String KEY_DATETIME = "key_datetime";//拍照时间
    public static final String KEY_LOCATION_ADDRESS = "key_location_address";//定位地址
    public static final String KEY_PHOTO_FILE_PATH = "key_photo_file_path";//原照片路径
    public static final String RETURN_FILE_PATH = "return_file_path";//返回文件路径
    private static final int QUALITY_ORIGINAL = 100;//原图
    private static final int QUALITY_HD = 40;//高清
    private static final int QUALITY_ORDINARY = 10;//普清

    private ImageView ivPicture;
    private TextView tvDatetime;
    private TextView tvAddress;
    private TextView tvRetake;
    private TextView tvSave;
    private RadioButton rbOriginal;
    private RadioButton rbHD;
    private RadioButton rbOrdinary;

    private File mSaveFile;
    private File mPhotoFile;
    private String mSavePath;
    private String mPhotoFilePath;
    private String mDatetime;
    private String mAddress;
    private int mQuality;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_compress_photo);

        initData();
        initView();
        CompressPicture();
    }

    private void initData() {
        mPhotoFilePath = getIntent().getStringExtra(KEY_PHOTO_FILE_PATH);
        mDatetime = getIntent().getStringExtra(KEY_DATETIME);
        mAddress = getIntent().getStringExtra(KEY_LOCATION_ADDRESS);

        mPhotoFile = new File(mPhotoFilePath);
        mSavePath = mPhotoFile.getParent();
    }

    private void initView() {
        ivPicture = (ImageView) findViewById(R.id.iv_picture);
        tvDatetime = (TextView) findViewById(R.id.tv_datetime);
        tvAddress = (TextView) findViewById(R.id.tv_address);
        tvRetake = (TextView) findViewById(R.id.tv_retake);
        tvSave = (TextView) findViewById(R.id.tv_save);
        rbOriginal = (RadioButton) findViewById(R.id.rb_original);
        rbHD = (RadioButton) findViewById(R.id.rb_hd);
        rbOrdinary = (RadioButton) findViewById(R.id.rb_ordinary);

        tvDatetime.setText(mDatetime);
        tvAddress.setText(mAddress);

        rbHD.setChecked(true);
        mQuality = QUALITY_HD;
    }

    private void CompressPicture() {
        Luban.with(this)
                .load(mPhotoFile)
                .setTargetDir(mSavePath)
                .ignoreBy(200)
                .setCompressListener(new OnCompressListener() {
                    @Override
                    public void onStart() {
                        showProgress("处理图片中...");
                    }

                    @Override
                    public void onSuccess(File file) {
                        mPhotoFile = file;
                        ImageLoaderFactory.getLoader().loadImage(CompressPhotoActivity.this, ivPicture, mPhotoFile.getAbsolutePath());
                        tvRetake.setOnClickListener(mClick);
                        tvSave.setOnClickListener(mClick);
                        hideProgress();
                    }

                    @Override
                    public void onError(Throwable e) {
                        ToastMaster.toast("图片不存在，请重新拍照");
                        hideProgress();
                        finish();
                    }
                }).launch();
    }

    private void drawWaterMark() {
        showProgress("图片处理中...");
        ThreadPoolUtils.getInstache().cachedExecute(new Runnable() {
            @Override
            public void run() {

                final String filePath = mPhotoFile.getAbsolutePath();
                final Drawable timeDrawable = ContextCompat.getDrawable(CompressPhotoActivity.this, R.drawable.ic_time);
                final Drawable addressDrawable = ContextCompat.getDrawable(CompressPhotoActivity.this, R.drawable.ic_location);

                mSaveFile = ImageUtils.drawWaterMark(filePath, mSavePath, timeDrawable, addressDrawable, mDatetime, mAddress, mQuality);

                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        hideProgress();
                        if (mSaveFile != null && mSaveFile.exists()) {
                            final String filePath = mSaveFile.getAbsolutePath();
                            Intent intent = new Intent();
                            intent.setClass(CompressPhotoActivity.this, CreateActivity.class);
                            intent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_CLEAR_TOP);
                            intent.putExtra(RETURN_FILE_PATH, filePath);
                            startActivity(intent);
                        } else {
                            ToastMaster.toast("图片不存在，请重新拍照");
                            finish();
                        }
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
        } else {
            mQuality = QUALITY_ORDINARY;
        }
        drawWaterMark();
    }

    private View.OnClickListener mClick = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            switch (v.getId()) {
                case R.id.tv_retake:
                    onBackPressed();
                    break;
                case R.id.tv_save:
                    doSavePhoto();
                    break;
            }
        }
    };
}
