
package com.threerings.msoy.avrg.client {

import com.threerings.presents.dobj.DObject;
import com.threerings.presents.dobj.MessageEvent;
import com.threerings.presents.dobj.MessageListener;
import com.threerings.util.Integer;
import com.threerings.util.ObjectMarshaller;
import com.whirled.game.data.PropertySetEvent;
import com.whirled.game.data.PropertySetListener;

/**
 * Listens for property changes and messages and dispatches to named functions.
 */
public class BackendNetAdapter
    implements PropertySetListener, MessageListener
{
    /**
     * Creates a new adapter. If a function name is given as null, then that function will not
     * be dispatched. If not null, then an exception is thrown if the value could not be found.
     * @object the object on which messages and property changes will be posted
     * @messageName the name of messages to filter on, e.g. "Umsg"
     * @userFuncs the dictionary of front end user functions
     * @propertyChange the name of the user function to use for dispatching property changes
     * @messageReceived the name of the user function to use for dispatching messages
     */
    public function BackendNetAdapter (
        object :DObject, messageName :String, userFuncs :Object, propertyChanged :String, 
        messageReceived :String) :void
    {
        _object = object;
        _messageName = messageName;

        if (propertyChanged != null) {
            _propertyChanged = userFuncs[propertyChanged] as Function;
            if (_propertyChanged == null) {
                throw new Error("User function " + propertyChanged + " not found");
            }
        }

        if (messageReceived != null) {
            _messageReceived = userFuncs[messageReceived] as Function;
            if (_messageReceived == null) {
                throw new Error("User function " + messageReceived + " not found");
            }
        }

        _object.addListener(this);
    }

    public function setTargetId (targetId :int) :void
    {
        _targetId = targetId;
    }

    /**
     * Removes this adapter from the object's listeners.
     */
    public function release () :void
    {
        _object.removeListener(this);
    }

    /** @inheritDoc */
    // from PropertySetListener
    public function propertyWasSet (event :PropertySetEvent) :void
    {
        if (_propertyChanged == null) {
            return;
        }

        var key :Integer = event.getKey();
        var keyObj :Object = (key == null) ? null : key.value;

        try {
            if (_targetId != 0) {
                _propertyChanged(
                    _targetId, event.getName(), event.getValue(), event.getOldValue(), keyObj);

            } else {
                _propertyChanged(
                    event.getName(), event.getValue(), event.getOldValue(), keyObj);
            }

        } catch (err :Error) {
            BackendUtils.log.warning("Error in user-code: " + err);
            BackendUtils.log.logStackTrace(err);
        }
    }

    /** @inheritDoc */
    // from MessageListener
    public function messageReceived (event :MessageEvent) :void
    {
        if (_messageReceived == null) {
            return;
        }

        if (event.getName() == _messageName) {
            var args :Array = event.getArgs();
            var mname :String = (args[0] as String);
            var data :Object = ObjectMarshaller.decode(args[1]);
            var senderId :int = (args[2] as int);

            try {
                _messageReceived(mname, data, senderId);

            } catch (err :Error) {
                BackendUtils.log.warning("Error in user-code: " + err);
                BackendUtils.log.logStackTrace(err);
            }
        }
    }

    protected var _object :DObject;
    protected var _messageName :String;
    protected var _propertyChanged :Function;
    protected var _messageReceived :Function;
    protected var _targetId :int = 0;
}
}
