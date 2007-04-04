package
{

public class SummationFilter extends AudioNode
{
    private var a :AudioNode, b :AudioNode;
    private var abuf :Array, bbuf :Array;
    
    public function SummationFilter (format :SoundFormat, a :AudioNode, b :AudioNode)
    {
        super(format);
        this.abuf = new Array(format.bufferSize);
        this.bbuf = new Array(format.bufferSize);
        this.a = a;
        this.b = b;
    }
    
    override public function generateSamples (startTime :Number, buffer :Array) :void
    {
        a.generateSamples (startTime, abuf);
        b.generateSamples (startTime, bbuf);

        for (var i :int = 0; i < _soundFormat.bufferSize; ++i)
        {
            buffer[i] = abuf[i] + bbuf[i];
        }
    }
}
}
