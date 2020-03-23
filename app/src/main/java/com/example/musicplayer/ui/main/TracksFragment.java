package com.example.musicplayer.ui.main;

import android.Manifest;
import android.app.AlertDialog;
import android.app.Notification;
import android.app.PendingIntent;
import android.content.ContextWrapper;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.PopupMenu;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import androidx.lifecycle.ViewModelProviders;
import androidx.room.Room;

import com.example.musicplayer.AppDatabase;
import com.example.musicplayer.BlankFragment;
import com.example.musicplayer.CustomAdapterAllSongs;
import com.example.musicplayer.MainActivity;
import com.example.musicplayer.MediaStoreService;
import com.example.musicplayer.R;
import com.example.musicplayer.Song;
import com.example.musicplayer.TabbedActivity;
import com.google.android.exoplayer2.ExoPlaybackException;
import com.google.android.exoplayer2.ExoPlayer;
import com.google.android.exoplayer2.ExoPlayerFactory;
import com.google.android.exoplayer2.Player;
import com.google.android.exoplayer2.SimpleExoPlayer;
import com.google.android.exoplayer2.source.ConcatenatingMediaSource;
import com.google.android.exoplayer2.source.ProgressiveMediaSource;
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector;
import com.google.android.exoplayer2.ui.PlayerNotificationManager;
import com.google.android.exoplayer2.ui.PlayerView;
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory;
import com.google.android.exoplayer2.util.Util;
import com.pixplicity.easyprefs.library.Prefs;

import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;


/**
 * A placeholder fragment containing a simple view.
 */
public class TracksFragment extends Fragment {

    ListView listView;
    CustomAdapterAllSongs adapter;
    List<String> artists;
    List<Song> tracks;
    PlayerView playerView ;
    DefaultDataSourceFactory dataSourceFactory;
    private SimpleExoPlayer player ;
    private int oldPosition;
    EditText inputSearch;
    PlayerNotificationManager playerNotificationManager;
    ConcatenatingMediaSource concatenatingMediaSource;
    Button searchButton;

    private static final String ARG_SECTION_NUMBER = "section_number";

    private PageViewModel pageViewModel;

