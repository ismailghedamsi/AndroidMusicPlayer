package com.example.musicplayer;

import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;

import java.util.List;

public class CustomAdapter extends ArrayAdapter<Song> implements AdapterView.OnItemClickListener {
    List<Song> songs ;
    private int mSelectedItem = 0;
    private int TAG_UNSELECTED = 0;
    private int TAG_SELECTED = 1;
    public CustomAdapter(@NonNull Context context, int resource, List<Song> songs) {
        super(context, resource,songs);
        this.songs = songs;
    }

    public void selectItem(int position) {
        mSelectedItem = position;
        notifyDataSetChanged();
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        // Get the data item for this position
        Song dataModel = getItem(position);
        // Check if an existing view is being reused, otherwise inflate the view
        ViewHolder viewHolder; // view lookup cache stored in tag

        final View result;

        if (convertView == null) {

            viewHolder = new ViewHolder();
            LayoutInflater inflater = LayoutInflater.from(getContext());
            convertView = inflater.inflate(R.layout.song_item, parent, false);
            viewHolder.artistName = (TextView) convertView.findViewById(R.id.artistName);
            viewHolder.songTitle = (TextView) convertView.findViewById(R.id.songTitle);
            viewHolder.duration = (TextView) convertView.findViewById(R.id.duration);
            result=convertView;

            convertView.setTag(viewHolder);
        } else {
            viewHolder = (ViewHolder) convertView.getTag();
            result=convertView;
        }


        viewHolder.artistName.setText(dataModel.getArtistName());
        viewHolder.songTitle.setText(dataModel.getTitle());
        viewHolder.duration.setText(dataModel.getDuration());

        int type = getItemViewType(position);
        if(type == TAG_SELECTED) {
            convertView.setBackgroundColor(Color.parseColor("#1da7ff"));
        } else {
            convertView.setBackgroundColor(Color.parseColor("#f8f8f8"));
        }
        return convertView;
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

    }

    @Override
    public int getViewTypeCount() {
        return 2;
    }

    @Override
    public int getItemViewType(int position) {
        return position == mSelectedItem ? TAG_SELECTED : TAG_UNSELECTED;
    }

    private static class ViewHolder {
        TextView artistName;
        TextView songTitle;
        TextView duration;
    }
}