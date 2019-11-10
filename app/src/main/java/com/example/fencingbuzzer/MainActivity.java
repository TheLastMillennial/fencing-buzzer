package com.example.fencingbuzzer;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Color;
import android.media.AudioDeviceInfo;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.text.Html;
import android.text.Spanned;
import android.text.method.LinkMovementMethod;
import android.view.KeyEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.ProgressBar;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.ToggleButton;

import java.util.concurrent.TimeUnit;

import static android.view.View.KEEP_SCREEN_ON;


public class MainActivity extends AppCompatActivity {

    boolean epeeMode=false;
    boolean headphonesOutput=true;
    boolean legacyBeep=false;
    boolean unlocked=true;
    boolean soundPlayed=false;
    boolean passed=false;
    int bladeType=0;//0=epee,1=foil

    private TextView hzText,bladeBtn,audioBtn,beepBtn,slideToUnlockText,creditsText,btnStateText;
    private Button helpBtn,beepButton;
    private SeekBar hzSeek,slideToUnlockSlider;
    private Spanned githubLink;

    private final double duration = .3; // seconds
    private final int sampleRate = 8000;
    private final double numSamples = duration * sampleRate;
    private final double sample[] = new double[(int)numSamples];
    private double freqOfTone = 1500; // hz
    private int prevProgress=10;

    private final byte generatedSnd[] = new byte[2 * (int)numSamples];

    Handler handler = new Handler();


    //KINDA LIKE THE MAIN
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //assets on screen
        creditsText = (TextView)findViewById(R.id.creditsTextView);
        btnStateText = (TextView)findViewById(R.id.button_state_text);
        hzText =(TextView)findViewById(R.id.hzTextView);
        hzSeek = (SeekBar) findViewById(R.id.hzSeekBar);
        slideToUnlockSlider = (SeekBar) findViewById(R.id.slideToUnlockSlider);
        slideToUnlockText = (TextView)findViewById(R.id.slideToUnlockText);
        bladeBtn=(Button) findViewById(R.id.bladeTypeToggle);
        audioBtn=(Button) findViewById(R.id.AudioOutputToggle);
        beepBtn=(Button) findViewById(R.id.beepButton);
        helpBtn=(Button) findViewById(R.id.helpButton);
        beepButton=(Button) findViewById(R.id.beepButton);

        //hyperlink to github
        githubLink = Html.fromHtml("<a href='https://github.com/TheLastMillennial/fencing-buzzer/tree/master'>©2019: Brian K. & Erik F.</a>");
        creditsText.setMovementMethod(LinkMovementMethod.getInstance());
        creditsText.setText(githubLink);

