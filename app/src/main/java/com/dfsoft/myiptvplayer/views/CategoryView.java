package com.dfsoft.myiptvplayer.views;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.AttributeSet;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.FrameLayout;
import android.widget.ListView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.dfsoft.myiptvplayer.IPTVCategory;
import com.dfsoft.myiptvplayer.IPTVChannel;
import com.dfsoft.myiptvplayer.IPTVConfig;
import com.dfsoft.myiptvplayer.IPTVMessage;
import com.dfsoft.myiptvplayer.R;

public class CategoryView extends FrameLayout {

    private final String TAG = "CategoryView";

    private Boolean mVisible = false;

    private CategoryAdapter mCategoryAdapter = null;

    public IPTVConfig config = IPTVConfig.getInstance();

    private ListView mCateList = null;

    private ListView mChannelList = null;

    private ListView mEpgList = null;

    private Context mContext;

    public CategoryView(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);

        this.mContext = context;

        LayoutInflater.from(context).inflate(R.layout.layout_category, this);

        mCateList = findViewById(R.id.categorylistView);

        mChannelList = findViewById(R.id.category_channel_list);

        mEpgList = findViewById(R.id.category_epg_list);

        mCategoryAdapter = new CategoryAdapter(context,this.config.category);

        mCateList.setAdapter(mCategoryAdapter);
        mCateList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                activeCategory(position);
            }
        });
        mCateList.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                Log.d(TAG, "onItemSelected: "+position);
                activeCategory(position);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });

        mChannelList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                activeChannel(position);
            }
        });

        mChannelList.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                activeChannel(position);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });


        mCateList.setOnFocusChangeListener(this.mCateFocusChangeListener);

        mChannelList.setOnKeyListener(mKeyListener);

    }

    public void activeCategory(int index) {
        mCategoryAdapter.setCurrentItem(index);
        IPTVCategory cate = this.config.category.get(index);
        if (cate.channelAdapter == null) {
            cate.channelAdapter = new ChannelAdapter(this.mContext,this.config.category,index);
        }
        mChannelList.setAdapter(cate.channelAdapter);
        mCategoryAdapter.notifyDataSetChanged();

    }

    public void activeChannel(int index) {
        ChannelAdapter adapter = (ChannelAdapter) mChannelList.getAdapter();

        adapter.setCurrentItem(index);

        IPTVChannel channel = adapter.getChannel();
        if (channel != null) {
            if (channel.epg.isEmpty()) {
                channel.loadEPGData();
            }
            if (channel.epgAdapter == null) {
                channel.epgAdapter = new EPGAdapter(this.mContext, channel);
            }
            channel.epg.getCurrentTimer();
//            mEpgList.smoothScrollToPosition(channel.epgAdapter);
        }

        adapter.notifyDataSetChanged();
        int curtime = channel.epg.curTime;
        mEpgList.setAdapter(channel.epgAdapter);
        if (curtime != -1) {
            int h = mEpgList.getMeasuredHeight() / 2;
            mEpgList.setSelectionFromTop(curtime,h);
        }
    }

    private void showCurrentChannel() {
        IPTVChannel channel = config.getPlayingChannal();
        if (channel == null) return;
        IPTVCategory cate = config.getCategoryByChannel(channel);
        if (cate == null) return;
        int index = config.category.indexOf(cate);
        int h1 = mCateList.getMeasuredHeight() / 2;
        mCateList.setSelectionFromTop(index,h1);
        this.activeCategory(index);
        index = cate.data.indexOf(channel);
        this.activeChannel(index);
        int h = mChannelList.getMeasuredHeight() / 2;
        mChannelList.setSelectionFromTop(index,h);
//        mChannelList.setFocusable(true);
        mChannelList.requestFocus();
    }

    public void show() {
        this.setVisibility(View.VISIBLE);
        this.showCurrentChannel();
        this.mVisible = true;
    }

    public void hide() {
        this.setVisibility(View.GONE);
//        mChannelList.setFocusable(false);
        this.mVisible = false;
        config.iptvMessage.sendMessage(IPTVMessage.IPTV_FULLSCREEN);
    }

    public void toggle() {
        Log.d(TAG, "onKey: toggle mVisible = "+mVisible);
        if (this.mVisible) {
            this.hide();
        } else
            this.show();
    }

    public void updateEpg(IPTVChannel channel) {
        if (channel.epgAdapter != null)
            channel.epgAdapter.notifyDataSetChanged();

        EPGAdapter adapter = (EPGAdapter) mEpgList.getAdapter();
        if (adapter != null && adapter.channel == channel) {
            channel.epg.getCurrentTimer();
            int curtime = channel.epg.curTime;
            if (curtime != -1) {
                int h = mEpgList.getMeasuredHeight() / 2;
                mEpgList.setSelectionFromTop(curtime, h);
            }

        }

    }

    private OnKeyListener mKeyListener = new OnKeyListener() {
        @Override
        public boolean onKey(View v, int keyCode, KeyEvent event) {
            if (!mVisible) return false;
            Log.d(TAG, "onKey: "+keyCode + " mVisible = "+mVisible);
            if (keyCode == KeyEvent.KEYCODE_ENTER || keyCode == KeyEvent.KEYCODE_DPAD_CENTER) {
                hide();
                ChannelAdapter adapter = (ChannelAdapter) mChannelList.getAdapter();
                if (adapter != null)
                    config.iptvMessage.sendMessage(IPTVMessage.IPTV_CHANNEL_PLAY,adapter.getChannel());
                return true;
            }
            if (keyCode == KeyEvent.KEYCODE_ESCAPE || keyCode == KeyEvent.KEYCODE_DEL) {
                hide();
                return true;
            }
            return false;
        }
    };

    private OnFocusChangeListener mCateFocusChangeListener = new OnFocusChangeListener() {
        @Override
        public void onFocusChange(View v, boolean hasFocus) {
            if (hasFocus)  {
                mEpgList.setVisibility(View.GONE);
            } else {
                mEpgList.setVisibility(View.VISIBLE);
            }
            Log.d(TAG, "onFocusChange: "+hasFocus);
        }
    };


}
