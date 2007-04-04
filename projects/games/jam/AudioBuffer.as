package
{
    import flash.utils.ByteArray;
    import flash.utils.Endian;
    
    /**
     * A class that contains a byte array of audio samples.
     * 
     * Based on NumberToIntegerAudioBuffer by /spender/ of /flashbrighton.org/
     */
    public class AudioBuffer
    {
        private var _soundFormat :SoundFormat;
        private var _audioBuffer :ByteArray;

        /**
         * Creates a new buffer
         */
        public function AudioBuffer (soundFormat :SoundFormat)
        {
            _soundFormat = soundFormat;
            _audioBuffer = new ByteArray();
            _audioBuffer.endian = Endian.LITTLE_ENDIAN;
            _audioBuffer.length = soundFormat.bufferSize;
        }

        /**
         * Fills the audio buffer with values pulled from the Number array.
         */
        public function fill (source :Array) :void
        {
            var fillElt :Function = fill8bit;
            switch(_soundFormat.sampleSize) {
            case SoundFormat.SAMPLESIZE_16BIT: fillElt = fill16bit;
            }

            _audioBuffer.position = 0;
            for (var i :int = 0; i < source.length; ++i) {
                fillElt(Math.max(Math.min(source[i], 1.0), -1.0));
            }
        }

        /*
         * Returns the audio buffer as an array of bytes. Caller should treat
         * the resulting raw buffer as read-only.
         */
        public function get buffer () :ByteArray
        {
            return _audioBuffer;
        }

        private function fill8bit (value :Number) :void
        {
            _audioBuffer.writeByte((value + 1) * 127.5);
        }
        
        private function fill16bit (value :Number) :void
        {
            _audioBuffer.writeShort(value * 32767);
        }

    }
}
