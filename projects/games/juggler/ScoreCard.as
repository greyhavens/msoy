package {
public class ScoreCard {

    public function ballCaught() :void
    {
        sequentialCatches++;
        if (sequentialCatches > peakSequentialCatches) 
        {
            peakSequentialCatches = sequentialCatches;
        }
     
        if (sequentialCatches > ballsInPlay) 
        {
            currentJuggled = ballsInPlay;
            
            if(currentJuggled > mostJuggled) 
            {
                mostJuggled = currentJuggled;
            }            
        } 
                                
        score += catchValue;

        updateCatchValue();
        updateDisplay();
    }

    public function ballAdded() :void
    {
        sequentialCatches = 0;
        ballsInPlay += 1;
        if (ballsInPlay > peakBallsInPlay)
        {
            peakBallsInPlay = ballsInPlay;
        }
        
        updateCatchValue();        
        updateDisplay();
    }

    public function ballDropped() :void
    {        
        sequentialCatches = 0;
        ballsInPlay -= 1;
        
        updateCatchValue();
        updateDisplay();
    }
    
    private function updateCatchValue() :void
    {
        catchValue = (currentJuggled * sequentialCatches);
        const cj2:int = currentJuggled * currentJuggled * 3;
        catchValue = catchValue > cj2 ? cj2 : catchValue;
    }
    
    private function updateDisplay() :void
    {
        if (_display != null)
        {
            _display.update(this);
        }
    }
    
    public function set display (display:ScoreDisplay) :void
    {
        _display = display;
    }
    
    private var _display:ScoreDisplay;

    public var score:int = 0;
    
    public var ballsInPlay:int = 0;
    
    public var peakBallsInPlay:int = 0;
    
    public var sequentialCatches:int = 0;
    
    public var peakSequentialCatches:int = 0;
    
    public var currentJuggled:int = 0;
    
    public var mostJuggled:int = 0;
    
    public var catchValue:int = 0;
}
}