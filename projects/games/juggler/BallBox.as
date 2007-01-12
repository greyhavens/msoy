package {

import flash.display.DisplayObject;


/* An infinite sized box of different colored balls */
public class BallBox {
    
    public function BallBox(juggler:Juggler, space:Space) :void
    {
        _juggler = juggler;
        _space = space;
        
        balls.push(blueBall);
        balls.push(redBall);
        balls.push(greenBall);
    }
    
    public function provideBall() :Ball
    {
        const art:Object = new balls[_top]();
        
        _top ++;
        if (_top >= balls.length)
        {
            _top = 0;
        }
        
        (art as DisplayObject).x = -18;
        (art as DisplayObject).y = -18;
        
        return new Ball(_juggler, _space, art);
    }
    
    private var _juggler:Juggler;
    
    private var _space:Space;
    
    private var _top:int = 0;
    
    private const balls:Array = new Array();
    
    [Bindable]
    [Embed(source="ball_red.swf")]
    private var redBall:Class;
    
    [Bindable]
    [Embed(source="ball_green.swf")]
    private var blueBall:Class;
    
    [Bindable]
    [Embed(source="ball_blue.swf")]
    private var greenBall:Class;    
}
}