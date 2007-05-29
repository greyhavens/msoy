//
// $Id$

package com.threerings.msoy.client.notifications {

import mx.controls.Text;
    
public class ClientOnlyMessageDisplay extends NotificationDisplay
{
    public function ClientOnlyMessageDisplay (dispatch :NotificationHandler, message :String)
    {
        super(dispatch, null);
        _message = message;
    }

    // from TitleWindow
    override protected function createChildren () :void
    {
        super.createChildren();

        var label :Text = new Text();
        label.text = _message;
        label.percentWidth = 100;
        addChild(label);
    }

    protected var _message :String;

}
}
