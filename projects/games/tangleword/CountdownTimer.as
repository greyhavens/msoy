package
{

import flash.display.Sprite;    

import flash.text.TextField;
import flash.text.TextFormat;

import flash.utils.Timer;
import flash.events.TimerEvent;


/**
   Countdown timer displays a countdown clock, and stops at zero.
*/
public class CountdownTimer extends TextField
{
    // Constructor, sets everything up
    public function CountdownTimer ()
    {
        // Set up display
        var format : TextFormat = Resources.makeFormatForCountdown ();
        this.selectable = false;
        this.defaultTextFormat = format;
        this.borderColor = Resources.defaultBorderColor;
        this.border = true;

        // Subscribe to ticks every second
        _timer = new Timer (1000, 0);
        _timer.addEventListener (TimerEvent.TIMER, tickHandler);
    }

    /** Starts the timer with the specified countdown period in seconds */
    public function start (seconds : Number) : void
    {
        _countdownPeriod = seconds * 1000; // convert to ms
        _startTime = (new Date()).time;
        _timer.start ();

        // pretend we're processing the first tick when this starts
        tickHandler (null);
    }

    /** Stops the timer, but keeps displaying the last time value */
    public function stop () : void
    {
        _timer.stop ();
    }

    /** Clears timer display */
    public function clear () : void
    {
        this.text = "";
    }


    // PRIVATE METHODS

    /** Handles a tick, updating the UI */
    private function tickHandler (event : TimerEvent) : void
    {
        // Compute the number of minutes and seconds remaining
        var deltams : Number = (new Date()).time - _startTime;
        var remainingms : Number = _countdownPeriod - deltams;
        var remainingsecs : Number = Math.round (remainingms / 1000);

        if (remainingsecs <= 0)
        {
            stop ();           // stop the timer
            remainingsecs = 0; // zero out the display
        }
        
        var minutes : Number = Math.floor (remainingsecs / 60);
        var seconds : Number = remainingsecs % 60;

        // Update timer text. I can't believe ActionScript doesn't have
        // a string formatting function. >:(
        var formatTime : Function = function (n : Number) : String
        {
            if (n >= 0 && n < 10) return "0" + Math.round (n);
            else return new String(Math.round (n));
        }
        var t : String = formatTime.call (null, minutes) + ":" + formatTime.call (null, seconds);
        this.text = t;

    }

    
        


    // PRIVATE VARIABLES

    /** Timer storage */
    private var _timer : Timer;

    /** How much time is being counted down, in milliseconds */
    private var _countdownPeriod : Number;
    
    /** The last time the timer was started, in milliseconds since the
        start of Unix epoch. */
    private var _startTime : Number;


}


}
