package com.example.livefeed;

import android.app.PictureInPictureParams;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.util.Rational;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import org.videolan.libvlc.LibVLC;
import org.videolan.libvlc.Media;
import org.videolan.libvlc.MediaPlayer;
import org.videolan.libvlc.util.VLCVideoLayout;

import java.io.File;
import java.util.ArrayList;

public class VlcPlayerActivity extends AppCompatActivity {

    private LibVLC libVLC;
    private MediaPlayer mediaPlayer;

    private VLCVideoLayout vlcVideoLayout;
    private LinearLayout header;
    private ScrollView scrollView;
    private FrameLayout frameLayout;

    private RelativeLayout.LayoutParams originalLayoutParams;

    private ImageButton recordButton, reconnectButton;
    private TextView statusText, deviceInfoText;

    private boolean isRecording = false;
    private String url;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_vlc_ui);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        initializeViews();
        initializePlayer();

        Intent intent = getIntent();
        if (intent != null && intent.getStringExtra("URL") != null) {
            url = intent.getStringExtra("URL");
        }

        deviceInfoText.setText(url);
        connectToStream();

        recordButton.setOnClickListener(view -> {
            if (isRecording) {
                stopRecording();
            } else {
                startRecording();
            }
        });

        reconnectButton.setOnClickListener(view -> connectToStream());

        findViewById(R.id.btnBack).setOnClickListener(view -> finish());

        mediaPlayer.setEventListener(event -> runOnUiThread(() -> {
            switch (event.type) {
                case MediaPlayer.Event.Playing:
                    updateStatus("Connected", Color.GREEN);
                    break;
                case MediaPlayer.Event.Buffering:
                    updateStatus("Buffering...", Color.YELLOW);
                    break;
                case MediaPlayer.Event.Stopped:
                case MediaPlayer.Event.EndReached:
                case MediaPlayer.Event.EncounteredError:
                    updateStatus("Disconnected", Color.RED);
                    break;
            }
        }));
    }

    private void initializeViews() {
        vlcVideoLayout = findViewById(R.id.videoPlayer);
        header = findViewById(R.id.header);
        scrollView = findViewById(R.id.scrollView);
        frameLayout = findViewById(R.id.videoContainer);
        recordButton = findViewById(R.id.record);
        reconnectButton = findViewById(R.id.btnreconnect);
        statusText = findViewById(R.id.isConnected);
        deviceInfoText = findViewById(R.id.deviceinfo);
    }

    private void initializePlayer() {
        ArrayList<String> options = new ArrayList<>();
        options.add("--rtsp-tcp");
        options.add("--network-caching=300");
        options.add("--no-drop-late-frames");
        options.add("--no-skip-frames");

        libVLC = new LibVLC(this, options);
        mediaPlayer = new MediaPlayer(libVLC);
        mediaPlayer.attachViews(vlcVideoLayout, null, false, false);
    }

    private void connectToStream() {
        Media media = createBaseMedia(url);
        mediaPlayer.setMedia(media);
        mediaPlayer.play();
        Toast.makeText(this, "Playing: " + url, Toast.LENGTH_SHORT).show();
    }

    private Media createBaseMedia(String streamUrl) {
        Media media = new Media(libVLC, Uri.parse(streamUrl));
        media.setHWDecoderEnabled(true, false);
        media.addOption(":network-caching=300");
        return media;
    }

    private void startRecording() {
        if (mediaPlayer == null || isRecording) return;

        File outputDir = getExternalFilesDir(Environment.DIRECTORY_MOVIES);
        if (outputDir == null) {
            Toast.makeText(this, "Storage access error", Toast.LENGTH_SHORT).show();
            return;
        }

        String filePath = new File(outputDir, "recording_" + System.currentTimeMillis() + ".mp4").getAbsolutePath();

        Media media = createBaseMedia(url);
        String sout = ":sout=#duplicate{dst=display,dst=standard{access=file,mux=mp4,dst=" + filePath + "}}";
        media.addOption(sout);
        media.addOption(":sout-keep");

        mediaPlayer.setMedia(media);
        mediaPlayer.play();

        isRecording = true;
        Toast.makeText(this, "Recording started", Toast.LENGTH_SHORT).show();
    }

    private void stopRecording() {
        if (mediaPlayer == null || !isRecording) return;

        mediaPlayer.stop();
        connectToStream();

        isRecording = false;
        Toast.makeText(this, "Recording stopped", Toast.LENGTH_SHORT).show();
    }

    private void updateStatus(String text, int color) {
        statusText.setText(text);
        statusText.setTextColor(color);
    }

    private void enterPictureInPictureModeIfAvailable() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Rational aspectRatio = new Rational(16, 9);
            PictureInPictureParams params = new PictureInPictureParams.Builder()
                    .setAspectRatio(aspectRatio)
                    .build();
            enterPictureInPictureMode(params);
        }
    }

    @Override
    public void onUserLeaveHint() {
        enterPictureInPictureModeIfAvailable();
    }

    @Override
    public void onPictureInPictureModeChanged(boolean isInPiPMode) {
        super.onPictureInPictureModeChanged(isInPiPMode);

        if (isInPiPMode) {
            if (frameLayout.getLayoutParams() instanceof RelativeLayout.LayoutParams) {
                originalLayoutParams = (RelativeLayout.LayoutParams) frameLayout.getLayoutParams();
            }

            header.setVisibility(View.GONE);
            scrollView.setVisibility(View.GONE);

            frameLayout.setLayoutParams(new RelativeLayout.LayoutParams(
                    RelativeLayout.LayoutParams.MATCH_PARENT,
                    RelativeLayout.LayoutParams.MATCH_PARENT
            ));
            frameLayout.bringToFront();
        } else {
            header.setVisibility(View.VISIBLE);
            scrollView.setVisibility(View.VISIBLE);

            if (originalLayoutParams != null) {
                frameLayout.setLayoutParams(originalLayoutParams);
            }
        }
    }

    private void releasePlayer() {
        if (mediaPlayer != null) {
            mediaPlayer.stop();
            mediaPlayer.release();
            mediaPlayer = null;
        }
        if (libVLC != null) {
            libVLC.release();
            libVLC = null;
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N && isInPictureInPictureMode()) return;
        releasePlayer();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        releasePlayer();
    }
}
