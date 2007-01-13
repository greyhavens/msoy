package 
{
    
import flash.text.TextField;

public class ScoreDisplay {
    
    public function ScoreDisplay (juggler:PlayField)
    {
        const labels:Array = 
            new Array(scoreLabel, ballsInPlayLabel, sequentialCatchesLabel, mostJuggledLabel,
                    catchValueLabel);
            
        var pos:int = INDENT;
            
        for each (var field:TextField in labels)
        {
            field.selectable = false;
            field.x = INDENT;
            field.y = pos;
            field.width = 300;
            pos += LINE_HEIGHT;
            juggler.addChild(field);
        }
    }

    public function update(card:ScoreCard) :void
    {
        scoreLabel.text = "score: " + card.score;
        ballsInPlayLabel.text = "balls in play: " + card.ballsInPlay;
        sequentialCatchesLabel.text = "sequential catches: " + card.sequentialCatches;
        mostJuggledLabel.text = "most ball juggled: " + card.mostJuggled;
        catchValueLabel.text = "catches are worth: " + card.catchValue;
    }

    private const INDENT:int = 10;

    private const LINE_HEIGHT:int = 15; 

    private const scoreLabel:TextField = new TextField();

    private const ballsInPlayLabel:TextField = new TextField();
    
    private const sequentialCatchesLabel:TextField = new TextField();
    
    private const mostJuggledLabel:TextField = new TextField();

    private const catchValueLabel:TextField = new TextField();
}
}