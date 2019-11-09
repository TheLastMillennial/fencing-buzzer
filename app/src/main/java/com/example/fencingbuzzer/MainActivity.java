package com.example.fencingbuzzer;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.content.DialogInterface;
import android.media.AudioDeviceInfo;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.media.MediaPlayer;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Vibrator;
import android.provider.MediaStore;
import android.view.KeyEvent;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.ProgressBar;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.ToggleButton;

import org.w3c.dom.Text;

import java.util.concurrent.TimeUnit;


public class MainActivity extends AppCompatActivity {

    boolean epeeMode=false;
    boolean headphonesOutput=true;
    int bladeType=0;//0=epee,1=foil

    private TextView textView;
    private ProgressBar progressBar;
    private SeekBar seekBar;

    private final double duration = .3; // seconds
    private final int sampleRate = 8000;
    private final double numSamples = duration * sampleRate;
    private final double sample[] = new double[(int)numSamples];
    public double freqOfTone = 1500; // hz

    private final byte generatedSnd[] = new byte[2 * (int)numSamples];

    Handler handler = new Handler();


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //the only button on the screen sets off the beeping noise.
        //https://youtu.be/dFlPARW5IX8?t=22m12s
        Button beepButton=(Button) findViewById(R.id.beepButton);
        beepButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                loopSound();

            }
        });

        textView=(TextView)findViewById(R.id.hzTextView);
        seekBar = (SeekBar) findViewById(R.id.hzSeekBar);

        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                freqOfTone=progress;
                textView.setText(""+progress+"hz");
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                genTone();
                playSound();
            }
        });

        //toggle button for changing from epee to foil mode
        ToggleButton bladeToggle = (ToggleButton) findViewById(R.id.bladeTypeToggle);
        bladeToggle.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
                builder.setCancelable(true);
                builder.setTitle("Unimplemented Feature");
                builder.setMessage("This feature has no effect right now.");
                builder.setNegativeButton("OK", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.cancel();
                    }
                });

                if (isChecked) {
                    // The toggle is enabled (foil mode)
                    epeeMode=!epeeMode;
                } else {
                    // The toggle is disabled (epee mode)
                    epeeMode=!epeeMode;
                }
            }
        });

        //toggle button for changing audio outputs
        final ToggleButton audioToggle = (ToggleButton) findViewById(R.id.AudioOutputToggle);
        audioToggle.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                //checks if device can support this feature
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M){
                    //device is not supported, bring up popup window to tell them!
                    AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
                    builder.setCancelable(true);
                    builder.setTitle("Outdated Device!");
                    builder.setMessage("Unfortunately, your device is outdated so this feature will have no effect. Please update to Android 6 or higher to use it.");
                    builder.setNegativeButton("OK", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.cancel();
                        }
                    });
                    builder.show();
                    audioToggle.setChecked(false);
                }else {
                    //device can support the feature!
                    if (isChecked) {
                        // The toggle is enabled (speakers mode)

                        headphonesOutput = !headphonesOutput;

                    } else {
                        // The toggle is disabled (headphones mode)
                        headphonesOutput = !headphonesOutput;
                    }
                }
            }
        });


    }

    @Override
    protected void onResume() {
        super.onResume();

        // Use a new tread as this can take a while
        final Thread thread = new Thread(new Runnable() {
            public void run() {
                genTone();
                handler.post(new Runnable() {

                    public void run() {
                        playSound();
                    }
                });
            }
        });
        thread.start();
    }

    //makes tone play 15 times
    public void loopSound(){
        for (int i=0;i<5;i++) {
            playSound();
            try {
                TimeUnit.MILLISECONDS.sleep((long)(duration*1000+50));
            }catch (Exception e){
                e.printStackTrace();
            }

        }
    }




    void genTone(){
        // fill out the array
        for (int i = 0; i < numSamples; ++i) {
            sample[i] = Math.sin(2 * Math.PI * i / (sampleRate/freqOfTone));
        }

        // convert to 16 bit pcm sound array
        // assumes the sample buffer is normalised.
        int idx = 0;
        for (final double dVal : sample) {
            // scale to maximum amplitude
            final short val = (short) ((dVal * 32767));
            // in 16 bit wav PCM, first byte is the low order byte
            generatedSnd[idx++] = (byte) (val & 0x00ff);
            generatedSnd[idx++] = (byte) ((val & 0xff00) >>> 8);

        }
    }

    //plays a tone
    void playSound(){
        final AudioTrack audioTrack = new AudioTrack(
                AudioManager.STREAM_MUSIC,
                sampleRate,
                AudioFormat.CHANNEL_OUT_MONO,
                AudioFormat.ENCODING_PCM_16BIT,
                generatedSnd.length,
                AudioTrack.MODE_STATIC);
        audioTrack.write(generatedSnd, 0, generatedSnd.length);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && headphonesOutput==false) {
            AudioDeviceInfo mAudioOutputDevice = findAudioDevice(AudioManager.GET_DEVICES_OUTPUTS, AudioDeviceInfo.TYPE_BUILTIN_SPEAKER);
            audioTrack.setPreferredDevice(mAudioOutputDevice);// NOT COMPATIBLE WITH ANDROID 4!
        }

        audioTrack.play();

    }

    //finds the audio device
    private AudioDeviceInfo findAudioDevice(int deviceFlag, int deviceType) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            AudioManager manager = (AudioManager) this.getSystemService(Context.AUDIO_SERVICE);
            AudioDeviceInfo[] adis = manager.getDevices(deviceFlag);
            for (AudioDeviceInfo adi : adis) {

                if (adi.getType() == deviceType) {
                    return adi;
                }

            }
        }
        return null;
    }

    //checks if button on blade is pressed (used for FOIL)
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if(keyCode == KeyEvent.KEYCODE_HEADSETHOOK ) {
            //displays button status as closed
            TextView textViewMain = findViewById(R.id.button_state_text);
            textViewMain.setText("Closed");
            //plays beep
            if(epeeMode==false)
                loopSound();
        }
        return super.onKeyDown(keyCode, event);
    }

    //checks if button on blade is depressed (used for EPEE)
    public boolean 	onKeyUp(int keyCode, KeyEvent event) {

        if(keyCode == KeyEvent.KEYCODE_HEADSETHOOK){
            //displays button status as open
            TextView textViewMain = findViewById(R.id.button_state_text);
            textViewMain.setText("Open");
            //plays beep on phone
            if(epeeMode==true)
                loopSound();
            return true;
        }
        return super.onKeyUp(keyCode, event);
    }

    //makes phone beep notification UNUSED CURRENTLY
    public void playBeep(){

        //https://stackoverflow.com/questions/2618182/how-to-play-ringtone-alarm-sound-in-android
        try{

            //plays beep and vibrates phone
            Uri notification = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
            Ringtone r = RingtoneManager.getRingtone(getApplicationContext(), notification);

            r.play();
            //vibrateDevice();
            TimeUnit.MILLISECONDS.sleep(300);

            r.stop();

        }catch (Exception e){
            e.printStackTrace();
        }

    }
}
