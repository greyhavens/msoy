//
// $Id$

package com.threerings.msoy.client.notifications {

import com.threerings.msoy.chat.client.ReportingListener;
import com.threerings.util.ClassUtil;
import com.threerings.msoy.client.HeaderBar;
import com.threerings.msoy.client.MemberService;
import com.threerings.msoy.client.PlaceBox;
import com.threerings.msoy.client.TopPanel;
import com.threerings.msoy.client.WorldContext;
import com.threerings.msoy.data.FriendAcceptedInvitationNotification;
import com.threerings.msoy.data.FriendStatusChangeNotification;
import com.threerings.msoy.data.MemberObject;
import com.threerings.msoy.data.MsoyCodes;
import com.threerings.msoy.data.Notification;
import com.threerings.presents.client.BasicDirector;
import com.threerings.presents.client.Client;
import com.threerings.presents.dobj.EntryAddedEvent;
import com.threerings.presents.dobj.EntryRemovedEvent;
import com.threerings.presents.dobj.EntryUpdatedEvent;
import com.threerings.presents.dobj.SetListener;

public class NotificationHandler extends BasicDirector
    implements SetListener
{
    public function NotificationHandler (ctx :WorldContext, panel :TopPanel)
    {
        super(ctx);

        _panel = panel;

        // notification -> display mapping. please note: this mapping ignores notification
        // inheritance hierarchy. each subclass of Notification requires its own display entry.
        _definitions = [
            { type: FriendStatusChangeNotification,
              display: FriendStatusChangeDisplay },
            
            { type: FriendAcceptedInvitationNotification,
              display: FriendAcceptedInvitationDisplay },
            ];
    }

    // from BasicDirector
    override protected function clientObjectUpdated (client :Client) :void
    {
        super.clientObjectUpdated(client);
        client.getClientObject().addListener(this);
    }

    // from BasicDirector
    override protected function registerServices (client :Client) :void
    {
        super.registerServices(client);
        client.addServiceGroup(MsoyCodes.BASE_GROUP);
    }

    // from BasicDirector
    override protected function fetchServices (client :Client) :void
    {
        super.fetchServices(client);
        _msvc = (client.requireService(MemberService) as MemberService);
    }

    // from interface SetListener
    public function entryAdded (event :EntryAddedEvent) :void
    {
        var name :String = event.getName();
        if (name == MemberObject.NOTIFICATIONS) {
            var notification :Notification = event.getEntry() as Notification;
            if (notification != null) {
                displayNotification(notification);
            } else {
                Log.getLog(this).warning(
                    "Received a notification event with an invalid entry: " + event.getEntry());
            }
        }
    }

    // from interface SetListener
    public function entryUpdated (event :EntryUpdatedEvent) :void
    {
        // no op
    }

    // from interface SetListener
    public function entryRemoved (event :EntryRemovedEvent) :void
    {
        // no op
    }

    /** Called by individual popup windows, tells the server the notification was processed,
     *  and can be removed from the player's list. */
    public function notificationClosed (display :NotificationDisplay) :void
    {
        var index :int = _displays.indexOf(display);
        if (index != -1) {
            _displays.splice(index, 1);
        }

        layout();
        
        _msvc.acknowledgeNotification(
            getWorldContext().getClient(), display.notificationId,
            new ReportingListener(getWorldContext()));
    }

    /** Retrieves this dispatch object's world context. */
    public function getWorldContext () :WorldContext
    {
        return _ctx as WorldContext;
    }

    /** Creates and returns a new display display object for the given notification instance. */
    protected function getDisplay (notification :Notification) :NotificationDisplay
    {
        var nClass :Class = ClassUtil.getClass(notification);
        var displayClass :Class;
        _definitions.some(function (o :Object, i :*, a :*) :Boolean {
                if (o.type == nClass) {
                    displayClass = o.display;
                }
                return displayClass != null;
            });

        if (displayClass != null) {
            return new displayClass(notification, this);
        } else {
            Log.getLog(this).warning("Notification display not found for: " + notification);
            return null;
        }
    }
    
    /** Creates and displays an appropriate notification UI. */
    protected function displayNotification (notification :Notification) :void
    {
        var display :NotificationDisplay = getDisplay(notification);
        display.open();
        _displays.push(display);
        layout();
    }

    /** Updates the position of each display window. */
    public function layout () :void
    {
        var header :HeaderBar = _panel.getHeaderBar();
        var placebox :PlaceBox = _panel.getPlaceContainer();
        
        var right :Number = header.x + header.width;
        var y :Number = placebox.y;
        
        for each (var item :NotificationDisplay in _displays) {
            item.y = y;
            item.x = right - item.width;
            y += item.height;
        }
        
    }

    protected var _panel :TopPanel;
    protected var _msvc :MemberService;

    /** An array of object that specify mapping from Notification classes to
     *  display classes that can display them. */
    protected var _definitions :Array;

    /** Contains a list of notification displays currently being shown. */
    protected var _displays :Array = new Array();
}
}
