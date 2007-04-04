package
{
    /**
     * Describes what kind of PCM file we're working with, and what kind of a temp buffer.
     *
     * Based on PCMSoundFormat by /spender/ of /flashbrighton.org/
     */
    public class SoundFormat
    {
        /** Sample rate constant: 44100 Hz */
        public static const SAMPLERATE_44100 :int = 3;
        
        /** Sample rate constant: 22050 Hz */
        public static const SAMPLERATE_22050 :int = 2;
        
        /** Sample rate constant: 11025 Hz */
        public static const SAMPLERATE_11025 :int = 1;
        
        /** Sample rate constant: 5512.5 Hz */
        public static const SAMPLERATE_5512_5 :int = 0;
        
        
        /** Sample size constant: 8 bit sample. */
        public static const SAMPLESIZE_8BIT :int = 1;

        /** Sample size constant: 16 bit sample. */
        public static const SAMPLESIZE_16BIT :int = 2;


        /** Channel type constant: mono channel. */
        public static const CHANNELTYPE_MONO :int = 1;

        /** Channel type constant: stereo channel. */
        public static const CHANNELTYPE_STEREO :int = 2;
        
        
        /** Playback buffer size. Must be a multiple of 2048. */
        public var bufferSize :int;
        
        /** The audio sample rate in Hz: 44100, 22050, 11025 or 5512.5 */
        public var sampleRate :Number;
        
        /** The audio sample rate index according to documentation. */
        public var sampleRateIndex :Number;

        /** Audio depth in bytes, either 1 or 2. */
        public var sampleSize :int;

	/** The number of channels, 1 for mono, 2 for stereo. */
        public var channels :int;

	/** Audio bit depth, either 8 or 16. */
        public var sampleSizeBits :int;
        
        /** The number of individual samples in an audio buffer, one per each channel. */
        public var sampleCountPerBuffer :int;
        
	/**
         * Construct a SoundFormat object with default values.
         */
        public function SoundFormat()
        {
            initialize (SAMPLERATE_44100, SAMPLESIZE_16BIT, CHANNELTYPE_MONO, 20);
        }

        /**
         * Initialize the sound object. 
         * @param sampleRateIndex The audio sample rate, one of the SAMPLERATE_* constants
         * @param sampleSize Audio bit depth, one of the SAMPLESIZE_* constants
         * @param channelType Channel information, one of the CHANNELTYPE_* constants        
         * @param bufferSize The size of the buffer, as multiples of 2048
         *                   (e.g. the value of 20 means the buffer will be 20 * 2048 bytes large)
         */
        protected function initialize (
            sampleRateIndex :int, sampleSize :int, channelType :int, bufferMultiple :int) :void
        {
            this.bufferSize = 2048 * bufferMultiple;
            this.channels = channelType;
            this.sampleSize = sampleSize;
            this.sampleSizeBits = sampleSize * 8;
            this.sampleRateIndex = sampleRateIndex;

            this.sampleCountPerBuffer = bufferSize * channels;
            
            switch (sampleRateIndex) {
            case SAMPLERATE_44100: this.sampleRate = 44100; break;
            case SAMPLERATE_22050: this.sampleRate = 22050; break;
            case SAMPLERATE_11025: this.sampleRate = 11025; break;
            case SAMPLERATE_5512_5: this.sampleRate = 5512.5; break;
            }
	}
    }
}
