package {
public class ScoreCard {

    public function ballCaught() :void
    {
        sequentialCatches++;
        if (sequentialCatches > peakSequentialCatches) 
        {
            peakSequentialCatches = sequentialCatches;
        }
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

    private var score:int = 0;
    
    private var ballsInPlay:int = 0;
    
    private var peakBallsInPlay:int = 0;
    
    private var sequentialCatches:int = 0;
    
    private var peakSequentialCatches:int = 0;
}
}