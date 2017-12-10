package com.android.watermarkdemo.ui.adapter;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import com.android.watermarkdemo.R;
import com.android.watermarkdemo.widget.imageloader.ImageLoaderFactory;

import java.util.List;

/**
 * 图片列表
 */

public class PictureListAdapter extends RecyclerView.Adapter {

    private OnListClickListener mListClick;
    private Context mContext;
    private List<String> mList;
    private LayoutInflater mInflater;

    private View.OnClickListener mClick = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            if (mListClick != null) {
                final int position = (int) v.getTag();
                switch (v.getId()) {
                    case R.id.iv_close:
                        mListClick.onTagClick(OnListClickListener.ITEM_TAG0, position);
                        break;
                    default:
                        mListClick.onItemClick(position);
                        break;
                }
            }
        }
    };

    public PictureListAdapter(Context context, List<String> list) {
        mContext = context;
        mList = list;
        mInflater = LayoutInflater.from(context);
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        return new ListViewHolder(mInflater.inflate(R.layout.item_picture_list, parent, false));
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
        bindItem((ListViewHolder) holder, position);
    }

    private void bindItem(ListViewHolder holder, final int position) {
        if (position >= 0 && position < mList.size()) {
            final String model = mList.get(position);

            ImageLoaderFactory.getLoader().loadImage(mContext, holder.ivPicture, model);

            holder.ivClose.setVisibility(View.VISIBLE);
        } else {
            holder.ivClose.setVisibility(View.GONE);
            holder.ivPicture.setImageResource(0);
        }

        holder.ivClose.setTag(position);
        holder.ivClose.setOnClickListener(mClick);

        holder.itemView.setTag(position);
        holder.itemView.setOnClickListener(mClick);
    }

    @Override
    public int getItemCount() {
        final int count = mList.size();
        return count < 9 ? count + 1 : count;
    }

    private class ListViewHolder extends RecyclerView.ViewHolder {

        private View itemView;
        private ImageView ivPicture;
        private ImageView ivClose;

        private ListViewHolder(View itemView) {
            super(itemView);
            this.itemView = itemView;
            ivPicture = (ImageView) itemView.findViewById(R.id.iv_picture);
            ivClose = (ImageView) itemView.findViewById(R.id.iv_close);
        }
    }

    public void setListClick(OnListClickListener listClick) {
        this.mListClick = listClick;
    }
}
