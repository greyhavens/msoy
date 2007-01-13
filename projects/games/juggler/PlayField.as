package
{

import flash.display.DisplayObject;    

	
public interface PlayField extends Positioned
{
	function registerAsActor(actor:Actor) :void;
	
	function addChild(child:DisplayObject) :DisplayObject;

	function removeChild(child:DisplayObject) :DisplayObject;

    function deregisterAsActor(target:Actor) :void;

    function registerForCollisions(body:CanCollide) :void;

    function deregisterForCollisions(body:CanCollide) :void;

	function get scoreCard() :ScoreCard;
	
	function get ballBox() :BallBox;
}
}