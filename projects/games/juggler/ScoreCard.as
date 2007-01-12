package {
public class ScoreCard {

    public function ballCaught() :void
    {
        sequentialCatches++;
        if (sequentialCatches > peakSequentialCatches) 
        {
            peakSequentialCatches = sequentialCatches;
        }
     
        if (sequentialCatches > ballsInPlay && ballsInPlay > mostJuggled)
        {
            mostJuggled = ballsInPlay;
        }
        
        score += (ballsInPlay * sequentialCatches);
    }

    public function ballAdded() :void
    {
        ballsInPlay += 1;
        if (ballsInPlay > peakBallsInPlay)
        {
            peakBallsInPlay = ballsInPlay;
        }
    }

    public function ballDropped() :void
    {        
        sequentialCatches = 0;
        ballsInPlay -= 1;
    }

    public var score:int = 0;
    
    public var ballsInPlay:int = 0;
    
    public var peakBallsInPlay:int = 0;
    
    public var sequentialCatches:int = 0;
    
    public var peakSequentialCatches:int = 0;
    
    public var mostJuggled:int = 0;
}
}