package
{
    import flash.events.EventDispatcher;
    import flash.events.Event;
    import flash.media.Sound;
    import flash.utils.Timer;
    import flash.events.TimerEvent;
    import flash.media.SoundChannel;

    /**
     * Sound generation engine, takes buffers filled in by generators and filters,
     * and fills in the sound buffer.
     *
     * Based on PlaybackEngine by /spender/ of /flashbrighton.org/
     */
    public class SoundEngine extends EventDispatcher
    {
        /** This event is sent when the engine is ready to be started. */
        public static const READY :String = "sound engine ready";
        
        /**
         * Instantiates a PlaybackEngine.
         */
        public function SoundEngine (source :AudioNode)
        {
            _source = source;
            _format = source.getFormat();
			
            _ready = false;
			
            _factory = new SoundFactory(_format);
            _factory.addEventListener(
                SoundCreatedEvent.SOUND_OBJECT_CREATED, handleSoundCreated, false, 0, true);
            
        }
        
        /**
         * Handles the creation of a sound object.
         */
        private function handleSoundCreated (soundEvent :SoundCreatedEvent) :void
        {
            if (_silentSound == null) {
                // the silent sound gets created first - let's remember it
                _silentSound = soundEvent.sound;
                updateAudioBuffer();
            } else {
                // audio object got created - let's get ready to process it
                _nextSound = soundEvent.sound;
                if (!_ready) {
                    _ready = true;
                    this.dispatchEvent(new Event(READY));
                }
            }
        }
        
        /**
         * Starts playback. This can be called any time after the PlaybackEngine
         * has emitted a READY event.
         */
        public function start():void
        {
            if (_ready) {
                soundComplete(null);
            } else {
                throw new Error ("start() called before the engine is ready.");
            }
        }
        
        /**
         * Handler for the SOUND_COMPLETE event emitted by the channel playing
         * the silent sound. This will be called upon multiples of 2048 samples.
         * All timing is hinged off this event.
         */
        private function soundComplete (event :Event) :void
        {
            if (_nextSound == null) { //the data isn't ready yet... this shouldn't happen
                throw new Error ("soundComplete buffer underrun!");
            }

            // play the audio buffer, and the fake silent sound
            _nextSound.play();
            _nextSound = null;
            var channel :SoundChannel = _silentSound.play();
            channel.addEventListener(Event.SOUND_COMPLETE, soundComplete, false, 0, true);

            // in the meantime, regenerate the audio buffer
            updateAudioBuffer();

        }

        /** Simple accessor to update the audio buffer. */
        private function updateAudioBuffer () :void
        {
            _factory.fillAudioBuffer(_source);
        }

        private var _factory :SoundFactory;
        private var _ready :Boolean;
        private var _silentSound :Sound;
        private var _nextSound :Sound;
        
        private var _source :AudioNode;
        private var _format :SoundFormat;


    }
}
