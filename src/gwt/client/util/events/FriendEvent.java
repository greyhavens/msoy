//
// $Id$

package client.util.events;

import com.google.gwt.core.client.JavaScriptObject;

import com.threerings.msoy.data.all.MemberName;

import client.util.FlashClients;

/**
 * Notifies when a friend is added or removed.
 */
public class FriendEvent extends FlashEvent
{
    /** The action dispatched when a friend is added: defined in BaseClient.as. */
    public static final int FRIEND_ADDED = 1;

    /** The action dispatched when a friend is removed: defined in BaseClient.as. */
    public static final int FRIEND_REMOVED = 2;

    /** The name of this event type: defined in BaseClient.as. */
    public static final String NAME = "friend";

    @Override // FlashEvent
    public String getEventName ()
    {
        return NAME;
    }

    public FriendEvent ()
    {
    }

    public FriendEvent (int action, MemberName friend)
    {
        _action = action;
        _memberId = friend.getMemberId();
        _displayName = friend.toString();
    }

    @Override // FlashEvent
    public void readFlashArgs (JavaScriptObject args) 
    {
        _action = FlashClients.getIntElement(args, 0);
        _displayName = FlashClients.getStringElement(args, 1);
        _memberId = FlashClients.getIntElement(args, 2);
    }

    @Override // FlashEvent
    public void notifyListener (FlashEventListener listener)
    {
        if (listener instanceof FriendsListener) {
            switch (_action) {
            case FRIEND_ADDED:
                ((FriendsListener) listener).friendAdded(this);
                break;
            case FRIEND_REMOVED:
                ((FriendsListener) listener).friendRemoved(this);
                break;
            }
        }
    }

    public int getAction ()
    {
        return _action;
    }

    public MemberName getFriend ()
    {
        return new MemberName(_displayName, _memberId);
    }

    protected int _action;
    protected int _memberId;
    protected String _displayName;
}
