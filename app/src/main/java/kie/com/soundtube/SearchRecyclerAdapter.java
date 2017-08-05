package kie.com.soundtube;

import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import java.util.List;

public class SearchRecyclerAdapter extends RecyclerView.Adapter<SearchRecyclerAdapter.ViewHolder> {

    public List<DataHolder> dataHolders;

    public SearchRecyclerAdapter(List<DataHolder> dataHolders) {
        this.dataHolders = dataHolders;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view;
        if (viewType == 0) {
            RelativeLayout relativeLayout = new RelativeLayout(parent.getContext());
            relativeLayout.setLayoutParams(new ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, Tools.convertDpToPixel(56, parent.getContext())));
            view = relativeLayout;
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

        public ImageView imageView;
        public TextView titleview;
        public TextView durationview;

        public ViewHolder(View itemView) {
            super(itemView);
            imageView = (ImageView) itemView.findViewById(R.id.imageView);
            titleview = (TextView) itemView.findViewById(R.id.titleview);
            durationview = (TextView) itemView.findViewById(R.id.durationview);
        }
    }
}
