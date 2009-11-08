//
// $Id$

package com.threerings.msoy.notify.client {

import mx.containers.VBox;

import mx.controls.scrollClasses.ScrollBar;

import mx.core.ScrollPolicy;
import mx.core.UIComponent;

public class NotificationHistoryDisplay extends VBox
{
    // TODO: hmm, without the fudge, text is cropped
    public static const FUDGE :int = 10;
    public static const PADDING :int = ScrollBar.THICKNESS + FUDGE;

    public function NotificationHistoryDisplay (notifications :Array)
    {
        styleName = "notificationHistoryDisplay";
        height = 200;
        _notifications = notifications;

        horizontalScrollPolicy = ScrollPolicy.OFF;
        verticalScrollPolicy = ScrollPolicy.ON;
    }

    public function addNotification (notifDisplay :UIComponent) :void
    {
        var atBottom :Boolean = (verticalScrollPosition == maxVerticalScrollPosition);
        addChild(notifDisplay);
        if (atBottom) {
            deferScrollToEnd();
        }
    }

    override protected function createChildren () :void
    {
        super.createChildren();

        // make sure any left over vertical space is used up
        var spacer :VBox = new VBox();
        spacer.percentWidth = 100;
        spacer.percentHeight = 100;
        addChild(spacer);

        for each (var notification :UIComponent in _notifications) {
            // now add the notification
            addChild(notification);
        }
        deferScrollToEnd();
    }

    protected function deferScrollToEnd () :void
    {
        callLater(function () :void {
            verticalScrollPosition = maxVerticalScrollPosition;
        });
    }

    protected var _notifications :Array;
}
}
