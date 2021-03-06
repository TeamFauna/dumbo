/**
 * AudioFingerprinter.java
 * EchoprintLib
 * 
 * Created by Alex Restrepo on 1/22/12.
 * Copyright (C) 2012 Grand Valley State University (http://masl.cis.gvsu.edu/)
 * 
 * Permission is hereby granted, free of charge, to any person obtaining a copy of
 * this software and associated documentation files (the "Software"), to deal in
 * the Software without restriction, including without limitation the rights to
 * use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies
 * of the Software, and to permit persons to whom the Software is furnished to do
 * so, subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package fauna.dumbo;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Hashtable;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.entity.StringEntity;
import org.json.JSONObject;

import android.app.Activity;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.util.Log;

/**
 * Main fingerprinting class<br>
 * This class will record audio from the microphone, generate the fingerprint code using a native library and query the data server for a match
 * 
 * @author Alex Restrepo (MASL)
 *
 */
public class Fingerprinter implements Runnable 
{
  public final static String META_SCORE_KEY = "meta_score";
  public final static String SCORE_KEY = "score";
  public final static String ALBUM_KEY = "release";
  public final static String TITLE_KEY = "track";
  public final static String TRACK_ID_KEY = "track_id";
  public final static String ARTIST_KEY = "artist";

  private final String SERVER_URL = "http://www.willhughes.ca/echo/query";

  private final int FREQUENCY = 11025;
  private final int CHANNEL = AudioFormat.CHANNEL_IN_MONO;
  private final int ENCODING = AudioFormat.ENCODING_PCM_16BIT;  

  private Thread thread;
  private volatile boolean isRunning = false;
  AudioRecord mRecordInstance = null;

  private short audioData[];
  private int bufferSize; 
  private int secondsToRecord;
  private int finished;
  private boolean success;
  private volatile boolean continuous;

  private AudioFingerprinterListener listener;

  /**
   * Constructor for the class
   * 
   * @param listener is the AudioFingerprinterListener that will receive the callbacks
   */
  public Fingerprinter(AudioFingerprinterListener listener)
  {
    this.listener = listener;
  }

  /**
   * Starts the listening / fingerprinting process using the default parameters:<br>
   * A single listening pass of 20 seconds 
   */
  public void fingerprint()
  {
    // set dafault listening time to 20 seconds
    this.fingerprint(20);
  }

  /**
   * Starts a single listening / fingerprinting pass
   * 
   * @param seconds the seconds of audio to record.
   */
  public void fingerprint(int seconds)
  {
    // no continuous listening
    this.fingerprint(seconds, true);
  }

  /**
   * Starts the listening / fingerprinting process
   * 
   * @param seconds the number of seconds to record per pass
   * @param continuous if true, the class will start a new fingerprinting pass after each pass
   */
  public void fingerprint(int seconds, boolean continuous)
  {
    if(this.isRunning)
      return;

    this.continuous = continuous;

    // cap to 30 seconds max, 10 seconds min.
    this.secondsToRecord = 30;//Math.max(Math.min(seconds, 30), 10);

    // start the recording thread
    thread = new Thread(this);
    thread.start();
  }

  /**
   * stops the listening / fingerprinting process if there's one in process
   */
  public void stop() 
  {
    this.continuous = false;
    if(mRecordInstance != null)
      mRecordInstance.stop();
  }


  /* FOR TESTING */
  private static int[] mSampleRates = new int[] { 8000, 11025, 22050, 44100 };
  //private static int[] mSampleRates = new int[] {  11025 };
  public AudioRecord findAudioRecord() {
      for (int rate : mSampleRates) {
          for (short audioFormat : new short[] { AudioFormat.ENCODING_PCM_8BIT, AudioFormat.ENCODING_PCM_16BIT }) {
              for (short channelConfig : new short[] { AudioFormat.CHANNEL_IN_MONO, AudioFormat.CHANNEL_IN_STEREO }) {
                  try {
                      Log.d("FingerprinterTest", "Attempting rate " + rate + "Hz, bits: " + audioFormat + ", channel: "
                              + channelConfig);
                      int bufferSize = AudioRecord.getMinBufferSize(rate, channelConfig, audioFormat);

                      if (bufferSize != AudioRecord.ERROR_BAD_VALUE) {
                          // check if we can instantiate and have a success
                          Log.d("FingerprinterTestSuc", "Good Values, rate: " + rate + "Hz, bits: " + audioFormat + ", channel: " + channelConfig + ", BufferSize: " + bufferSize); 
                          AudioRecord recorder = new AudioRecord(MediaRecorder.AudioSource.MIC, rate, channelConfig, audioFormat, bufferSize);

                          if (recorder.getState() == AudioRecord.STATE_INITIALIZED)
                              Log.d("FingerprinterTestSucGood", "GOOD!");
                          else
                              Log.d("FingerprinterTestSucBad", "NO GOOD! :(");
                      }
                  } catch (Exception e) {
                      Log.e("Fingerprinter", rate + "Exception, keep trying.",e);
                  }
              }
          }
      }
      return null;
  }

