package com.testplayer.app;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class StreamsAdapter extends RecyclerView.Adapter<StreamsAdapter.ViewHolder> {
    private Context context;
    private  List<String> streams;

    public StreamsAdapter(Context context, List<String> streams){
        this.context = context;
        this.streams = streams;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View view = inflater.inflate(R.layout.list_item, parent, false);

        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        String stream = streams.get(position);
        holder.title.setTag(position);
        holder.title.setText(stream);
    }

    @Override
    public int getItemCount() {
        return streams.size();
    }


    public String getItem(int position) {
        return streams.get(position);
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        public LinearLayout root;
        public TextView title;
        public ViewHolder(View itemView) {
            super(itemView);
            title =  itemView.findViewById(R.id.title);
            root =  itemView.findViewById(R.id.root);
        }
    }
}