        //the only button on the screen sets off the beeping noise.
        //https://youtu.be/dFlPARW5IX8?t=22m12s
        beepButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                loopSound();

            }
        });

        helpBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this,android.R.style.Theme_DeviceDefault_Dialog_Alert);
                builder.setCancelable(true);

                builder.setTitle("Help:");
                builder.setMessage(
                        "Welcome to the fencing buzz-box emulator!\n\nThere are three buttons on the bottom of the screen. \n" +
                        " - The right button plays a test beep.\n" +
                        " - The middle button changes the audio output for the beep. \n" +
                        "    * Headphones mode will make the device send audio through the headphone jack. Please be aware that you must plug headphones into the passthrough port on the adapter to hear audio in this mode!\n" +
                        "    * Speakers mode will force the device to send audio through the built in speakers. Please not that this is NOT fully compatible with Android 4 and 5!\n" +
                        " - The button on the left changes which blade is being used and will change the beep behavior accordingly.\n" +
                        "\n" +
                        "There are two sliders.\n" +
                        " - The top slider 'locks' the screen by removing all clickable buttons and preventing phone from sleeping.\n" +
                        " - The bottom slider adjusts the tone of the beep.\n\n"+
                        "Please report any bugs to the app's Github page which you can reach by clicking the copyright date in the top left corner of the main screen.");
                builder.setNegativeButton("OK", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.cancel();
                    }
                });
                builder.show();
            }
        });

        slideToUnlockSlider.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                //makes sure user is sliding rather than just tapping the slider
                if (!(progress+1 == prevProgress || progress -1 == prevProgress))
                    progress=prevProgress;
                prevProgress=progress;
                slideToUnlockSlider.setProgress(progress);
                //locks or unlocks screen
                if (progress==0)
                    unlocked=false;
                else if(progress==slideToUnlockSlider.getMax())
                    unlocked=true;
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                if (unlocked){
                    hzSeek.setVisibility(View.VISIBLE);
                    hzText.setVisibility(View.VISIBLE);
                    bladeBtn.setVisibility(View.VISIBLE);
                    audioBtn.setVisibility(View.VISIBLE);
                    beepBtn.setVisibility(View.VISIBLE);
                    creditsText.setVisibility(View.VISIBLE);
                    helpBtn.setVisibility(View.VISIBLE);
                    slideToUnlockText.setText("<-- slide to lock screen -- ");
                    slideToUnlockText.setTextColor(Color.WHITE);
                    btnStateText.setVisibility(View.VISIBLE);
                    btnStateText.setTextColor(Color.DKGRAY);
                    getWindow().addFlags(WindowManager.LayoutParams.FLAG_ALLOW_LOCK_WHILE_SCREEN_ON);
                }else{
                    hzSeek.setVisibility(View.INVISIBLE);
                    hzText.setVisibility(View.INVISIBLE);
                    bladeBtn.setVisibility(View.INVISIBLE);
                    audioBtn.setVisibility(View.INVISIBLE);
                    beepBtn.setVisibility(View.INVISIBLE);
                    creditsText.setVisibility(View.INVISIBLE);
                    helpBtn.setVisibility((View.INVISIBLE));
                    slideToUnlockText.setText(" -- slide to unlock screen -->");
                    slideToUnlockText.setTextColor(Color.DKGRAY);
                    btnStateText.setVisibility(View.INVISIBLE);
                    getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
                }
            }
        });

        //for changing the tone pitch
        hzSeek.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                freqOfTone=progress;
                hzText.setText(""+progress+"hz");
                if (progress==0){
                    legacyBeep=true;
                    hzText.setText("Legacy Beep (Only plays through speakers)");
                }else{
                    legacyBeep=false;
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                //checks if user wants to play notification rather than tone
                if(legacyBeep){
                    playBeep();
                }else{
                    genTone();
                    playSound();
                }
            }
        });

        //toggle button for changing from epee to foil mode
        ToggleButton bladeToggle = (ToggleButton) findViewById(R.id.bladeTypeToggle);
        bladeToggle.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
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
                //device can support the feature!
                if (isChecked) {
                    // The toggle is enabled (speakers mode)
                    //checks if device can support this feature
                    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
                        //device is not supported, bring up popup window to tell them!
                        AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this,android.R.style.Theme_DeviceDefault_Dialog_Alert);
                        builder.setCancelable(true);
                        builder.setTitle("Outdated Device!");
                        builder.setMessage("Unfortunately, your device is outdated so an alternative beep using your notification sound has been used instead. Please update to Android 6 or higher to continue using the tone.");
                        builder.setNegativeButton("OK", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                dialog.cancel();
                            }
                        });
                        builder.show();
                        legacyBeep = true;
                    } else {
                        headphonesOutput = !headphonesOutput;
                    }
                } else {
                    // The toggle is disabled (headphones mode)
                    legacyBeep=false;
                    headphonesOutput = !headphonesOutput;
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
                        //commented out to prevent sound from randomly playing when screen state changes (ie rotation, locking, launching app)
                        //playSound();
                    }
                });
            }
        });
        thread.start();
    }

    //makes tone play 15 times
    public void loopSound(){
        for (int i=0;i<5;i++) {
            //if it's a modern device, play tone, otherwise use notification sound
            if(!legacyBeep) {
                playSound();
            }
            else {
                playBeep();
                if (i==2)
                    break;
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
        try{
            audioTrack.write(generatedSnd, 0, generatedSnd.length);

        }catch (Exception e){
            e.printStackTrace();
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && headphonesOutput==false) {
            AudioDeviceInfo mAudioOutputDevice = findAudioDevice(AudioManager.GET_DEVICES_OUTPUTS, AudioDeviceInfo.TYPE_BUILTIN_SPEAKER);
            audioTrack.setPreferredDevice(mAudioOutputDevice);// NOT COMPATIBLE WITH ANDROID 4!
        }

        try {
            audioTrack.play();
            TimeUnit.MILLISECONDS.sleep((long) (duration * 1000 + 50));
        } catch (Exception e) {
            e.printStackTrace();
        }
        audioTrack.release();
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
            if(epeeMode==false && soundPlayed==false)
                loopSound();
            soundPlayed=true;
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
            soundPlayed=false;
            return true;
        }
        return super.onKeyUp(keyCode, event);
    }

    //makes phone beep notification LEGACY
    public void playBeep(){
        //https://stackoverflow.com/questions/2618182/how-to-play-ringtone-alarm-sound-in-android
        try{
            //gets notification sound then plays it.
            Uri notification = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
            Ringtone r = RingtoneManager.getRingtone(getApplicationContext(), notification);

            r.play();
            TimeUnit.MILLISECONDS.sleep(300);
            r.stop();

        }catch (Exception e){
            e.printStackTrace();
        }

    }
}