  /**
   * The main thread<br>
   * Records audio and generates the audio fingerprint, then it queries the server for a match and forwards the results to the listener.
   */
  public void run() 
  {
    this.isRunning = true;
    this.finished = 0;
    this.success = false;
    try 
    {     
      // create the audio buffer
      // get the minimum buffer size
      int minBufferSize = AudioRecord.getMinBufferSize(FREQUENCY, CHANNEL, ENCODING);

      // and the actual buffer size for the audio to record
      // frequency * seconds to record.
      bufferSize = Math.max(minBufferSize, this.FREQUENCY * 10);//this.secondsToRecord);

      audioData = new short[bufferSize*3];

      Log.d("Fingerprinter", "CREATE AUDIORECORD");

      // start recorder
      mRecordInstance = new AudioRecord(
                MediaRecorder.AudioSource.DEFAULT,
                FREQUENCY, CHANNEL, 
                ENCODING, bufferSize*3);//minBufferSize); //TODO: minbuffersize?

      //AudioRecord n = findAudioRecord();
      
      if (mRecordInstance == null) {
       Log.d("Fingerprinter", "NO RECORD INSTANCE!!!! LE SIGH");
       return;
      }

      Log.d("Fingerprinter", "DONE: creating AUDIORECORD");
      willStartListening();

      mRecordInstance.startRecording();
      boolean firstRun = true;
      int runs = 1;
      int samplesIn = 0;
      do 
      {   
        try
        {
          willStartListeningPass();

          Log.d("Fingerprinter","LOOP");
          long time = System.currentTimeMillis();
          // fill audio buffer with mic data.
          Log.d("Fingerprinter","BUffersize: " + bufferSize + " samplesIn: " + samplesIn);
          do 
          {         
            int req = bufferSize*runs - samplesIn;
            Log.d("Fingerprinter","BUffersize: " + bufferSize + " samplesIn: " + samplesIn + " req: " +req);
            samplesIn += mRecordInstance.read(audioData, samplesIn, req);

            if(mRecordInstance.getRecordingState() == AudioRecord.RECORDSTATE_STOPPED)
              break;

            if(samplesIn >= bufferSize * runs && runs < 3) {
              Log.d("Fingerprinter","BUffersize: " + bufferSize + " samplesIn: " + samplesIn + " req: " +req);
              Log.d("FingerprinterThread","Run: " +runs);
              runs += 1;
              final int fruns = runs - 1;
              final int nSamples = samplesIn;
              Thread newT = new Thread() {
                @Override
                public void run() { 
                  runAllPass(nSamples, fruns); 
                }
              };
              newT.start();

            }
          } 
          while (samplesIn < bufferSize*3);       
          if (!this.success) { 
              Log.d("FingerprinterThread","Run: " +runs);
              final int fruns = 3;
              final int nSamples = samplesIn;
              Thread newT = new Thread() {
                @Override
                public void run() { 
                  runAllPass(nSamples, fruns); 
                }
              };
              newT.start();
          }

          Log.d("Fingerprinter", "Audio recorded: " + (System.currentTimeMillis() - time) + " millis");

          // see if the process was stopped.
          if(mRecordInstance.getRecordingState() == AudioRecord.RECORDSTATE_STOPPED || (!firstRun && !this.continuous))
            break;

          // create an echoprint codegen wrapper and get the code
            firstRun = false;

            didFinishListeningPass();

            if (runs >= 3) {
              stop();
            }
        }
        catch(Exception e)
        {
          e.printStackTrace();
          Log.e("Fingerprinter", e.getLocalizedMessage());

          didFailWithException(e);
        }
      }
      while (this.continuous);
    } 
    catch (Exception e) 
    {
      e.printStackTrace();
      Log.e("Fingerprinter", e.getLocalizedMessage());

      didFailWithException(e);
    }

    if(mRecordInstance != null)
    {
      mRecordInstance.stop();
      mRecordInstance.release();
      mRecordInstance = null;
    }
    this.isRunning = false;

    while (this.finished < 3 && !this.success) { 
    }
    didFinishListening();
  }