    public static TracksFragment newInstance()  {
        TracksFragment fragment = new TracksFragment();
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
        View root = inflater.inflate(R.layout.tracks_gallery, container, false);

        if(ContextCompat.checkSelfPermission(getContext(),
                Manifest.permission.READ_EXTERNAL_STORAGE)
                == PackageManager.PERMISSION_GRANTED) {

            instanciateSharedPreference();
            tracks = MediaStoreService.getMusicLibrary(getActivity().getContentResolver());
            Collections.sort(tracks, (o1, o2) -> o1.getTitle().compareToIgnoreCase(o2.getTitle()));
            artists = new ArrayList<>(new HashSet<String>(tracks.stream().map((song) -> song.getArtistName()).collect(Collectors.toList())));
            Collections.sort(artists);
            AppDatabase db = Room.databaseBuilder(getContext(),AppDatabase.class, "database-name").build();
            updateNumberOfPlay(db);
            setUpMusicPlayer(root);
            adapter =
                    new CustomAdapterAllSongs(getContext(), R.layout.song_item, tracks);
            listView = root.findViewById(R.id.songsList);
            listView.setAdapter(adapter);
            setSongsListView(adapter);
            selectLastPlayerSong(adapter);
            inputSearch = (EditText) root.findViewById(R.id.autoCompleteTextView);
            inputSearch.addTextChangedListener(new TextWatcher() {

                @Override
                public void onTextChanged(CharSequence cs, int arg1, int arg2, int arg3) {
                    // When user changed the Text
                    ((CustomAdapterAllSongs)listView.getAdapter()).getFilter().filter(cs);
                }

                @Override
                public void beforeTextChanged(CharSequence arg0, int arg1, int arg2,
                                              int arg3) {
                    // TODO Auto-generated method stub

                }

                @Override
                public void afterTextChanged(Editable arg0) {
                    // TODO Auto-generated method stub
                }
            });

            searchButton = root.findViewById(R.id.searchButton);
            searchButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    buildAlertDialog(adapter, inputSearch.getText().toString());

                }
            });
        }
        return root;
    }

    public void buildAlertDialog(CustomAdapterAllSongs adapter, String searchedTitle){
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        Map<Integer,String> foundSongs = new HashMap<>();
        builder.setTitle("Choose an song");
        for(int i=0;i<tracks.size();i++){

            if( StringUtils.containsIgnoreCase(tracks.get(i).getTitle(),searchedTitle)){
                foundSongs.put(i,tracks.get(i).getTitle() + "\n" + tracks.get(i).getArtistName());
            }
        }
        String[] foundSongsTitles = foundSongs.values().toArray(new String[foundSongs.size()]);

        builder.setItems(foundSongsTitles, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                String title = foundSongsTitles[which].substring(0,foundSongsTitles[which].indexOf("\n")).trim();
                String artist =  foundSongsTitles[which].substring(foundSongsTitles[which].indexOf("\n")).trim();
                Song foundSong = tracks.stream().filter((song) -> song.getArtistName().equalsIgnoreCase(artist) && StringUtils.containsIgnoreCase(song.getTitle(),title)).collect(Collectors.toList()).get(0);
                Toast.makeText(getContext(),tracks.indexOf(foundSong)+"",Toast.LENGTH_LONG).show();

                listView.requestFocusFromTouch();
                listView.setSelection(tracks.indexOf(foundSong));
                adapter.selectItem(tracks.indexOf(foundSong));
                listView.setItemChecked(tracks.indexOf(foundSong),true);
                Prefs.putInt("lastPlayedSong",tracks.indexOf(foundSong));
            }
        });

        AlertDialog dialog = builder.create();
        dialog.show();

    }

    private void selectLastPlayerSong(CustomAdapterAllSongs adapter) {
        new Prefs.Builder()
                .setContext(getContext())
                .setMode(ContextWrapper.MODE_PRIVATE)
                .setPrefsName("lastPlayedSong")
                .setUseDefaultSharedPreference(true)
                .build();
        listView.setSelection(Prefs.getInt("lastPlayedSong",0));
        adapter.selectItem(Prefs.getInt("lastPlayedSong",0));
    }

    private void setSongsListView(CustomAdapterAllSongs adapter) {

        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

                Prefs.putInt("lastPlayedSong",position);
                view.setBackgroundColor(Color.BLUE);
                oldPosition = position;
                adapter.selectItem(position);
                player.prepare(   new ProgressiveMediaSource.Factory(dataSourceFactory)
                        .createMediaSource(Uri.parse(tracks.get(position).getPath())));
                player.setPlayWhenReady(true);
            }
        });

        listView.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
                Toast.makeText(getContext(),"clicked",Toast.LENGTH_LONG).show();
                PopupMenu popup = new PopupMenu(getContext(), view);

                popup.getMenuInflater().inflate(R.menu.main_menu,
                        popup.getMenu());
                popup.show();


                popup.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
                    @Override
                    public boolean onMenuItemClick(MenuItem item) {
                        switch (item.getItemId()) {
                            case R.id.addToPlaylist:

                                break;
                        }
                        return true;
                    }
                });
                return true;
            }

        });
    }

    private void setUpMusicPlayer(View view) {
        playerView = view.findViewById(R.id.video_view);
        player = ExoPlayerFactory.newSimpleInstance(getContext(),new DefaultTrackSelector());
        dataSourceFactory = new DefaultDataSourceFactory(getContext(), Util.getUserAgent(getContext(),"audio demo"));

        playerNotificationManager = PlayerNotificationManager.createWithNotificationChannel(getContext(), "channel1", R.string.playback_channel_name, 1,
                new PlayerNotificationManager.MediaDescriptionAdapter() {
                    @Override
                    public String getCurrentContentTitle(Player player) {
                        return tracks.get(player.getCurrentWindowIndex()).getTitle();
                    }

                    @Nullable
                    @Override
                    public PendingIntent createCurrentContentIntent(Player player) {
                        Intent intent = new Intent(getContext(), MainActivity.class);
                        return PendingIntent.getActivity(getContext(),0,intent,PendingIntent.FLAG_UPDATE_CURRENT);
                    }

                    @Nullable
                    @Override
                    public String getCurrentContentText(Player player) {
                        return tracks.get(player.getCurrentWindowIndex()).getArtistName();
                    }

                    @Nullable
                    @Override
                    public Bitmap getCurrentLargeIcon(Player player, PlayerNotificationManager.BitmapCallback callback) {
                        return null;
                    }
                }
        );
        playerNotificationManager.setNotificationListener(new PlayerNotificationManager.NotificationListener() {
                                                              @Override
                                                              public void onNotificationStarted ( int notificationId, Notification notification){
                                                                  Intent intent = new Intent();
                                                                  Util.startForegroundService(getContext(),intent);
                                                              }
                                                              @Override
                                                              public void onNotificationCancelled ( int notificationId){
                                                                  player.release();
                                                              }
                                                          }
        );

        player.addListener(new ExoPlayer.EventListener(){
            @Override
            public void onLoadingChanged(boolean isLoading) {
            }

            @Override
            public void onPlayerStateChanged(boolean playWhenReady, int playbackState) {
                if(playbackState == ExoPlayer.STATE_ENDED){
                    player.prepare(   new ProgressiveMediaSource.Factory(dataSourceFactory)
                            .createMediaSource(Uri.parse(tracks.get(oldPosition+1).getPath())));
                    player.setPlayWhenReady(true);
                }
            }


            @Override
            public void onPlayerError(ExoPlaybackException error) {
            }

            @Override
            public void onPositionDiscontinuity(int i) {
                int latestWindowIndex = player.getCurrentWindowIndex();
                // item selected in playlist has changed, handle here
                oldPosition = latestWindowIndex;
            }
        });
    }

    private void updateNumberOfPlay(AppDatabase db) {
        AsyncTask.execute(new Runnable() {
            @Override
            public void run() {
                try{
                    // db.userDao().insertUsers(getMusicLibrary());
                    Log.e(MainActivity.class.getSimpleName(),db.userDao().getAll().size()+"");
                    Song s = db.userDao().getAll().get(1);
                    s.setNumberOfPLay(2);
                    db.userDao().updateSong(s);
                }catch(Exception e){
                    e.printStackTrace();
                }
            }
        });
    }

    private void instanciateSharedPreference() {
        new Prefs.Builder()
                .setContext(getContext())
                .setMode(ContextWrapper.MODE_PRIVATE)
                .setPrefsName("lastPlayedSong")
                .setUseDefaultSharedPreference(true)
                .build();
    }
}