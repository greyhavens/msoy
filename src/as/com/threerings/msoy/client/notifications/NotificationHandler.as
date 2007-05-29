//
// $Id$

package com.threerings.msoy.client.notifications {

import com.threerings.msoy.chat.client.ReportingListener;
import com.threerings.util.ClassUtil;
import com.threerings.util.MessageBundle;
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

        // notification -> popup mapping. please note: this mapping ignores notification
        // inheritance hierarchy. each subclass of Notification requires its own popup entry.
        _definitions = [
            { type: FriendStatusChangeNotification,
              popup: FriendStatusChangeDisplay },
            
            { type: FriendAcceptedInvitationNotification,
              popup: FriendAcceptedInvitationDisplay },
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
                displayPopup(makePopupFromNotification(notification));
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

    /**
     * Displays a new text-only popup window.
     */
    public function displayMessage (bundle :String, message :String) :void
    {
        var msgb :MessageBundle = getWorldContext().getMessageManager().getBundle(bundle);
        if (msgb == null) {
            Log.getLog(this).warning("No message bundle available to translate message " +
                                     "[bundle=" + bundle + ", message=" + message + "].");
        } else {
            message = msgb.xlate(message);
        }
    
        displayPopup(new ClientOnlyMessageDisplay(this, message));
    }
    
    /**
     * Called by individual popup windows after closing. Removes the display object from popup
     * list, rearranges those still displayed and, if appropriate, informs the server.
     */
    public function notificationClosed (popup :NotificationDisplay) :void
    {
        var index :int = _popups.indexOf(popup);
        if (index != -1) {
            _popups.splice(index, 1);
        }

        layout();

        if (! popup.clientOnly) {
            _msvc.acknowledgeNotification(
                getWorldContext().getClient(), popup.notificationId,
                new ReportingListener(getWorldContext()));
        }            
    }

    /** Retrieves this dispatch object's world context. */
    public function getWorldContext () :WorldContext
    {
        return _ctx as WorldContext;
    }

    /** Creates and returns a new popup display object for the given notification instance. */
    protected function makePopupFromNotification (notification :Notification) :NotificationDisplay
    {
        var nClass :Class = ClassUtil.getClass(notification);
        var popupClass :Class;
        _definitions.some(function (o :Object, i :*, a :*) :Boolean {
                if (o.type == nClass) {
                    popupClass = o.popup;
                }
                return popupClass != null;
            });

        if (popupClass != null) {
            return new popupClass(this, notification);
        } else {
            Log.getLog(this).warning("Notification popup not found for: " + notification);
            return null;
        }
    }
    
    /** Creates and displays an appropriate notification UI. */
    protected function displayPopup (popup :NotificationDisplay) :void
    {
        popup.open();
        _popups.push(popup);
        layout();
    }

    /** Updates the position of each popup window. */
    public function layout () :void
    {
        var header :HeaderBar = _panel.getHeaderBar();
        var placebox :PlaceBox = _panel.getPlaceContainer();
        
        var right :Number = header.x + header.width;
        var y :Number = placebox.y;
        
        for each (var item :NotificationDisplay in _popups) {
            item.y = y;
            item.x = right - item.width;
            y += item.height;
        }
        
    }

    protected var _panel :TopPanel;
    protected var _msvc :MemberService;

    /** An array of object that specify mapping from Notification classes to
     *  popup classes that can display them. */
    protected var _definitions :Array;

    /** Contains a list of notification popups currently being shown. */
    protected var _popups :Array = new Array();
}
}
