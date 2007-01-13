package
{
    
public class JugglingController extends KeyboardController
{
    public function JugglingController(body:Body, juggler:PlayField) :void 
    {
           action[KEY_Q] = body.leftHandLeft;
           action[KEY_W] = body.leftHandRight;
           action[KEY_O] = body.rightHandLeft;
           action[KEY_P] = body.rightHandRight;
           action[KEY_SPACE] = body.addBall;         
    }
    
    public function leftDown() :Boolean
    {
        return down[KEY_Q] || down[KEY_W];
    }
    
    public function rightDown() :Boolean
    {
        return down[KEY_O] || down[KEY_P];
    }    
}
}
