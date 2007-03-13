package com.threerings.msoy.world.client {

import com.threerings.msoy.client.ControlBackend;

public class EntityBackend extends ControlBackend
{
    /**
     * More initialization: set the sprite we control.
     */
    public function setSprite (sprite :MsoySprite) :void
    {
        _sprite = sprite;
    }

    override public function shutdown () :void
    {
        super.shutdown();

        // disconnect the sprite so that badly-behaved usercode cannot
        // touch it anymore
        _sprite = null;
    }

    override protected function populateControlProperties (o :Object) :void
    {
        super.populateControlProperties(o);

        // we give usercode functions in the backend
        // (instead of connecting them directly) so that we can easily
        // disconnect the sprite from the usercode
        o["requestControl_v1"] = requestControl_v1;
        o["lookupMemory_v1"] = lookupMemory_v1;
        o["updateMemory_v1"] = updateMemory_v1;
        o["getInstanceId_v1"] = getInstanceId_v1;
        o["setHotSpot_v1"] = setHotSpot_v1;
        o["sendMessage_v1"] = sendMessage_v1;

        // deprecated methods
        o["triggerEvent_v1"] = triggerEvent_v1;
    }

    protected function requestControl_v1 () :void
    {
        _sprite.requestControl();
    }

    protected function getInstanceId_v1 () :int
    {
        return _sprite.getInstanceId();
    }

    protected function lookupMemory_v1 (key :String) :Object
    {
        return _sprite.lookupMemory(key);
    }

    protected function updateMemory_v1 (key :String, value :Object) :Boolean
    {
        return _sprite.updateMemory(key, value);
    }

    protected function setHotSpot_v1 (x :Number, y :Number) :void
    {
        _sprite.setHotSpot(x, y);
    }

    protected function sendMessage_v1 (name :String, arg :Object, isAction :Boolean) :void
    {
        _sprite.sendMessage(name, arg, isAction);
    }

    // Deprecated on 2007-03-12
    protected function triggerEvent_v1 (event :String, arg :Object = null) :void
    {
        sendMessage_v1(event, arg, true);
    }

    /** The sprite that this backend is connected to. */
    protected var _sprite :MsoySprite;
}
}
