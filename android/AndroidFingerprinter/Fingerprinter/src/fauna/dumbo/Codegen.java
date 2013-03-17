package fauna.dumbo;

public class Codegen
{
    private final float normalizingValue = Short.MAX_VALUE;
 
    native String codegen(float data[], int numSamples);
 
    static
    {
            System.loadLibrary("echoprint-jni");
    }
 
    /**
     * Invoke the echoprint native library and generate the fingerprint code.<br>
     * Echoprint REQUIRES PCM encoded audio with the following parameters:<br>
     * Frequency: 11025 khz<br>
     * Data: MONO - PCM enconded float array
     *
     * @param data PCM encoded data as floats [-1, 1]
     * @param numSamples number of PCM samples at 11025 KHz
     * @return The generated fingerprint as a compressed - base64 string.
     */
    public String generate(float data[], int numSamples)
    {
        return codegen(data, numSamples);
    }
 
    /**
     * Invoke the echoprint native library and generate the fingerprint code.<br>
     * Since echoprint requires the audio data to be an array of floats in the<br>
     * range [-1, 1] this method will normalize the data array transforming the<br>
     * 16 bit signed shorts into floats.
     *
     * @param data PCM encoded data as shorts
     * @param numSamples number of PCM samples at 11025 KHz
     * @return The generated fingerprint as a compressed - base64 string.
     */
    public String generate(short data[], int numSamples)
    {
        float normalizeAudioData[] = new float[numSamples];
        for (int i = 0; i < numSamples - 1; i++)
            normalizeAudioData[i] = data[i] / normalizingValue;
 
        return this.codegen(normalizeAudioData, numSamples);
    }
}
