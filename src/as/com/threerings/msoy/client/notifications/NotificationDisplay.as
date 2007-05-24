//
// $Id$

package com.threerings.msoy.client.notifications {

import flash.display.DisplayObject;
import flash.events.TimerEvent;
import flash.utils.Timer;

import com.threerings.msoy.data.Notification;
import com.threerings.msoy.ui.FloatingPanel;

public /* abstract */ class NotificationDisplay extends FloatingPanel
{
    public function NotificationDisplay (n :Notification, dispatch :NotificationHandler)
    {
        super(dispatch.getWorldContext(), "");
        this.showCloseButton = true;
        this.styleName = "notificationDisplay";
        this.width = 200; // all notifications have the same width, for now at least
        
        _dispatch = dispatch;
        _id = n.id;

        _timer = new Timer(timeout, 1);
        _timer.addEventListener(TimerEvent.TIMER, function (event :TimerEvent) :void {
                close();
            });
    }

    /**
     * Returns the default notification timeout, in milliseconds. After this period of time, the
     * notification popup will close. Subclasses should override it as appropriate.
     */
    public function get timeout () :Number
    {
        return 5000; // seems like a reasonable number
    }

    /** Return the Id of the notification we're displaying. */
    public function get notificationId () :Number
    {
        return _id;
    }

    // from FloatingPanel
    override public function open
        (modal :Boolean = false, parent :DisplayObject = null, avoid :DisplayObject = null) :void
    {
        super.open(modal, parent, avoid);
        _timer.start();
    }

    // from FloatingPanel
    override public function close () :void
    {
        _timer.stop();
        super.close();
        _dispatch.notificationClosed(this);
    }

    /** Local storage of the notification Id. */
    public var _id :int;
    
    /** Reference back to the dispatch object. */
    protected var _dispatch :NotificationHandler;

    /** Timer that will cause this window to close after the given timeout. */
    protected var _timer :Timer;
}
}
