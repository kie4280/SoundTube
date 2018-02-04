package kie.com.soundtube;

import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.Switch;
import android.widget.TextView;

import java.util.List;

public class VideoRecyclerAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    public List<DataHolder> dataHolders;
    View header;
    String title;

    public VideoRecyclerAdapter(List<DataHolder> dataHolders) {
        this.dataHolders = dataHolders;
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {

        if (viewType == 0) {
            header = LayoutInflater.from(parent.getContext()).inflate(R.layout.related_video_header, parent, false);
            return new HeaderHolder(header);
        } else {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.video_thumnail, parent, false);
            return new VideoHolder(view);
        }


    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder object, int position) {

        if (position != 0) {
            DataHolder dataHolder = dataHolders.get(position - 1);
            VideoHolder holder = (VideoHolder) object;
            holder.imageView.setImageBitmap(dataHolder.thumbnail);
            holder.titleview.setText(dataHolder.title);
            holder.durationview.setText(dataHolder.videolength);
        } else {
            HeaderHolder holder = (HeaderHolder) object;
            holder.titleview.setText(title);
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

    public class VideoHolder extends RecyclerView.ViewHolder {

        public ImageView imageView = null;
        public TextView titleview = null;
        public TextView durationview = null;

        public VideoHolder(View itemView) {
            super(itemView);
            imageView = (ImageView) itemView.findViewById(R.id.imageView);
            titleview = (TextView) itemView.findViewById(R.id.titleview);
            durationview = (TextView) itemView.findViewById(R.id.durationview);
        }
    }

    public class HeaderHolder extends RecyclerView.ViewHolder {

        public TextView titleview = null;
        public Switch playSwitch = null;

        public HeaderHolder(View itemView) {
            super(itemView);
            playSwitch = (Switch) itemView.findViewById(R.id.autoPlaySwitch);
            titleview = (TextView) itemView.findViewById(R.id.videoTitle);
            playSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                    MediaPlayerService.autoplay = b;
                }
            });
        }
    }
}