  private void runAllPass(int samplesIn, int nums) { 
    try {
          Codegen codegen = new Codegen();
            String code = codegen.generate(audioData, samplesIn);

            if(code.length() == 0)
            {
              // no code?
              // not enough audio data?
              return;
            }

            didGenerateFingerprintCode(code);
            
            Log.d("Fingerprinter", "CODE CREATED: " + code);

            // fetch data from echonest

            JSONObject requestJson = new JSONObject();
            requestJson.put("version", "4.12");
            requestJson.put("length", "1");
            requestJson.put("string", code);

            StringEntity se = new StringEntity(requestJson.toString());

            String urlstr = SERVER_URL;      
            HttpClient client = new DefaultHttpClient();
            HttpPost post = new HttpPost(urlstr);

            post.setEntity(se);
            post.setHeader("Accept", "application/json");
            post.setHeader("Content-type", "application/json");

            // get response
            HttpResponse response = client.execute(post);                
            // Examine the response status
                Log.d("Fingerprinter",response.getStatusLine().toString());

                // Get hold of the response entity
                HttpEntity entity = response.getEntity();
                // If the response does not enclose an entity, there is no need
                // to worry about connection release

                String result = "";
                if (entity != null) 
                {
                    // A Simple JSON Response Read
                    InputStream instream = entity.getContent();
                    result= convertStreamToString(instream);
                    // now you have the string representation of the HTML request
                    instream.close();
                }


            Log.d("Fingerprinter", "RESULTS: " + result);
            // parse JSON
            JSONObject jobj = new JSONObject(result);

            if(jobj.has("code"))
              Log.d("Fingerprinter", "Response code:" + jobj.getInt("code") + " (" + this.messageForCode(jobj.getInt("code")) + ")");

            if(jobj.getBoolean("success"))
            {
              if(jobj.has("match"))
              {
                this.success = true;
                stop();
                JSONObject match = jobj.getJSONObject("match");
                match.put("flength", nums * 10);
                didFindMatchForCode(match, code);
              }
              else
                didNotFindMatchForCode(code);           
            }         
            else
            {
              didFailWithException(new Exception("Unknown error"));
            }
    }
    catch (Exception e) {
      throw new RuntimeException(e);
    }
    this.finished+=1;
  }

  private static String convertStreamToString(InputStream is) 
  {
      /*
       * To convert the InputStream to String we use the BufferedReader.readLine()
       * method. We iterate until the BufferedReader return null which means
       * there's no more data to read. Each line will appended to a StringBuilder
       * and returned as String.
       */
      BufferedReader reader = new BufferedReader(new InputStreamReader(is));
      StringBuilder sb = new StringBuilder();

      String line = null;
      try {
          while ((line = reader.readLine()) != null) {
              sb.append(line + "\n");
          }
      } catch (IOException e) {
          e.printStackTrace();
      } finally {
          try {
              is.close();
          } catch (IOException e) {
              e.printStackTrace();
          }
      }
      return sb.toString();
  }

  private String messageForCode(int code)
  {
    try{
      String codes[] = {
          "NOT_ENOUGH_CODE", "CANNOT_DECODE", "SINGLE_BAD_MATCH", 
          "SINGLE_GOOD_MATCH", "NO_RESULTS", "MULTIPLE_GOOD_MATCH_HISTOGRAM_INCREASED",
          "MULTIPLE_GOOD_MATCH_HISTOGRAM_DECREASED", "MULTIPLE_BAD_HISTOGRAM_MATCH", "MULTIPLE_GOOD_MATCH"
          }; 

      return codes[code];
    }
    catch(ArrayIndexOutOfBoundsException e)
    {
      return "UNKNOWN";
    }
  }

