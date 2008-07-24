//
// $Id$

package com.threerings.msoy.avrg.client {

import flash.events.Event;

public class ThaneAVRGameBackend
{
    public function isConnected () :Boolean
    {
        return _userFuncs != null;
    }

    public function getConnectListener () :Function
    {
        return handleUserCodeConnect;
    }

    protected function handleUserCodeConnect (evt :Object) :void
    {
        var props :Object = evt.props;
        _userFuncs = props.userProps;

        var ourProps :Object = new Object();
        populateProperties(ourProps);
        props["hostProps"] = ourProps;
    }

    protected function populateProperties (props :Object) :void
    {
    }

    protected var _userFuncs :Object;
}

}
