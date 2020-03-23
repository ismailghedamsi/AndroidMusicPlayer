package com.example.musicplayer.ui.main;

import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import androidx.lifecycle.ViewModelProviders;

import com.example.musicplayer.BlankFragment;
import com.example.musicplayer.CustomAdapterAllSongs;
import com.example.musicplayer.MediaStoreService;
import com.example.musicplayer.R;
import com.example.musicplayer.Song;
import com.example.musicplayer.TabbedActivity;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;
import java.util.stream.Collectors;


/**
 * A placeholder fragment containing a simple view.
 */
public class ArtistsFragment extends Fragment {

    ListView listView;
    ArrayAdapter arrayAdapter;
    List<String> artists;
    ListView listViewArtistsSongs;
    List<Song> songs;
    List<Song> songsOfArtist;

    private static final String ARG_SECTION_NUMBER = "section_number";

    private PageViewModel pageViewModel;

    public static ArtistsFragment newInstance()  {
        ArtistsFragment fragment = new ArtistsFragment();
        Bundle bundle = new Bundle();
        fragment.setArguments(bundle);
        return fragment;
    }


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        pageViewModel = ViewModelProviders.of(this).get(PageViewModel.class);
        int index = 1;
        if (getArguments() != null) {
            index = getArguments().getInt(ARG_SECTION_NUMBER);
        }

        pageViewModel.setIndex(index);
    }

    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_artists, container, false);
        listView = root.findViewById(R.id.list_view_artists);
        listViewArtistsSongs = root.findViewById(R.id.list_view_artistsSongs);
                songs = MediaStoreService.getMusicLibrary(getActivity().getContentResolver());

                artists =   new ArrayList<>(new HashSet<String>(songs.stream().map((song) -> song.getArtistName()).collect(Collectors.toList())));
                Collections.sort(artists);

        arrayAdapter = new ArrayAdapter(getContext(),android.R.layout.simple_list_item_1,artists);
        listView.setAdapter(arrayAdapter);
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

                songsOfArtist = songs.stream().filter((song) -> song.getArtistName().equals(  artists.get(position))).collect(Collectors.toList());
                CustomAdapterAllSongs adapter = new CustomAdapterAllSongs(getContext(),R.layout.song_item,songsOfArtist);
                listViewArtistsSongs.setAdapter(adapter);
                listView.setVisibility(View.GONE);
                listViewArtistsSongs.setVisibility(View.VISIBLE);
            }

        });
        return root;
    }

    public void goBackToArtistLIst(){
        getActivity().runOnUiThread(new Runnable(){
            @Override
            public void run() {
                Toast.makeText(getContext(),"test",Toast.LENGTH_LONG).show();
                listViewArtistsSongs.setVisibility(View.GONE);
                listView.setVisibility(View.VISIBLE);
            }

    });

    }


}