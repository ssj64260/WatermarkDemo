package com.android.watermarkdemo.ui.activity;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import com.android.watermarkdemo.R;
import com.android.watermarkdemo.app.BaseActivity;
import com.android.watermarkdemo.config.Constants;
import com.android.watermarkdemo.db.LiteOrmHelper;
import com.android.watermarkdemo.model.AttendanceBean;
import com.android.watermarkdemo.ui.adapter.OnListClickListener;
import com.android.watermarkdemo.ui.adapter.PictureListAdapter;
import com.android.watermarkdemo.utils.DataCleanManager;
import com.android.watermarkdemo.utils.DateTimeUtils;
import com.android.watermarkdemo.utils.FileUtils;
import com.android.watermarkdemo.utils.SDCardUtils;
import com.android.watermarkdemo.utils.ThreadPoolUtil;
import com.android.watermarkdemo.utils.ToastMaster;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.File;
import java.util.ArrayList;
import java.util.List;


public class CreateActivity extends BaseActivity {

    private static final int REQUEST_CODE_TAKE_PHOTO = 1000;//拍照

    private EditText etAddress;
    private RecyclerView rvPictureList;
    private TextView tvSave;

    private List<String> mPictureList;
    private PictureListAdapter mPictureAdapter;

    private View.OnClickListener mClick = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            hideKeyboard();
            switch (v.getId()) {
                case R.id.iv_toolbar_back:
                    onBackPressed();
                    break;
                case R.id.tv_save:
                    doSave();
                    break;
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create);

        initData();
        initView();

    }

    @Override
    protected void onDestroy() {
        final String filePath = SDCardUtils.getExternalFilesDir(this) + Constants.PATH_TEMP;
        final File tempFile = new File(filePath);
        DataCleanManager.deleteAllFiles(tempFile);
        super.onDestroy();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (REQUEST_CODE_TAKE_PHOTO == requestCode) {
            if (RESULT_OK == resultCode && data != null) {
                final String filePath = data.getStringExtra(TakePhotoActivity.RETURN_FILE_PATH);
                if (!TextUtils.isEmpty(filePath)) {
                    mPictureList.add(filePath);
                    mPictureAdapter.notifyDataSetChanged();
                }
            }
        }
    }

    private void initData() {
        mPictureList = new ArrayList<>();
        mPictureAdapter = new PictureListAdapter(this, mPictureList);
        mPictureAdapter.setListClick(new OnListClickListener() {
            @Override
            public void onItemClick(int position) {
                if (position >= 0 && position < mPictureList.size()) {
                    //TODO 查看图片
                } else {
                    final String address = etAddress.getText().toString();
                    if (!TextUtils.isEmpty(address)) {
                        Intent intent = new Intent();
                        intent.setClass(CreateActivity.this, TakePhotoActivity.class);
                        intent.putExtra(TakePhotoActivity.KEY_LOCATION_ADDRESS, address);
                        startActivityForResult(intent, REQUEST_CODE_TAKE_PHOTO);
                    } else {
                        ToastMaster.toast("请先输入当前地址");
                    }
                }
            }

            @Override
            public void onTagClick(int tag, int position) {
                if (ITEM_TAG0 == tag) {
                    mPictureList.remove(position);
                    mPictureAdapter.notifyDataSetChanged();
                }
            }
        });
    }

    private void initView() {
        initToolbar();

        etAddress = (EditText) findViewById(R.id.et_address);
        rvPictureList = (RecyclerView) findViewById(R.id.rv_picture_list);
        tvSave = (TextView) findViewById(R.id.tv_save);

        rvPictureList.setLayoutManager(new GridLayoutManager(this, 3));
        rvPictureList.setAdapter(mPictureAdapter);

        tvSave.setOnClickListener(mClick);
    }

    private void initToolbar() {
        final ImageView ivBack = (ImageView) findViewById(R.id.iv_toolbar_back);
        final TextView tvTitle = (TextView) findViewById(R.id.tv_toolbar_title);

        ivBack.setOnClickListener(mClick);
        tvTitle.setText("考勤");
    }

    private void doSave() {
        final String address = etAddress.getText().toString();
        if (TextUtils.isEmpty(address)) {
            ToastMaster.toast("请先输入当前地址");
        } else {
            showProgress("提交中...");

            ThreadPoolUtil.getInstache().cachedExecute(new Runnable() {
                @Override
                public void run() {
                    final String date = DateTimeUtils.getCnDate();
                    final String week = DateTimeUtils.getCnWeek();
                    final String time = DateTimeUtils.getEnShortTime();
                    final String datetime = date + "\t" + week + "\t" + time;

                    final String savePath = SDCardUtils.getExternalFilesDir(CreateActivity.this) + Constants.PATH_ATTENDANCE;

                    for (String path : mPictureList) {
                        final File file = new File(path);
                        if (file.exists()) {
                            FileUtils.copyFile(file.getAbsolutePath(), savePath + file.getName());
                        }
                    }

                    final Gson gson = new GsonBuilder().create();
                    final String pictureListJson = gson.toJson(mPictureList);

                    final AttendanceBean attendance = new AttendanceBean();
                    attendance.setDatetime(datetime);
                    attendance.setAddress(address);
                    attendance.setPictureListJson(pictureListJson);

                    final LiteOrmHelper dbHelper = new LiteOrmHelper(CreateActivity.this);
                    dbHelper.save(attendance);
                    dbHelper.closeDB();

                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            hideProgress();
                            finish();
                        }
                    });
                }
            });
        }
    }
}
