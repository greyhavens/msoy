package {

import flash.display.Sprite;
import flash.events.Event;
import flash.utils.Timer;
import flash.events.TimerEvent;

public class Jam extends Sprite
{
    private var engine: SoundEngine;
    
    public function Jam ()
    {
        var fmt :SoundFormat = new SoundFormat();

        var one :NoteGenerator = new NoteGenerator(fmt, 0.25, [60, 69, 67, 69]);
        var two :NoteGenerator = new NoteGenerator(fmt, 0.5, [64, 65]);

        one.setEnvelope(0.0, 0.0, 0.4, 0.2, 0.9, 0.4);
        two.setEnvelope(0.0, 0.2, 0.0, 0.2, 1.0, 0.4);

        var mix :SummationFilter = new SummationFilter(fmt, one, two);
        var sm :SmoothingFilter = new SmoothingFilter(fmt, mix);
        engine = new SoundEngine(sm);
        engine.addEventListener (SoundEngine.READY, engineReady, false, 0, true);
    }
    
    private function engineReady (event:Event) :void
    {
        engine.start(); 
    }
    
}
}
