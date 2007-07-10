//
// $Id$

package com.threerings.msoy.notify.client {

import mx.containers.VBox;

import com.threerings.msoy.ui.FloatingPanel;

import com.threerings.msoy.client.WorldContext;

import com.threerings.msoy.notify.data.Notification;

public class NotificationDisplay extends FloatingPanel
{
    public function NotificationDisplay (ctx :WorldContext, notifs :Array)
    {
        super(ctx, "Notifications");
        _notifs = notifs;
        styleName = "notificationDisplay";
        showCloseButton = true;
    }

    override public function close () :void
    {
        super.close();
        _ctx.getNotificationDirector().notificationPanelClosed();
    }

    override protected function createChildren () :void
    {
        super.createChildren();

        var box :VBox = new VBox();

        // TODO: close boxes for each notification
        // TODO: other stuff!
        for each (var notif :Notification in _notifs) {
            box.addChild(notif.getDisplay());
        }
        addChild(box);
    }

    protected var _notifs :Array;
}
}
