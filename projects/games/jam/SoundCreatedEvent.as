package 
{
    import flash.events.Event;
    import flash.media.Sound;
    
    /**
     * This event is fired by the SoundFactory, to notify others that a new sound
     * object has been created (i.e. the silent swf or the actual swf).
     * 
     * Based on SoundEvent by /spender/ of /flashbrighton.org/
     */
    public class SoundCreatedEvent extends Event
    {
        public static const SOUND_OBJECT_CREATED :String = "soundObjectCreated";
        
        /**
         * This event should only be instantiated by SoundFactory
         */
        public function SoundCreatedEvent (sound :Sound, type :String = SOUND_OBJECT_CREATED)
        {
            super(type);
            this.sound = sound;
        }
        
        /**
         * Sound instance that was created
         */
        public var sound :Sound;

    }
}
