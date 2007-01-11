package {
import flash.display.Sprite;

public class Track extends Sprite 
{
    public function Track ()
    {
        addChild(new TRACK_2());
        var track3 :Sprite = new TRACK_3();
        track3.y = -1024;
        addChild(track3);
    }

    [Embed(source='rsrc/track.swf#track1')]
    protected static const TRACK_1 :Class;

    [Embed(source='rsrc/track.swf#track2')]
    protected static const TRACK_2 :Class;

    [Embed(source='rsrc/track.swf#track3')]
    protected static const TRACK_3 :Class;
}
}
