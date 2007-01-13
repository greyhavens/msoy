package {

public class Space {
 
    public function Space(l :Number, t :Number, r:Number, b:Number, rate: Number) {
        left = l;
        top = t;
        right = r;
        bottom = b;     
        setFrameRate(rate);                
    }
    
    public function width() :Number
    {
        return right - left;
    }
    
    public function height() :Number
    {
        return bottom - top;
    }
    
    private function setFrameRate (rate :Number) :void
    {
        _frameRate = rate;
        frameDuration = (1/rate) * 1000;
        gravityPerFrame = gravity / _frameRate;
        frictionPerFrame = friction / _frameRate;
    }
    
    public function get frameRate () :Number
    {
        return _frameRate;
    }
    
    public var left :Number;
    
    public var right :Number;
    
    public var top :Number;
    
    public var bottom: Number;
    
    public const gravity :Number = 1500; // pixels/sec/sec
    
    public var gravityPerFrame :Number;
    
    private const friction :Number = 0.03; // units/sec/sec  
    
    public var frictionPerFrame :Number;
    
    private var _frameRate :Number; // fps
    
    public var frameDuration :Number; // ms per frame
}
}