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
        var one :SineWaveGenerator = new SineWaveGenerator(fmt, 0.25);
        var two :SineWaveGenerator = new SineWaveGenerator(fmt, 1);
        var mix :SummationFilter = new SummationFilter(fmt, one, two);
        engine = new SoundEngine(mix);
        engine.addEventListener (SoundEngine.READY, engineReady, false, 0, true);
    }
    
    private function engineReady (event:Event) :void
    {
        engine.start(); 
    }
    
}
}
