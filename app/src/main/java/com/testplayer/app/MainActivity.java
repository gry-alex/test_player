package com.testplayer.app;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.pm.ActivityInfo;
import android.content.res.AssetManager;
import android.graphics.Color;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.exoplayer2.C;
import com.google.android.exoplayer2.ExoPlaybackException;
import com.google.android.exoplayer2.PlaybackPreparer;
import com.google.android.exoplayer2.Player;
import com.google.android.exoplayer2.SimpleExoPlayer;
import com.google.android.exoplayer2.source.MediaSource;
import com.google.android.exoplayer2.source.TrackGroup;
import com.google.android.exoplayer2.source.TrackGroupArray;
import com.google.android.exoplayer2.source.hls.HlsMediaSource;
import com.google.android.exoplayer2.trackselection.AdaptiveTrackSelection;
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector;
import com.google.android.exoplayer2.trackselection.MappingTrackSelector;

import com.google.android.exoplayer2.trackselection.TrackSelection;
import com.google.android.exoplayer2.ui.PlayerControlView;
import com.google.android.exoplayer2.ui.PlayerView;

import com.google.android.exoplayer2.ui.TrackSelectionView;
import com.google.android.exoplayer2.upstream.DataSpec;
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory;
import com.google.android.exoplayer2.upstream.HttpDataSource;
import com.google.android.exoplayer2.util.Util;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.scalars.ScalarsConverterFactory;

public class MainActivity extends AppCompatActivity implements Player.EventListener, PlaybackPreparer, PlayerControlView.VisibilityListener, TrackSelectionView.TrackSelectionListener {

    private View streamsPage,
            playerPage,
            decorView;
    private PlayerView playerView;
    private SimpleExoPlayer player;
    private TextView jsonText;
    private static String ERROR_TAG = "ERROR";
    private static String SUCCES_TAG = "SUCCES";
    private static String BASE_PATH = "file:///android_asset/";
    private AssetManager assetManager;
    private String APP_NAME;

    private DefaultTrackSelector trackSelector;
    private MediaSource mediaSource;
    private  PlayerControlView controller;
    private int currentStreamIndex,
            back_pressed_count;
    private RecyclerView streamsList;
    private Context context;
    String [] ratios = {"4:3", "2:1", "1:1", "16:9",};


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        context = this;
        APP_NAME = getString(R.string.app_name);

        streamsPage = findViewById(R.id.list_page);

        playerPage = findViewById(R.id.player_page);

        jsonText = playerPage.findViewById(R.id.json_text);

        playerView = playerPage.findViewById(R.id.player_view);
        playerView.setUseController(false);
        playerView.setTag(0);
        ((ProgressBar) playerView.findViewById(R.id.exo_buffering)).getIndeterminateDrawable().setColorFilter(Color.WHITE,android.graphics.PorterDuff.Mode.MULTIPLY);

        controller = playerPage.findViewById(R.id.player_controller);
        controller.setShowTimeoutMs(3000);



        decorView = getWindow().getDecorView();

        streamsList =  findViewById(R.id.streams_list);
        streamsList.setLayoutManager(new LinearLayoutManager(this));



        assetManager = getAssets();

