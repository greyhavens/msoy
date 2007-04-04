package
{

public class NoteGenerator extends AudioNode
{
    private var _f :Number;
    private var _globalStartTime :Number;

    private var notes :Array = [60, 64, 69];
    private var noteLength :Number = 0.5; // seconds
    private var noteIndex :int = -1;
    private var nextNoteStart :Number = 0; // in seconds since _globalStartTime
    private var tNote :Number = 0; // in seconds since beginning of this note
    
    public function NoteGenerator (format :SoundFormat, delay :Number, notes :Array)
    {
        super(format);
        _f = noteToFrequency(60); // initial note: middle C
        
        noteLength = delay;
        if (notes != null) {
            this.notes = notes;
        }
        
        _globalStartTime = (new Date()).time / 1000.0;
    }

    override public function generateSamples (startTime :Number, buffer :Array) :void
    {
        var t :Number = startTime - _globalStartTime;
        var dt :Number = 1 / _soundFormat.sampleRate;

        var x :Number = 2 * Math.PI * t;
        var dx :Number = 2 * Math.PI * dt;

        var gain :Number = 0.5;

        for (var i :int = 0; i < _soundFormat.bufferSize; ++i)
        {
            // check if we need to switch to a new note
            if (t >= nextNoteStart) {
                noteIndex = (noteIndex + 1) % notes.length;
                _f = noteToFrequency(notes[noteIndex]);
                nextNoteStart += noteLength;
                tNote = 0;
            }

            // attenuate gain based on note length
            gain = 0.5 * Math.max(0, 1 - tNote);

            buffer[i] = Math.sin(x * _f) * gain;
            x += dx;
            t += dt;
            tNote += dt;
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
