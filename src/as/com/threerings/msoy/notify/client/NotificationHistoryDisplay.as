//
// $Id$

package com.threerings.msoy.notify.client {

import flash.geom.Point;
import flash.geom.Rectangle;

import mx.containers.VBox;

import mx.controls.scrollClasses.ScrollBar;

import mx.core.ScrollPolicy;
import mx.core.UIComponent;

public class NotificationHistoryDisplay extends VBox
{
    public function NotificationHistoryDisplay (notifications :Array, width :int)
    {
        styleName = "notificationHistoryDisplay";
        height = 200;
        this.width = width + ScrollBar.THICKNESS;
        _notifications = notifications;

        horizontalScrollPolicy = ScrollPolicy.OFF;
        verticalScrollPolicy = ScrollPolicy.ON;
    }

    public function getStageScrollBounds () :Rectangle
    {
        var scroll :ScrollBar = verticalScrollBar; // this is a getter function
        if (scroll == null) {
            return null;
        }
        var pos :Point = localToGlobal(new Point(scroll.x, scroll.y));
        return new Rectangle(pos.x, pos.y, scroll.width, scroll.height);
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

        callLater(function () :void {
            verticalScrollPosition = maxVerticalScrollPosition;
        });
    }

    protected var _notifications :Array;
}
}
