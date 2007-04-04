package
{

public class SineWaveGenerator extends AudioNode
{
    private var dt :Number;
    private var f :Number;
    private var note :Number;
    private var stepsize :Number;
    private var globalStartTime :Number;
 
    public function SineWaveGenerator (format :SoundFormat, delay :Number)
    {
        super(format);
        dt = 1 / _soundFormat.sampleRate;
        setNote(0); // initial note: middle C
        
        this.stepsize = delay;
        
        globalStartTime = (new Date()).time / 1000.0;
    }

    // Sets the current-playing note, specified as a note relative to middle C
    // (so: concert A is 9, one octave below middle C is -12, etc.)
    public function setNote (noteRelativeToMiddleC :Number) :void
    {
        f = noteToFrequency (noteRelativeToMiddleC + 60);
    }
    
    override public function generateSamples (startTime :Number, buffer :Array) :void
    {
        var len :Number = startTime - globalStartTime;
        var x :Number = 2 * Math.PI * startTime;
        var dx :Number = 2 * Math.PI * dt;
        
        var now :Number = len;

        for (var i :int = 0; i < _soundFormat.bufferSize; ++i)
        {
            var newnote :Number = Math.floor(now / stepsize);
            if (newnote != note) {
                note = newnote;
                setNote(note);
            }

            buffer[i] = Math.sin(x * f) * 0.5;
            x += dx;
            now += dt;
        }
    }

    // pitch conversion
    // concert A is #69 on midi scale, or
    // A5 = p = 69 + 12 * log2 (f / 440), therefore:
    // f = 440 * 2 ^ ((p - 69) / 12)
    protected function noteToFrequency (midiNumber :Number) :Number
    {
        return 440 * Math.pow (2, (midiNumber - 69) / 12);
    }
    
}
}
