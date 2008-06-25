//
// $Id$

package com.threerings.msoy.notify.client {

import flash.display.DisplayObject;
import flash.display.DisplayObjectContainer;

import flash.events.MouseEvent;
import flash.events.TextEvent;

import flash.geom.Point;

import flash.text.AntiAliasType;
import flash.text.TextField;
import flash.text.TextFieldAutoSize;
import flash.text.TextFormat;

import mx.containers.Canvas;
import mx.containers.HBox;

import mx.controls.Button;

import mx.core.Application;
import mx.core.UIComponent;
import mx.core.ScrollPolicy;

import mx.managers.PopUpManager;

import caurina.transitions.Tweener;

import com.threerings.flex.FlexWrapper;
import com.threerings.flex.PopUpUtil;

import com.threerings.util.Log;

import com.threerings.msoy.utils.TextUtil;

import com.threerings.msoy.world.client.WorldContext;

import com.threerings.msoy.chat.client.ChatOverlay;

public class NotificationDisplay extends HBox
{
    public function NotificationDisplay (ctx :WorldContext) :void
    {
        _ctx = ctx;
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
        _popupBtn.enabled = false;
    }

    public function displayNotification (notification :String) :void
    {
        _pendingNotifications.push(notification);
        checkPendingNotifications();

        if (_notificationHistory != null) {
            if (_notificationHistory.verticalScrollPosition == 
                _notificationHistory.maxVerticalScrollPosition) {
                callLater(function () :void {
                    _notificationHistory.verticalScrollPosition = 
                        _notificationHistory.maxVerticalScrollPosition;
                });
            }
            _notificationHistory.addChild(createDisplay(notification, true));
        }
    }

    override protected function createChildren () :void
    {
        super.createChildren();
        styleName = "notificationDisplay"; 
        percentHeight = 100;

        addChild(_popupBtn = new Button());
        _popupBtn.styleName = "notificationClosedBtn";
        _popupBtn.width = 20;
        _popupBtn.height = 19;
        _popupBtn.buttonMode = true;
        _popupBtn.enabled = false;
        _popupBtn.addEventListener(MouseEvent.CLICK, displayNotificationHistory);
        systemManager.addEventListener(MouseEvent.CLICK, hideNotificationHistory);
        
        addChild(_canvas = new Canvas());
        _canvas.styleName = "notificationCanvas";
        _canvas.width = 200;
        _canvas.height = 19;
        _canvas.horizontalScrollPolicy = ScrollPolicy.OFF;
        _canvas.verticalScrollPolicy = ScrollPolicy.OFF;
    }

    protected function checkPendingNotifications () :void
    {
        if (_currentlyAnimating || _pendingNotifications.length == 0) {
            return;
        }
        
        _currentlyAnimating = true;
        var notification :UIComponent = createDisplay(_pendingNotifications.shift() as String);
        notification.x = _canvas.width;
        _canvas.removeAllChildren();
        _canvas.addChild(notification);
        _popupBtn.enabled = true;
        Tweener.addTween(notification, 
            {x: _canvas.width - notification.width, time: 0.75, transition: "easeoutquart",
                onComplete: function () :void {
                    _currentlyAnimating = false;
                    checkPendingNotifications();
                }
            });
    }

    protected function createDisplay (notification :String, 
        forHistory :Boolean = false) :UIComponent
    {
        var format :TextFormat = ChatOverlay.createChatFormat();
        format.color = 0x999999;
        var text :TextField = new TextField();
        text.multiline = forHistory;
        text.wordWrap = forHistory;
        // I would rather not make the text selectable, but clicking on links doesn't work if it's 
        // not.  wtf?
        text.selectable = true; 
        text.autoSize = TextFieldAutoSize.LEFT;
        text.antiAliasType = AntiAliasType.ADVANCED;
        TextUtil.setText(text, TextUtil.parseLinks(notification, format, true, true), format);

        if (forHistory) {
            text.width = 200;
        } else {
            while (text.width > _canvas.width && text.length > 4) {
                // as odd as this looks, it replaces the last 4 characters with "..."
                text.replaceText(text.length - 5, text.length, "...");
            }
        }

        var wrapper :FlexWrapper = new FlexWrapper(text);
        wrapper.width = text.width;
        wrapper.height = text.height;
        if (!forHistory) {
            return wrapper;
        }

        // if for history, we need to wrap it in the box that can be styled with padding, etc
        var box :HBox = new HBox();
        box.styleName = "notificationHistoryCell";
        box.addChild(wrapper);
        return box;
    }

    protected function displayNotificationHistory (...ignored) :void
    {
        if (_notificationHistory != null) {
            return; 
        }

        _popupBtn.styleName = "notificationOpenBtn";
        _notificationHistory = new NotificationHistoryDisplay(prepareNotifications(
            _ctx.getNotificationDirector().getCurrentNotifications()));
        _notificationHistory.addEventListener(TextEvent.LINK, linkClicked);
        PopUpManager.addPopUp(_notificationHistory, _ctx.getTopPanel(), false);
        var buttonPos :Point = localToGlobal(new Point(_popupBtn.x, _popupBtn.y));
        _notificationHistory.x = buttonPos.x;
        _notificationHistory.y = buttonPos.y - _notificationHistory.height;
        PopUpUtil.fitInRect(_notificationHistory, _ctx.getTopPanel().getPlaceViewBounds());

        // prevent the hide logic from firing off of this mouse click
        _opening = true;
        callLater(function () :void {
            _opening = false;
        });
    }

    protected function hideNotificationHistory (...ignored) :void
    {
        if (_opening || _notificationHistory == null) {
            return;
        }

        _popupBtn.styleName = "notificationClosedBtn";
        PopUpManager.removePopUp(_notificationHistory);
        _notificationHistory = null;
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

    protected function prepareNotifications (strings :Array) :Array
    {
        var notifications :Array = [];
        for each (var notification :String in strings) {
            notifications.push(createDisplay(notification, true));
        }
        return notifications;
    }

    protected static const log :Log = Log.getLog(NotificationDisplay);

    protected var _ctx :WorldContext;
    protected var _canvas :Canvas;
    protected var _popupBtn :Button;
    protected var _pendingNotifications :Array = [];
    protected var _currentlyAnimating :Boolean = false;
    protected var _notificationHistory :NotificationHistoryDisplay;
    protected var _opening :Boolean = false;
}
}