  private void didFinishListening()
  {
    if(listener == null)
      return;

    if(listener instanceof Activity)
    {
      Activity activity = (Activity) listener;
      activity.runOnUiThread(new Runnable() 
      {   
        public void run() 
        {
          listener.didFinishListening();
        }
      });
    }
    else
      listener.didFinishListening();
  }

  private void didFinishListeningPass()
  {
    if(listener == null)
      return;

    if(listener instanceof Activity)
    {
      Activity activity = (Activity) listener;
      activity.runOnUiThread(new Runnable() 
      {   
        public void run() 
        {
          listener.didFinishListeningPass();
        }
      });
    }
    else
      listener.didFinishListeningPass();
  }

  private void willStartListening()
  {
    if(listener == null)
      return;

    if(listener instanceof Activity)
    {
      Activity activity = (Activity) listener;
      activity.runOnUiThread(new Runnable() 
      {   
        public void run() 
        {
          listener.willStartListening();
        }
      });
    }
    else  
      listener.willStartListening();
  }

  private void willStartListeningPass()
  {
    if(listener == null)
      return;

    if(listener instanceof Activity)
    {
      Activity activity = (Activity) listener;
      activity.runOnUiThread(new Runnable() 
      {   
        public void run() 
        {
          listener.willStartListeningPass();
        }
      });
    }
    else
      listener.willStartListeningPass();
  }

  private void didGenerateFingerprintCode(final String code)
  {
    if(listener == null)
      return;

    if(listener instanceof Activity)
    {
      Activity activity = (Activity) listener;
      activity.runOnUiThread(new Runnable() 
      {   
        public void run() 
        {
          listener.didGenerateFingerprintCode(code);
        }
      });
    }
    else
      listener.didGenerateFingerprintCode(code);
  }

  private void didFindMatchForCode(final JSONObject table, final String code)
  {
    if(listener == null)
      return;

    if(listener instanceof Activity)
    {
      Activity activity = (Activity) listener;
      activity.runOnUiThread(new Runnable() 
      {   
        public void run() 
        {
          listener.didFindMatchForCode(table, code);
        }
      });
    }
    else
      listener.didFindMatchForCode(table, code);
  }

  private void didNotFindMatchForCode(final String code)
  {
    if(listener == null)
      return;

    if(listener instanceof Activity)
    {
      Activity activity = (Activity) listener;
      activity.runOnUiThread(new Runnable() 
      {   
        public void run() 
        {
          listener.didNotFindMatchForCode(code);
        }
      });
    }
    else
      listener.didNotFindMatchForCode(code);
  }

  private void didFailWithException(final Exception e)
  {
    if(listener == null)
      return;

    if(listener instanceof Activity)
    {
      Activity activity = (Activity) listener;
      activity.runOnUiThread(new Runnable() 
      {   
        public void run() 
        {
          listener.didFailWithException(e);
        }
      });
    }
    else
      listener.didFailWithException(e);
  }

  /**
   * Interface for the fingerprinter listener<br>
   * Contains the different delegate methods for the fingerprinting process
   * @author Alex Restrepo
   *
   */
  public interface AudioFingerprinterListener
  {   
    /**
     * Called when the fingerprinter process loop has finished
     */
    public void didFinishListening();

    /**
     * Called when a single fingerprinter pass has finished
     */
    public void didFinishListeningPass();

    /**
     * Called when the fingerprinter is about to start
     */
    public void willStartListening();

    /**
     * Called when a single listening pass is about to start
     */
    public void willStartListeningPass();

    /**
     * Called when the codegen libary generates a fingerprint code
     * @param code the generated fingerprint as a zcompressed, base64 string
     */
    public void didGenerateFingerprintCode(String code);

    /**
     * Called if the server finds a match for the submitted fingerprint code 
     * @param table a hashtable with the metadata returned from the server
     * @param code the submited fingerprint code
     */
    public void didFindMatchForCode(JSONObject table, String code);

    /**
     * Called if the server DOES NOT find a match for the submitted fingerprint code
     * @param code the submited fingerprint code
     */
    public void didNotFindMatchForCode(String code);

    /**
     * Called if there is an error / exception in the fingerprinting process
     * @param e an exception with the error
     */
    public void didFailWithException(Exception e);
  }
}
