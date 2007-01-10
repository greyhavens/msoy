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
    
    private function setFrameRate(rate :Number) :void
    {
        frameRate = rate;
        frameDuration = (1/rate) * 1000;
        gravityPerFrame = gravity / frameRate;
        frictionPerFrame = friction / frameRate;
    }
    
    public var left :Number;
    
    public var right :Number;
    
    public var top :Number;
    
    public var bottom: Number;
    
    public var gravity :Number = 1000; // pixels/sec/sec
    
    public var gravityPerFrame :Number;
    
    private var friction :Number = 0.03; // units/sec/sec  
    
    public var frictionPerFrame :Number;
    
    public var frameRate :Number; // fps
    
    public var frameDuration :Number; // ms per frame
}
}