package kie.com.soundtube;

import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.MenuInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.PopupMenu;
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
//        if (viewType == 0) {
//            RelativeLayout relativeLayout = new RelativeLayout(parent.getContext());
//            relativeLayout.setLayoutParams(new ViewGroup.LayoutParams(
//                    ViewGroup.LayoutParams.MATCH_PARENT, Tools.convertDpToPixel(56, parent.getContext())));
//            view = relativeLayout;
//        } else {
//            view = LayoutInflater.from(parent.getContext()).inflate(R.layout.video_thumnail, parent, false);
//        }
        view = LayoutInflater.from(parent.getContext()).inflate(R.layout.video_thumnail, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        DataHolder dataHolder = dataHolders.get(position);
        holder.thumbnail.setImageBitmap(dataHolder.thumbnail);
        holder.titleview.setText(dataHolder.title);
        holder.durationview.setText(dataHolder.videolength);

    }

    @Override
    public int getItemViewType(int position) {
//        return position == 0 ? 0 : 1;
        return 0;
    }

    @Override
    public int getItemCount() {
        return dataHolders.size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder {

        public ImageView thumbnail, videooption;
        public TextView titleview;
        public TextView durationview;

        public ViewHolder(final View itemView) {
            super(itemView);
            thumbnail = (ImageView) itemView.findViewById(R.id.imageView);
            titleview = (TextView) itemView.findViewById(R.id.titleview);
            durationview = (TextView) itemView.findViewById(R.id.durationview);
            videooption = (ImageView) itemView.findViewById(R.id.video_option);
            videooption.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    PopupMenu popup = new PopupMenu(itemView.getContext(), view);
                    MenuInflater inflater = popup.getMenuInflater();
                    inflater.inflate(R.menu.video_option, popup.getMenu());
                    popup.show();
                }
            });
        }
    }

}
