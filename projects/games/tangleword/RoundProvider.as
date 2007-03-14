package
{

import flash.utils.Timer;
import flash.events.Event;
import flash.events.TimerEvent;
import flash.events.EventDispatcher;

import com.threerings.ezgame.StateChangedEvent;
import com.threerings.ezgame.StateChangedListener;

import com.whirled.WhirledGameControl;


/**
 * Round provider extends EZGameControl's round functionality with automatic host management,
 * tracking time till end of round (or end of pause between rounds), and other convenience
 * functions.
 *
 * The provider fires two events, RoundProviderEvent.STARTED and .ENDED, which happen
 * /after/ the provider's internal data has been updated. Clients can subscribe to these events
 * with a guarantee that provider's data will be ready for use after the event was sent out.
 *
 * Usage example:
 *
 * <pre>
 * _rounds = new RoundProvider(game, 60, 20); // 60 second rounds with 20 second breaks
 * _rounds.addEventListener(RoundProviderEvent.STARTED, handleStart);
 * ...
 * protected function handleStart (event :RoundProviderEvent) :void {
 *   _countdownClock.updateDisplay(_rounds.endTime); 
 * }
 * 
 */
public class RoundProvider extends EventDispatcher
    implements StateChangedListener
{
    /** Constructor. Expects and instance of WhirledGameControl, and round and
     *  pause lengths /in seconds/. Tries to start the first round automatically
     *  with the given round length. */
    public function RoundProvider (
        gameCtrl : WhirledGameControl, roundLength :int, pauseLength :int)
    {
        _roundLengthMs = roundLength * 1000;
        _pauseLengthMs = pauseLength * 1000;
        _gameCtrl = gameCtrl;
        _gameCtrl.registerListener(this);

        // this timer is only used for timing rounds (not pauses between rounds)
        _timer = new Timer(0);
        _timer.addEventListener(TimerEvent.TIMER, handleTimer);
    }

    /** Starts the first round. This function should be called after all listeners
     *  had a chance to subscribe to the appropriate events. */
    public function initialize () :void
    {
        // how long should we wait? if i'm the first player in the game (i.e. the host),
        // just start a full first round. otherwise, set the timer to trigger when the
        // existing first round is *supposed* to end.
        if (_gameCtrl.amInControl()) {
            setupRoundEndTimer(_roundLengthMs);
        } else {
            setupRoundEndTimer(endTime - now);
        }
    }

    /** Removes any listeners. */
    public function handleUnload (event : Event) : void
    {
        _timer.removeEventListener(TimerEvent.TIMER, handleTimer);
        _gameCtrl.unregisterListener(this);
    }

    /** Returns true if a round is active. */
    public function get inRound () :Boolean
    {
        return (_gameCtrl.getRound() > 0);
    }

    /** Returns true if a pause is active. */
    public function get inPause () :Boolean
    {
        return !inRound;
    }
    
    /** Handles round changes. Beginning of a new round is the interesting case,
     *  since it will be used to start the timer that will eventually end the round. */
    public function stateChanged (event :StateChangedEvent) :void
    {
        switch (event.type) {
            
        case StateChangedEvent.ROUND_STARTED:
            setupRoundEndTimer(_roundLengthMs);
            break;

        case StateChangedEvent.ROUND_ENDED:
            cleanupRoundEndTimer();
            break;
        } 
    }

    /** Returns time when the current round (or pause) is scheduled to end, in Date.time format
     *  (i.e., milliseconds since the beginning of Unix epoch. */
    public function get endTime () :int
    {
        return int(_gameCtrl.get("_round_provider_endtime"));
    }

    // IMPLEMENTATION DETAILS

    /** Sets the time when the current round (or pause) is scheduled to end,
     *  in Date.time format (i.e. milliseconds since the beginning of Unix epoch).
     *  Setting this variable does not actually change the timers that schedule
     *  round start or end; this variable is used for visual feedback only.
     */
    protected function setEndTime (time :int) :void
    {
        _gameCtrl.set("_round_provider_endtime", time);
    }

    /** Returns current time, in Date.time format. */
    protected function get now () :int
    {
        return (new Date()).time;
    }
    
    /** Starts the timer that will eventually cause the round to end. */
    protected function setupRoundEndTimer (roundLengthMs :int) :void
    {
        // all clients start the round timer
        _timer.reset();
        _timer.delay = roundLengthMs;
        _timer.start();
        // if i'm the host, update the timeout variable
        if (_gameCtrl.amInControl()) {
            setEndTime(now + roundLengthMs);
        }
        // inform listeners
        this.dispatchEvent (
            new RoundProviderEvent (RoundProviderEvent.STARTED, _gameCtrl, roundLengthMs));
    }

    /** After the round had ended, stop the timer. This will have happened after
     *  the call to endRound(). */
    protected function cleanupRoundEndTimer () :void
    {
        // all clients stop the round timer
        _timer.stop();
        // inform listeners
        this.dispatchEvent (
            new RoundProviderEvent (RoundProviderEvent.ENDED, _gameCtrl, _pauseLengthMs));
    }

    /** Handles round end timer events - when the timer triggers, the host client will
     *  tell the game to treat the round as ended, and schedule a new round to begin
     *  after the specified delay. */
    protected function handleTimer (event :TimerEvent) :void
    {
        // if i'm the host, tell the EZ game to end the round, and start a pause of a specific
        // length. also, update the timeout variable.
        if (_gameCtrl.amInControl()) {
            _gameCtrl.endRound(int(_pauseLengthMs / 1000));
            setEndTime(now + _pauseLengthMs);
        }
    }
    
    /** Local game control storage. */
    protected var _gameCtrl :WhirledGameControl;

    /** Length of each round, in milliseconds. */
    protected var _roundLengthMs :int;

    /** Length of each pause between rounds, in milliseconds. */
    protected var _pauseLengthMs :int;

    /** Timeout timer. If this variable is null, it means no timer
     *  was initialized; otherwise it's set to the instance that will
     *  call our callback function. */
    protected var _timer :Timer;

}
}
