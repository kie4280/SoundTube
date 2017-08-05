package kie.com.soundtube;

import android.graphics.Color;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import java.util.List;

public class VideoRecyclerAdapter extends RecyclerView.Adapter<VideoRecyclerAdapter.ViewHolder> {

    public List<DataHolder> dataHolders;
    TextView titleView;
    String text;

    public VideoRecyclerAdapter(List<DataHolder> dataHolders) {
        this.dataHolders = dataHolders;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view;
        if (viewType == 0) {
            titleView = new TextView(parent.getContext());
            titleView.setTextSize(24f);
            titleView.setTextColor(Color.BLACK);
            view = titleView;
        } else {
            view = LayoutInflater.from(parent.getContext()).inflate(R.layout.video_layout, parent, false);
        }

        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {

        if (position != 0) {
            DataHolder dataHolder = dataHolders.get(position - 1);
            holder.imageView.setImageBitmap(dataHolder.thumbnail);
            holder.titleview.setText(dataHolder.title);
            holder.durationview.setText(dataHolder.videolength);
        } else {
            titleView.setText(text);
        }

    }

    @Override
    public int getItemViewType(int position) {
        return position == 0 ? 0 : 1;
    }

    @Override
    public int getItemCount() {
        return dataHolders.size() + 1;
    }

    public class ViewHolder extends RecyclerView.ViewHolder {

        public ImageView imageView = null;
        public TextView titleview = null;
        public TextView durationview = null;

        public ViewHolder(View itemView) {
            super(itemView);
            imageView = (ImageView) itemView.findViewById(R.id.imageView);
            titleview = (TextView) itemView.findViewById(R.id.titleview);
            durationview = (TextView) itemView.findViewById(R.id.durationview);
        }
    }

    public class BlankHolder extends RecyclerView.ViewHolder {
        public BlankHolder(View itemView) {
            super(itemView);
        }
    }
}
