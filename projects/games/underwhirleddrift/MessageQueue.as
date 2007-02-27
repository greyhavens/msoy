package 
{

import com.threerings.ezgame.EZGameControl;
import flash.utils.getTimer;

public class MessageQueue 
{
    public function MessageQueue (gameCtrl :EZGameControl)
    {
        _gameCtrl = gameCtrl;
    }

    public function sendMessage (messageName :String, value :Object) :void
    {
        if (_sendTimes.length < 100) {
            _gameCtrl.sendMessage(messageName, value);
            _sendTimes.push(getTimer());
        } else if (getTimer() - _sendTimes[0] > SEND_THROTTLE_TIME) {
            _gameCtrl.sendMessage(messageName, value);
            _sendTimes.shift();
            _sendTimes.push(getTimer());
        } else {
            // do nothing here for now - since position information is sent with all messages,
            // the next message thats allowed will get everybody else up to date
            _log.debug("cannot send message! over throttle!");
        }
    }

    protected static const SEND_THROTTLE_TIME :int = 10200; // in ms, with a buffer

    protected var _gameCtrl :EZGameControl;
    protected var _sendTimes :Array = new Array();

    protected var _log :Log = Log.getLog(this);
}
}
