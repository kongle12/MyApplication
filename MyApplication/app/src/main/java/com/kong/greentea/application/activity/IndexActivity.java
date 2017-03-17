package com.kong.greentea.application.activity;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Picture;
import android.graphics.Point;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.TypedValue;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;

import com.example.ding.myapplication.R;
import com.google.android.gms.appindexing.Action;
import com.google.android.gms.appindexing.AppIndex;
import com.google.android.gms.appindexing.Thing;
import com.google.android.gms.common.api.GoogleApiClient;
import com.kong.greentea.application.util.BitmapUtil;
import com.kong.greentea.application.util.SPUtils;
import com.kong.greentea.application.util.StatusBarCompat;

public class
IndexActivity extends Activity implements ViewPager.OnPageChangeListener {
    private static final int[] imgIdArray = {R.drawable.index, R.drawable.index2, R.drawable.index3};
    private ViewPager viewPager;
    private static final ImageView[] bgs = new ImageView[imgIdArray.length];
    private static final ImageView[] dots = new ImageView[imgIdArray.length];
    private LinearLayout dotContainer;
    private int dotWidth, dotHeight, dotPadding;
    /**
     * ATTENTION: This was auto-generated to implement the App Indexing API.
     * See https://g.co/AppIndexing/AndroidStudio for more information.
     */
    private GoogleApiClient client;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.guide_activity);
        StatusBarCompat.setStatusBarColor(this, getResources().getColor(R.color.head_color));
        first();
        //gotoNextPage();//直接进入下个页面，从而不使用引导图
        findView();
        initView();
        // ATTENTION: This was auto-generated to implement the App Indexing API.
        // See https://g.co/AppIndexing/AndroidStudio for more information.
        client = new GoogleApiClient.Builder(this).addApi(AppIndex.API).build();
    }


    // 判断是否第一次启动
    private void first() {
        if ((Boolean) SPUtils.get(this, "first_time", Boolean.TRUE)) {// 第一次启动
            SPUtils.put(this, "first_time", false);
        } else {// 第N次启动
            gotoNextPage();
        }
    }

    //
    private void gotoNextPage() {
        Intent intent = new Intent(IndexActivity.this, LoginBuyActivity.class);
        startActivity(intent);
       ActivityManager.getActivityManager().closeActivity(this);
    }

    private void findView() {
        dotContainer = (LinearLayout) findViewById(R.id.dot_container);
        viewPager = (ViewPager) findViewById(R.id.view_pager);
    }

    private void initView() {
        dotWidth = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP,
                5, getResources().getDisplayMetrics());
        dotHeight = dotWidth;
        dotPadding = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP,
                5, getResources().getDisplayMetrics());
        for (int i = 0; i < dots.length; i++) {
            bgs[i] = new ImageView(this);
            dots[i] = new ImageView(this);
            LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(dotWidth, dotHeight);
            layoutParams.leftMargin = dotPadding;
            layoutParams.rightMargin = dotPadding;
            dotContainer.addView(dots[i], layoutParams);
        }

        viewPager.setAdapter(new GuideAdapter(bgs, viewOnclick));
        viewPager.setOnPageChangeListener(this);
        viewPager.setCurrentItem(0);
    }

    private View.OnClickListener viewOnclick = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            if ((Integer) v.getTag() == imgIdArray.length - 1) {
                gotoNextPage();
            }
        }
    };

    /**
     * ATTENTION: This was auto-generated to implement the App Indexing API.
     * See https://g.co/AppIndexing/AndroidStudio for more information.
     */
    public Action getIndexApiAction() {
        Thing object = new Thing.Builder()
                .setName("Index Page") // TODO: Define a title for the content shown.
                // TODO: Make sure this auto-generated URL is correct.
                .setUrl(Uri.parse("http://[ENTER-YOUR-URL-HERE]"))
                .build();
        return new Action.Builder(Action.TYPE_VIEW)
                .setObject(object)
                .setActionStatus(Action.STATUS_TYPE_COMPLETED)
                .build();
    }

    @Override
    public void onStart() {
        super.onStart();

        // ATTENTION: This was auto-generated to implement the App Indexing API.
        // See https://g.co/AppIndexing/AndroidStudio for more information.
        client.connect();
        AppIndex.AppIndexApi.start(client, getIndexApiAction());
    }

    @Override
    public void onStop() {
        super.onStop();

        // ATTENTION: This was auto-generated to implement the App Indexing API.
        // See https://g.co/AppIndexing/AndroidStudio for more information.
        AppIndex.AppIndexApi.end(client, getIndexApiAction());
        client.disconnect();
    }

    public class GuideAdapter extends PagerAdapter {
        ImageView[] datas;
        View.OnClickListener listener;

        public GuideAdapter(ImageView[] data, View.OnClickListener clickListener) {
            this.datas = data;
            this.listener = clickListener;
        }

        @Override
        public int getCount() {
            return datas.length;
        }

        @Override
        public boolean isViewFromObject(View view, Object object) {
            return view == object;
        }

        @Override
        public void destroyItem(ViewGroup container, int position, Object object) {
            container.removeView(datas[position]);
        }

        @Override
        public Object instantiateItem(ViewGroup container, int position) {
            container.addView(datas[position], new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT
                    , ViewGroup.LayoutParams.MATCH_PARENT));
            datas[position].setScaleType(ImageView.ScaleType.FIT_XY);
            datas[position].setOnClickListener(listener);
            datas[position].setImageResource(imgIdArray[position]);
            datas[position].setTag(position);
            return datas[position];
        }
    }

    @Override
    public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
        for (ImageView view : dots) {
            view.setImageResource(R.drawable.dot_normal);
        }
        dots[position].setImageResource((position != imgIdArray.length - 1)
                ? R.drawable.dot_white : R.drawable.dot_focused);
    }

    @Override
    public void onPageSelected(int position) {
    }

    @Override
    public void onPageScrollStateChanged(int state) {
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        for (ImageView view : bgs) {
            if (view != null) {
                Drawable d = view.getDrawable();
                if (d != null && d instanceof BitmapDrawable) {
                    BitmapUtil.recycleImg(((BitmapDrawable) d).getBitmap());
                }
            }
        }
    }
}
