//
// $Id$

package com.threerings.msoy.notify.client {

import flash.display.DisplayObject;
import flash.utils.Timer;

import flash.events.MouseEvent;
import flash.events.TextEvent;
import flash.events.TimerEvent;

import flash.geom.Point;
import flash.geom.Rectangle;

import flash.text.AntiAliasType;
import flash.text.TextField;
import flash.text.TextFieldAutoSize;
import flash.text.TextFormat;

import mx.containers.Canvas;
import mx.containers.HBox;

import mx.core.UIComponent;
import mx.core.ScrollPolicy;

import mx.managers.PopUpManager;

import caurina.transitions.Tweener;

import com.threerings.util.ClassUtil;
import com.threerings.util.Log;

import com.threerings.flex.CommandCheckBox;
import com.threerings.flex.FlexWrapper;

import com.threerings.msoy.client.Msgs;
import com.threerings.msoy.client.MsoyContext;
import com.threerings.msoy.utils.TextUtil;

import com.threerings.msoy.chat.client.ChatOverlay;

import com.threerings.msoy.notify.data.Notification;

public class NotificationDisplay extends HBox
{
    public function NotificationDisplay (ctx :MsoyContext) :void
    {
        _ctx = ctx;
        _clearTimer.addEventListener(TimerEvent.TIMER, clearCurrentNotification);
    }

    public function clearDisplay () :void
    {
        if (_canvas.numChildren > 0) {
            // should only have one child
            var child :DisplayObject = _canvas.getChildAt(0);
            Tweener.addTween(child, {x: _canvas.width, time: 0.75, transition: "easeoutquart",
                onComplete: function () :void {
                    if (child.parent == _canvas) {
                        _canvas.removeChild(child);
                    }
                }
            });
        }

        hideNotificationHistory();
    }

    public function displayNotification (notification :Notification) :void
    {
        // TODO: this is here to allow BadgeEarnedNotification to forgo normal notification
        // display.  It would be great if we required it to display something, and then clicking
        // on the announcement caused it to show the fancy display again.
        if (notification.getAnnouncement() == null) {
            // again, this is temporary.  We should always have the notification put something in
            // the history, and probably defer this custom display until the notification's
            // priority has decreed that it get shown in the history display.  Also, all
            // notifications should be allowed to do custom shit, so this is doubly weird.
            displayCustomNotification(notification);
            return;
        }

        _pendingNotifications.push(notification);
        checkPendingNotifications();

        if (_nHistory != null) {
            if (_nHistory.verticalScrollPosition == _nHistory.maxVerticalScrollPosition) {
                callLater(function () :void {
                    _nHistory.verticalScrollPosition = _nHistory.maxVerticalScrollPosition;
                });
            }
            _nHistory.addChild(createDisplay(notification, true));
        }
    }

    public function updatePopupLocation () :void
    {
        if (_nHistory != null) {
            var buttonPos :Point = localToGlobal(new Point(_popupBtn.x, _popupBtn.y));
            _nHistory.x = buttonPos.x;
            _nHistory.y = buttonPos.y - _nHistory.height;
        }
    }

    override protected function createChildren () :void
    {
        super.createChildren();
        styleName = "notificationDisplay";
        this.percentWidth = 100;

        addChild(_popupBtn = new CommandCheckBox(null, toggleNotificationHistory));
        _popupBtn.styleName = "panelToggle";

        addChild(_canvas = new Canvas());
        _canvas.styleName = "notificationCanvas";
        _canvas.percentWidth = 100;
        _canvas.minWidth = 200;
        _canvas.height = 19;
        _canvas.horizontalScrollPolicy = ScrollPolicy.OFF;
        _canvas.verticalScrollPolicy = ScrollPolicy.OFF;
    }

    protected function maybeCloseHistory (event :MouseEvent) :void
    {
        if (_nHistory == null) {
            return;
        }

        var bounds :Rectangle = _nHistory.getStageScrollBounds();
        if (bounds != null && !bounds.contains(event.stageX, event.stageY)) {
            hideNotificationHistory();
        }
    }

    protected function checkPendingNotifications () :void
    {
        if (_currentlyAnimating) {
            return;
        }
        if (_pendingNotifications.length == 0) {
            _clearTimer.start(); // in 60 seconds we'll clear this notification
            return;
        }

        _clearTimer.stop();
        _currentlyAnimating = true;
        var notification :UIComponent = createDisplay(_pendingNotifications.shift() as Notification);
        notification.x = _canvas.width;
        _canvas.removeAllChildren();
        _canvas.addChild(notification);
        Tweener.addTween(notification,
            {x: 0 /*_canvas.width - notification.width*/, time: 0.75, transition: "easeoutquart",
                onComplete: function () :void {
                    _currentlyAnimating = false;
                    checkPendingNotifications();
                }
            });
    }

    protected function displayCustomNotification (notification :Notification) :void
    {
        var clazz :String = notification.getDisplayClass();
        if (clazz == null) {
            return;
        }

        var thing :Object = new (ClassUtil.getClassByName(clazz))();
        thing.init(_ctx, notification);
    }

    protected function clearCurrentNotification (event :TimerEvent = null) :void
    {
        // TODO: fancy fade? that would call attention to a 60 second old notification which
        // doesn't seem like what we want
        _canvas.removeAllChildren();
    }

    protected function createDisplay (
        notification :Notification, forHistory :Boolean = false) :UIComponent
    {
        var format :TextFormat = ChatOverlay.createChatFormat();
        format.color = getColor(notification);
        var text :TextField = new TextField();
        text.multiline = forHistory;
        text.wordWrap = forHistory;
        // I would rather not make the text selectable, but clicking on links doesn't work if it's
        // not.  wtf?
        text.selectable = true;
        text.autoSize = TextFieldAutoSize.LEFT;
        text.antiAliasType = AntiAliasType.ADVANCED;
        const announcement :String = Msgs.NOTIFY.xlate(notification.getAnnouncement());
        TextUtil.setText(text, TextUtil.parseLinks(announcement, format, true, true), format);

        if (forHistory) {
            text.width = getHistoryWidth();
        } else {
            text.width = _canvas.width * 2;
            while (text.textWidth > _canvas.width && text.length > 4) {
                // as odd as this looks, it replaces the last 4 characters with "..."
                text.replaceText(text.length - 5, text.length, "...");
            }
        }

        var wrapper :FlexWrapper = new FlexWrapper(text, true);
        if (!forHistory) {
            return wrapper;
        }

        // if for history, we need to wrap it in the box that can be styled with padding, etc
        var box :HBox = new HBox();
        box.styleName = "notificationHistoryCell";
        box.addChild(wrapper);
        return box;
    }

    protected function toggleNotificationHistory (show :Boolean) :void
    {
        hideNotificationHistory();

        if (show) {
            _nHistory = new NotificationHistoryDisplay(prepareNotifications(
                _ctx.getNotificationDirector().getCurrentNotifications()), getHistoryWidth());
            _nHistory.addEventListener(TextEvent.LINK, linkClicked);
            PopUpManager.addPopUp(_nHistory, _ctx.getTopPanel(), false);
            updatePopupLocation();
            systemManager.addEventListener(MouseEvent.CLICK, maybeCloseHistory);
        }
    }

    protected function hideNotificationHistory (...ignored) :void
    {
        if (_nHistory == null) {
            return;
        }

        PopUpManager.removePopUp(_nHistory);
        systemManager.removeEventListener(MouseEvent.CLICK, maybeCloseHistory);
        _nHistory = null;
        _popupBtn.selected = false;
    }

    protected function getHistoryWidth () :int
    {
        return this.width - 40;
    }

    protected function linkClicked (event :TextEvent) :void
    {
        // TODO: Find a better way to handle this.  The popup'd history display's TextEvents from
        // clicked links aren't making their way out to the appropriate listeners.  So instead,
        // we're listening for them here directly, and dispatching a clone of them on a local
        // TextField that is on the display list.

        // if a link was clicked in the popup, then we must have a text field currently being
        // displayed...
        if (_canvas.numChildren == 0) {
            log.warning("received a TextEvent.LINK, but canvas is empty");
            return;
        }

        var wrapper :FlexWrapper = _canvas.getChildAt(0) as FlexWrapper;
        if (wrapper == null || wrapper.numChildren == 0) {
            log.warning(
                "First child of canvas is not a FlexWrapper, or it's empty [" + wrapper + "]");
            return;
        }

        var text :TextField = wrapper.getChildAt(0) as TextField;
        if (text == null) {
            log.warning("First child of wrapper is not a TextField");
            return;
        }

        // clone the event, and dispatch it on our locally displayed field
        text.dispatchEvent(event.clone());
    }

    protected function prepareNotifications (notifs :Array) :Array
    {
        var notifications :Array = [];
        for each (var notification :Notification in notifs) {
            // TODO: temporary work-around, as noted in displayNotification
            if (notification.getAnnouncement() == null) {
                continue;
            }
            notifications.push(createDisplay(notification, true));
        }
        return notifications;
    }

    protected function getColor (notification :Notification) :uint
    {
        switch (notification.getCategory()) {
        case Notification.SYSTEM: return 0xFF0000;
        case Notification.INVITE: return 0xFFA13D;
        case Notification.PERSONAL: return 0x40B8D2;
        case Notification.BUTTSCRATCHING: // fall through to default
        default: return 0x999999;
        }
    }

    protected static const log :Log = Log.getLog(NotificationDisplay);

    protected var _ctx :MsoyContext;
    protected var _canvas :Canvas;
    protected var _popupBtn :CommandCheckBox;
    protected var _pendingNotifications :Array = [];
    protected var _currentlyAnimating :Boolean = false;
    protected var _nHistory :NotificationHistoryDisplay;
    protected var _clearTimer :Timer = new Timer(60*1000, 1);
}
}
