package 
{
    import flash.utils.ByteArray;
    import flash.utils.Endian;
    import flash.events.EventDispatcher;
    import flash.display.Loader;
    import flash.media.Sound;
    import flash.events.Event;
    
    /**
     * This class creates SWF objects for the main audio buffer, and a 'silent SWF'
     * that's used as pacemaker, to trigger events that will refill the main audio buffer.
     *
     * Based on SoundFactory by /spender/ of /flashbrighton.org/
     */
    public class SoundFactory extends EventDispatcher
    {
        /**
         * Creates a SoundFactory
         */
        public function SoundFactory (soundFormat :SoundFormat)
        {
            _soundFormat=soundFormat;
            _intermediateBuffer = new Array(soundFormat.bufferSize);
            _audioBuffer = new AudioBuffer(_soundFormat);

            _loader = new Loader();
            _loader.contentLoaderInfo.addEventListener(Event.COMPLETE, swfCreated, false, 0, true);

            // from spender:
            // this took ages to find... remember, 8bit audio is unsigned, so zero point is 128.
            var defVal :int = (_soundFormat.sampleSize == SoundFormat.CHANNELTYPE_MONO) ? 128 : 0;  
            var silentSoundSize:int = _soundFormat.bufferSize - 2038; 
            _silenceSwf = makeSwf(silentSoundSize, defVal);
            _audioSwf = makeSwf(soundFormat.bufferSize, defVal);

            _loader.loadBytes(_silenceSwf);
        }
		
        /**
         * Given a reference to a generator, uses it to populate the audio buffer,
         * and plays the resulting sound.
         */
        public function fillAudioBuffer (source :AudioNode) :void
        {
            // pull out the wave form
            source.generateSamples((new Date()).time / 1000.0, _intermediateBuffer);
            _audioBuffer.fill(_intermediateBuffer);

            // fill in the swf and reload
            _audioSwf.position = _swfDataPosition;
            _audioSwf.writeBytes(_audioBuffer.buffer);
            _loader.loadBytes(_audioSwf); 			
        }

        /**
         * Handler for the Loader COMPLETE event
         */
        private function swfCreated (event :Event) :void
        {
            var soundClass :Class =
                Class(_loader.contentLoaderInfo.applicationDomain.getDefinition("SoundClass"));
            var sound :Sound = new soundClass();
            var event :Event = new SoundCreatedEvent(sound);
            dispatchEvent(event);
        }
        
        /**
         * Creates a byte array that corresponds to a SWF file, filled with the default value.
         * Returns the array and, as a side effect, sets a static variable that describes
         * the size of the SWF data header, just up to the actual audio buffer.
         */
        private function makeSwf (numSamples :Number, defaultValue :int) :ByteArray
        {
            var bytesPerSample :int = _soundFormat.channels * _soundFormat.sampleSize;

            var swfData :ByteArray = new ByteArray();
            swfData.endian = Endian.LITTLE_ENDIAN;
			
            writeBytes(SoundClassSwfByteCode.soundClassSwfBytes1, swfData);
            swfData.writeUnsignedInt(299 + numSamples * bytesPerSample);
            
            writeBytes(SoundClassSwfByteCode.soundClassSwfBytes2, swfData);
            swfData.writeUnsignedInt(numSamples * bytesPerSample + 7);
            swfData.writeByte(1);
            swfData.writeByte(0);
            var formatByte :int = 0x30;
            formatByte += (_soundFormat.sampleRateIndex << 2) +
                          ((_soundFormat.sampleSize - 1) << 1) +
                          (_soundFormat.channels - 1);
            swfData.writeByte(formatByte);		
            swfData.writeUnsignedInt(numSamples);
			
            // Store the position of the start of the audio data;
            // the header is the same for all swf files
            _swfDataPosition = swfData.position;
			
            // Fill the buffer with zeros
            for (var i :Number = 0; i < numSamples * bytesPerSample; i++) {
                swfData.writeByte(defaultValue);
            }
			
            writeBytes(SoundClassSwfByteCode.soundClassSwfBytes3, swfData);
			
            return swfData;			
        }

        /**
         * Copies bytes stored in a standard Array into a ByteArray.
         * No attempt is made to validate the source data.
         */
        private function writeBytes (src:Array, dest:ByteArray) :void
        {
            for (var i :int = 0; i < src.length; ++i) {
                dest.writeByte(src[i]);
            }
        }

        private var _intermediateBuffer :Array;
        private var _audioBuffer :AudioBuffer;
        private var _soundFormat :SoundFormat;
        
        private var _loader :Loader;
        private var _swfDataPosition :Number;
		
        private var _audioSwf :ByteArray;
        private var _silenceSwf :ByteArray;

    }
}
