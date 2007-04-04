package
{
    /**
     * Basic node in a sound graph. Each node should be able to fill a data array
     * with sound samples. This base class fills the array with zeros.
     */
    public class AudioNode
    {
        public function AudioNode (format :SoundFormat)
        {
            if (format.channels != SoundFormat.CHANNELTYPE_MONO) {
                throw new Error("This set of audio nodes only supports mono channels.");
            }
            _soundFormat = format;
        }

        /**
         * Returns this generator's sound format.
         */
        public function getFormat () :SoundFormat
        {
            return _soundFormat;
        }

        /**
         * Called by a sample consumer, such as a filter or player. The consumer will
         * pass two values:
         *  - an array of Number fields, and
         *  - starting time for this sample, in seconds since the start of epoch.
         *
         * The array represents a sound buffer, filled with interleaved samples for all channels.
         * The generator is expected to fill in this array with sample values in [-1, 1],
         * which correspond to the sound being generated.
         *
         * This method should be overridden by specialized generator subclasses.
         */
        public function generateSamples (startTime :Number, buffer :Array) :void
        {
            for (var i :int = 0; i < buffer.length; i++) {
                buffer[i] = 0;
            }
        }
            
        protected var _soundFormat :SoundFormat;

    }
}
