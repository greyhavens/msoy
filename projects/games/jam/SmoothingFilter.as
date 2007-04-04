package
{

public class SmoothingFilter extends AudioNode
{
    private var source :AudioNode;
    private var intermediateBuffer :Array;
    private var v1 :Number, v2 :Number;
    
    public function SmoothingFilter (format :SoundFormat, source :AudioNode)
    {
        super(format);
        this.intermediateBuffer = new Array(format.bufferSize);
        this.source = source;
    }
    
    override public function generateSamples (startTime :Number, buffer :Array) :void
    {
        source.generateSamples (startTime, intermediateBuffer);

        for (var i :int = 0; i < _soundFormat.bufferSize; ++i)
        {
            buffer[i] = (intermediateBuffer[i] + v1 + v2) / 3;
            v2 = v1;
            v1 = intermediateBuffer[i];
        }
    }
}
}
