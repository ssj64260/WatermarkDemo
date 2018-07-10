package com.android.watermarkdemo.ui.activity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.android.watermarkdemo.R;
import com.android.watermarkdemo.app.BaseActivity;
import com.android.watermarkdemo.db.LiteOrmHelper;
import com.android.watermarkdemo.model.AttendanceBean;
import com.android.watermarkdemo.utils.DataCleanManager;
import com.android.watermarkdemo.utils.FileUtils;
import com.android.watermarkdemo.utils.SDCardUtils;
import com.android.watermarkdemo.utils.ThreadPoolUtils;
import com.android.watermarkdemo.utils.ToastMaster;

import java.io.File;

/**
 * 首页
 */

public class MainActivity extends BaseActivity {

    private TextView tvCreate;
    private TextView tvAttendanceList;
    private TextView tvCache;
    private TextView tvDeleteDB;

    private String mTempPath;
    private File mTempFile;

    private View.OnClickListener mClick = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            switch (v.getId()) {
                case R.id.tv_create:
                    startActivity(new Intent(MainActivity.this, CreateActivity.class));
                    break;
                case R.id.tv_attendance_list:

                    break;
                case R.id.tv_cache:
                    doCleanCache();
                    break;
                case R.id.tv_delete_db:
                    doDeleteDateBase();
                    break;
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initData();
        initView();

    }

    @Override
    protected void onStart() {
        super.onStart();
        getCacheSize();
    }

    private void initData() {
        mTempPath = SDCardUtils.getExternalFilesDir(this);
        mTempFile = new File(mTempPath);
    }

    private void initView() {
        initToolbar();

        tvCreate = (TextView) findViewById(R.id.tv_create);
        tvAttendanceList = (TextView) findViewById(R.id.tv_attendance_list);
        tvCache = (TextView) findViewById(R.id.tv_cache);
        tvDeleteDB = (TextView) findViewById(R.id.tv_delete_db);

        tvCreate.setOnClickListener(mClick);
        tvAttendanceList.setOnClickListener(mClick);
        tvCache.setOnClickListener(mClick);
        tvDeleteDB.setOnClickListener(mClick);
    }

    private void initToolbar() {
        final ImageView ivBack = (ImageView) findViewById(R.id.iv_toolbar_back);
        final TextView tvTitle = (TextView) findViewById(R.id.tv_toolbar_title);
        ivBack.setVisibility(View.GONE);
        tvTitle.setText("应用");
    }

    private void getCacheSize() {
        long cacheSize = 0;
        try {
            cacheSize = FileUtils.getDirSize(mTempFile);
        } catch (Exception e) {
            e.printStackTrace();
        }

        final String cacheText = "清除缓存（" + FileUtils.formatFileSize(this, cacheSize) + "）";
        tvCache.setText(cacheText);
    }

    private void doCleanCache() {
        showProgress("删除中...");
        ThreadPoolUtils.getInstache().cachedExecute(new Runnable() {
            @Override
            public void run() {
                DataCleanManager.deleteAllFiles(mTempFile);

                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        getCacheSize();
                        hideProgress();
                        ToastMaster.toast("删除成功");
                    }
                });
            }
        });
    }

    private void doDeleteDateBase() {
        final LiteOrmHelper dbHelpter = new LiteOrmHelper(this);
        dbHelpter.deleteAll(AttendanceBean.class);
        dbHelpter.closeDB();
        ToastMaster.toast("数据库已清空");
    }
}
