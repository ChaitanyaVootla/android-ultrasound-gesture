package com.example.monster.airgesture;

import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioRecord;
import android.media.AudioTrack;
import android.media.MediaRecorder;
import android.media.RemoteControlClient;
import android.os.AsyncTask;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;


import java.util.Arrays;

import edu.emory.mathcs.jtransforms.fft.DoubleFFT_1D;


public class MainActivity extends ActionBarActivity {
    boolean isRecording=false;
    AudioRecord recorder;
    record recorderobj;
    int  i=1;
    private final int duration = 40; // seconds
    private final int sampleRate = 44100; // Hz
    private final int numSamples = duration * sampleRate;
    private final double sample[] = new double[numSamples];
    private final double freqOfTone = 18000; // Hz
    private final byte generatedSnd[] = new byte[2 * numSamples];
    public static TextView tv;
    double[] abs;
    double diff;
    int N=2048;
    String res="";
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        tv=(TextView)findViewById(R.id.textView2);
    }

    //runs after pressing the record button
    public void startRecording(View view){
        recorder = new AudioRecord(MediaRecorder.AudioSource.MIC,
                44100, AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT, 4096*2);
        recorderobj=new record();
        recorderobj.execute();
        Start();
        view.setClickable(false);
        Button btn=(Button)findViewById(R.id.button);
        btn.setClickable(true);
    }

    //runs when the stop button is pressed
    public void stopRecording(View view) {
        recorderobj.cancel(true);
        isRecording=false;
        recorder.stop();
        recorder.release();
        Log.i("audio","Stopped");
        view.setClickable(false);
        Button btn=(Button)findViewById(R.id.button2);
        btn.setClickable(true);
    }

    private class record extends AsyncTask<Object,String,String> {

        @Override
        protected String doInBackground(Object... params) {
            int bufferSize = AudioRecord.getMinBufferSize(44100,
                    AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT);

            Log.i("audio","buffersize:"+bufferSize);
            //'rec' variable is used to store the recording(4096 samples)
            short[] rec=new short[4096];
            //'re' and 're2' variables are used to store 2048 samples each taken from 'rec' variable
            short[] re=new short[2048];
            short[] re2=new short[2048];
            double[] im=new double[2048];
            double[] real=new double[2048];
            //used to store the magnitude of bins computed from the real and imaginary parts
            abs=new double[1024];

            recorder.startRecording();
            isRecording=true;
            DoubleFFT_1D fft2 = new DoubleFFT_1D(2048);
            while (isRecording)
            {
                //record 4096 samples
                recorder.read(rec,0,4096);
                //break them into two 2048 sample bundles
                re= Arrays.copyOfRange(rec,0,2048);
                re2= Arrays.copyOfRange(rec,2048,4096);
                //convert the stored array of shorts to doubles for the fft function
                real=floatMe(re);
                //runs the fft function
                fft2.realForward(real);
                //here the 'im' variable is redundant and can be removed, processfft checks for doppler shift after combining real and imaginary parts to get the magnitude of each bin and storing the values in 'abs' variable
                processfft(real, im);
                publishProgress(res);

                //this block of code does the same as above for the 're2' variable
                real=floatMe(re2);
                fft2.realForward(real);
                processfft(real,im);
                publishProgress(res);
                //i stores the number of iterations
                i++;
            }
            return null;
        }

        @Override
        protected void onProgressUpdate(String... values) {
            super.onProgressUpdate(values);
            //Log.i("gesture",res);
            //here the variable 'res' in which the gesture is stored (either towards or away or none in case of no gesture) is displayed
            tv.setText(res);
        }
    }

    private void processfft(double[] real, double[] im) {
        int i=0;
        abs=new double[1024];
        double max=0;
        int index=0;
        //in this loop the real and imaginary parts are used to get the magnitude using the formula real^2+imaginary^2=magnitude^2
        for(i=0;i<1023;i++)
        {
            abs[i]=Math.sqrt(Math.pow(real[2*i], 2) + Math.pow(real[2*i+1], 2));
            //this is to find the peak in the fft as the peak is the 20khz sound that was emitted
            if(abs[i]>max) {
                //max is the value of the peak bin
                max = abs[i];
                //index stores the array index which has the 20khz peak value(magnitude/amplitude)
                index=i;
            }
        }
        //Log.i("audio","max="+max+" index="+index);
        if(index>100) {
            //suml and sumr variables are not used
            double suml = 0, sumr = 0;
            boolean flag=false;
            //this checks if the bin value right next to the index or before the index has a value which is greater than a tenth of the peak bin's(index's) value
            if(abs[index-1] > max * 0.100 || abs[index+1] > max* 0.100)
                flag=true;
            //the following checks if the difference between the bins adjacent to the index/peak bin have enough difference between them to declare a gesture
            //the values of 9 and 11 in the if and else blocks are a result of careful examination of the bin values surrounding the peak bin and may vary between devices and surroundings
            //so far the values below worked fine in most the surroundings we tested in and in the following devices s4, oneplus one and samsung galaxy grand
            if(flag && abs[index+1]-abs[index-1]>9) {
                res = "TOWARDS sl=" + suml + "sr=" + sumr;
                Log.i("gesture", "index:" + abs[index] + " left:" + abs[index - 1] + " " + abs[index - 2] + " " + abs[index - 3] + " " + abs[index - 4] + "");
                Log.i("gesture"," right:"+abs[index+1]+" "+abs[index+2]+" "+abs[index+3]+" "+abs[index+4]+"");
            }
            else if(flag && abs[index-1]-abs[index+1]>11){
                res = "AWAY sl="+suml+"sr="+sumr;
                Log.i("gesture", "index:" + abs[index] + " left:" + abs[index - 1] + " " + abs[index - 2] + " " + abs[index - 3] + " " + abs[index - 4] + "");
                Log.i("gesture"," right:"+abs[index+1]+" "+abs[index+2]+" "+abs[index+3]+" "+abs[index+4]+"");
            }
            else
                res="";
            diff=abs[index-1]-abs[index+1];
            Log.i("gesture",res+"--index="+index+"  diff:"+diff);
        }
    }

    //converts array of shorts to array of double
    public static double[] floatMe(short[] pcms) {
        double[] real=new double[pcms.length];
        for (int i = 0; i < pcms.length; i++) {
            real[i] = pcms[i]/32768.0;
        }
        return real;
    }

    //genTone() generates the audio to be played and then plays it
    public void Start() {
        final Thread thread = new Thread(new Runnable() {
            public void run() {

                genTone();
                //while(isRecording){
                    final AudioTrack audioTrack = new AudioTrack(
                            AudioManager.STREAM_MUSIC, sampleRate,
                            AudioFormat.CHANNEL_OUT_MONO,
                            AudioFormat.ENCODING_PCM_16BIT,
                            generatedSnd.length, AudioTrack.MODE_STATIC);
                    audioTrack.write(generatedSnd, 0, generatedSnd.length);
                    audioTrack.play();
                    Log.i("audio","playing now");
                //}
            }

            private void genTone() {
                // fill out the array
                for (int i = 0; i < numSamples; ++i) {
                    sample[i] = Math.sin(2 * Math.PI * i
                            / (sampleRate / freqOfTone));
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
                Log.i("audio","Tone Generated");
            }
        });
        thread.start();
    }

    //any code below this comment can be neglected
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}