       /* try {
            String [] contents =  assetManager.list("");
         //   Log.e(SUCCES_TAG, "count = " + contents.length);
            if(contents != null && contents.length > 0) {
                ArrayList<String> streams = new ArrayList<String>();
                for (String content : contents){
                    if(content.contains(".m3u8")){
                          streams.add(content);
                    }
                }
                if(streams.size() > 0){
                    StreamsAdapter adapter = new StreamsAdapter(this,streams);
                    streamsList.setAdapter(adapter);
                    streamsList.setLayoutManager(new LinearLayoutManager(this));
                }
            }

        } catch (IOException e) {
            e.printStackTrace();
            Log.e(ERROR_TAG, "error: " + e.getMessage());
        }*/
    }

    @Override
    public void onStart() {
        super.onStart();
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR);
        streamsPage.setVisibility(View.VISIBLE);
        GetJsonFromServer();


    }

    @Override
    protected void onResume() {
        super.onResume();
       ;
        if(playerPage.getVisibility() == View.VISIBLE) {
            initializePlayer(false, false);
        }
        if (playerView != null) {
            playerView.onResume();
        }
        if(streamsPage.getVisibility() == View.VISIBLE){
            GetPlaylists();
        }



    }

    @Override
    public void onPause() {
        super.onPause();
        if(playerPage.getVisibility() == View.VISIBLE) {
            releasePlayer();
        }
        if (playerView != null) {
            playerView.onPause();
        }
    }

    @Override
    public void onStop() {
        super.onStop();
         if (playerView != null) {
            playerView.onPause();
        }
        if(playerPage.getVisibility() == View.VISIBLE) {
            releasePlayer();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        releasePlayer();
    }




    private void initializePlayer(boolean resetPosition, boolean resetSate) {
        TrackSelection.Factory trackSelectionFactory = new AdaptiveTrackSelection.Factory();
        trackSelector = new DefaultTrackSelector( this, trackSelectionFactory);

        player = new SimpleExoPlayer.Builder(this)
                    .setTrackSelector(trackSelector)
                    .build();
        player.addListener(this);
        player.prepare(mediaSource, resetPosition, resetSate);
        player.setPlayWhenReady(true);
        playerView.setPlayer(player);
        controller.setPlayer(playerView.getPlayer());
    }



   private MediaSource buildMediaSource(Uri uri) {
       DefaultDataSourceFactory dataSourceFactory = new DefaultDataSourceFactory(this, Util.getUserAgent(this, APP_NAME));
       return new HlsMediaSource.Factory(dataSourceFactory).createMediaSource(uri);
   }


   @SuppressLint("StaticFieldLeak")
   private void GetPlaylists(){
       streamsPage.findViewById(R.id.progress_bar).setVisibility(View.VISIBLE);
       streamsPage.findViewById(R.id.empty_text).setVisibility(View.INVISIBLE);

       new AsyncTask<Void, Void,  ArrayList<String>>() {

           @Override
           protected  ArrayList<String> doInBackground(Void... voids) {
               ArrayList<String> streams = null;

               try {
                   String [] contents =  assetManager.list("");
                   //   Log.e(SUCCES_TAG, "count = " + contents.length);
                   if(contents != null && contents.length > 0) {
                       streams = new ArrayList<String>();
                       for (String content : contents){
                           if(content.contains(".m3u8")){
                               streams.add(content);
                           }
                       }

                   }

               } catch (IOException e) {
                   e.printStackTrace();
                   Log.e(ERROR_TAG, "error: " + e.getMessage());
               }
               return streams;
           }

           @Override
           protected void onPostExecute( ArrayList<String> result) {
               super.onPostExecute(result);
               streamsPage.findViewById(R.id.progress_bar).setVisibility(View.INVISIBLE);
               if(result != null && result.size() > 0){
                   StreamsAdapter adapter = new StreamsAdapter(context,result);
                   streamsList.setAdapter(adapter);
               }else{
                   streamsPage.findViewById(R.id.empty_text).setVisibility(View.VISIBLE);
               }
           }
       }.execute();

   }


   private void GetJsonFromServer(){
       Retrofit retrofit = new Retrofit.Builder()
               .baseUrl( "http://info.limehd.tv/")
               .addConverterFactory(ScalarsConverterFactory.create())
               .build();
       ServerApi serverApi = retrofit.create(ServerApi.class);
       Call<String> info = serverApi.getInfo();
       info.enqueue(new Callback<String>() {
           @Override
           public void onResponse(@NonNull Call<String> call, @NonNull Response<String> response) {
               if(response.isSuccessful() && response.body() != null ){
                   try {
                       JSONObject json = new JSONObject(response.body());
                       String result = "";
                       if(json.length() > 0) {
                           for (int i = 0; i < json.length(); i++) {
                               result += json.names().getString(i) + " : " + json.get(json.names().getString(i)) + "\n";
                           }
                       }else{
                           result = getString(R.string.empty_json);
                       }
                        jsonText.setText(result);
                   } catch (JSONException e) {
                       e.printStackTrace();
                       jsonText.setText(e.getMessage());
                   }
               }else{
                   try {
                       jsonText.setText(response.errorBody().string());
                   } catch (IOException e) {
                       e.printStackTrace();
                       jsonText.setText(e.getMessage());
                   }
               }
           }

           @Override
           public void onFailure(@NonNull Call<String> call, Throwable t) {
               jsonText.setText(t.getMessage());
           }
       });
   }



    @Override
    public void onBackPressed() {
       if(playerPage.getVisibility() == View.VISIBLE){
            ClosePlayerPage();
        }else{
           if(back_pressed_count >= 1)
               super.onBackPressed();
           else {
               back_pressed_count++;
               Toast.makeText(this, getString(R.string.exit_text), Toast.LENGTH_SHORT).show();
               new Timer().schedule(new TimerTask() {
                   @Override
                   public void run() {
                       back_pressed_count = 0;
                   }
               }, 2000);
           }

        }
    }

    private void ClosePlayerPage(){
        controller.findViewById(R.id.prev_track).setEnabled(true);
        controller.findViewById(R.id.next_track).setEnabled(true);
        playerView.onPause();
        releasePlayer();
        showSystemUI();
        playerPage.setVisibility(View.INVISIBLE);
        streamsPage.setVisibility(View.VISIBLE);
    }

    public void BackBtnPressed(View view){
        ClosePlayerPage();
    }

    private void releasePlayer() {
        if (player != null) {
            player.setPlayWhenReady(false);
            player.release();
            player = null;
            trackSelector = null;

        }
    }

    public void ShowControl(View view){
        controller.show();
        controller.findViewById(R.id.prev_track).bringToFront();
    }

    public void OpenQualityView(View view){
        MappingTrackSelector.MappedTrackInfo mappedTrackInfo = trackSelector.getCurrentMappedTrackInfo();
        // Log.e("MAPPEDTRACK_INFO", "size: " + mappedTrackInfo.getRendererCount());
        for (int i = 0; i < mappedTrackInfo.getRendererCount(); i++) {
            int rendererType = mappedTrackInfo.getRendererType(i);
            boolean allowAdaptiveSelections =
                    rendererType == C.TRACK_TYPE_VIDEO
                            || (rendererType == C.TRACK_TYPE_AUDIO
                            && mappedTrackInfo.getTypeSupport(C.TRACK_TYPE_VIDEO)
                            == MappingTrackSelector.MappedTrackInfo.RENDERER_SUPPORT_NO_TRACKS);
            if(allowAdaptiveSelections) {
                TrackGroupArray trackGroups = mappedTrackInfo.getTrackGroups(i);
                for (int groupIndex = 0; groupIndex < trackGroups.length; groupIndex++) {
                    TrackGroup group = trackGroups.get(groupIndex);
                    for (int trackIndex = 0; trackIndex < group.length; trackIndex++) {
                        Log.e("CHECK_QUALITY", group.getFormat(trackIndex).toString());
                        Log.e("SMAPLE_MIME", group.getFormat(trackIndex).sampleMimeType);
                        Log.e("SMAPLE_Width", String.valueOf(group.getFormat(trackIndex).width));
                        Log.e("SMAPLE_Height", String.valueOf(group.getFormat(trackIndex).height));
                        Log.e("SMAPLE_Bitrate", String.valueOf(group.getFormat(trackIndex).bitrate));
                        Log.e("SMAPLE_ID", String.valueOf(group.getFormat(trackIndex).id));
                        //   Log.e("SMAPLE_METADATA", String.valueOf(group.getFormat(trackIndex).metadata.get(0).toString()));
                    }
                }
            }

        }
    }

    public void ChangeAspectRatio(View view){
       controller.hide();
       int tag = (int)playerView.getTag();// + 1;
       if(tag == ratios.length)
           tag = 0;

       playerView.setTag(tag+1);
        ConstraintLayout.LayoutParams lp = (ConstraintLayout.LayoutParams) playerPage.findViewById(R.id.ratio_root).getLayoutParams();
       lp.dimensionRatio = ratios[tag];
       ((TextView)(playerPage.findViewById(R.id.ratio_text))).setText(ratios[tag]);
        playerPage.findViewById(R.id.ratio_text).setVisibility(View.VISIBLE);
        new Timer().schedule(new TimerTask() {
            @Override
            public void run() {
                runOnUiThread(new Runnable() {

                    @Override
                    public void run() {
                        playerPage.findViewById(R.id.ratio_text).setVisibility(View.INVISIBLE);
                    }
                });
            }
        }, 500);
    }

    private void hideSystemUI() {

        decorView.setSystemUiVisibility(View.GONE);
       /* decorView.setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_FULLSCREEN
                       );*/
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);

    }

    private void showSystemUI() {
        decorView.setSystemUiVisibility(View.VISIBLE);
       /*decorView.setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN);*/
        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN);

    }

    public void OpenStream(View view){
        streamsPage.setVisibility(View.INVISIBLE);
        playerPage.setVisibility(View.VISIBLE);
        currentStreamIndex = (int)view.getTag();
        if (currentStreamIndex == 0)
            controller.findViewById(R.id.prev_track).setEnabled(false);
        String path = BASE_PATH + ((StreamsAdapter)streamsList.getAdapter()).getItem(currentStreamIndex);
        Uri uri = Uri.parse(path);

        mediaSource = buildMediaSource(uri);
        initializePlayer(true, true);
        hideSystemUI();
    }

    private void RestartPlayer(String path){
        Uri uri = Uri.parse(path);
        player.setPlayWhenReady(false);
        mediaSource = buildMediaSource(uri);
        player.prepare(mediaSource, true, true);
        player.setPlayWhenReady(true);
    }

    public void NextStream(View view){
        controller.findViewById(R.id.prev_track).setEnabled(true);//.setVisibility(View.VISIBLE);
        currentStreamIndex++;
        String path = BASE_PATH +((StreamsAdapter)streamsList.getAdapter()).getItem(currentStreamIndex);
        RestartPlayer(path);
        if(currentStreamIndex == ((StreamsAdapter)streamsList.getAdapter()).getItemCount()-1)
            //view.setVisibility(View.INVISIBLE);
            view.setEnabled(false);
    }

    public void PreviusStream(View view){
        controller.findViewById(R.id.next_track).setEnabled(true);//.setVisibility(View.VISIBLE);
        currentStreamIndex--;
        String path = BASE_PATH + ((StreamsAdapter)streamsList.getAdapter()).getItem(currentStreamIndex);
        RestartPlayer(path);
        if (currentStreamIndex == 0)
            view.setEnabled(false);
        // view.setVisibility(View.INVISIBLE);
    }



    @Override
    public void preparePlayback() {

    }

    @Override
    public void onVisibilityChange(int visibility) {

    }

    @Override
    public void onPlayerError(ExoPlaybackException error) {
        if (error.type == ExoPlaybackException.TYPE_SOURCE) {
            IOException cause = error.getSourceException();
            if (cause instanceof HttpDataSource.HttpDataSourceException) {
                // An HTTP error occurred.
                HttpDataSource.HttpDataSourceException httpError = (HttpDataSource.HttpDataSourceException) cause;
                // This is the request for which the error occurred.
                DataSpec requestDataSpec = httpError.dataSpec;
                // It's possible to find out more about the error both by casting and by
                // querying the cause.
                if (httpError instanceof HttpDataSource.InvalidResponseCodeException) {
                    // Cast to InvalidResponseCodeException and retrieve the response code,
                    // message and headers.
                } else {
                    // Try calling httpError.getCause() to retrieve the underlying cause,
                    // although note that it may be null.
                }
            }
        }
    }

    @Override
    public void onIsPlayingChanged(boolean isPlaying) {

        if (isPlaying) {

        } else {

        }
    }


    @Override
    public void onTrackSelectionChanged(boolean isDisabled, List<DefaultTrackSelector.SelectionOverride> overrides) {

    }
}
