//
// $Id$

package com.threerings.msoy.chat.data;

import com.threerings.presents.dobj.DObject;
import com.threerings.presents.dobj.DSet;

import com.threerings.crowd.chat.data.SpeakMarshaller;
import com.threerings.crowd.chat.data.SpeakObject;

import com.threerings.msoy.chat.data.ChatChannel;
import com.threerings.msoy.data.MemberObject;
import com.threerings.msoy.data.all.MemberName;

/**
 * Used to distribute chat messages to all participates in a chat channel.
 */
public class ChatChannelObject extends DObject
    implements SpeakObject
{
    // AUTO-GENERATED: FIELDS START
    /** The field name of the <code>channel</code> field. */
    public static final String CHANNEL = "channel";

    /** The field name of the <code>chatters</code> field. */
    public static final String CHATTERS = "chatters";

    /** The field name of the <code>speakService</code> field. */
    public static final String SPEAK_SERVICE = "speakService";
    // AUTO-GENERATED: FIELDS END

    /** This channel's metadata (type, name). */
    public ChatChannel channel;

    /** Info for all channel participants. */
    public DSet<ChatterInfo> chatters = new DSet<ChatterInfo>();

    /** Used to generate speak requests on this place object. */
    public SpeakMarshaller speakService;

    // from interface SpeakObject
    public void applyToListeners (ListenerOp op)
    {
        for (ChatterInfo info : chatters) {
            op.apply(info.name);
        }
    }

    // AUTO-GENERATED: METHODS START
    /**
     * Requests that the <code>channel</code> field be set to the
     * specified value. The local value will be updated immediately and an
     * event will be propagated through the system to notify all listeners
     * that the attribute did change. Proxied copies of this object (on
     * clients) will apply the value change when they received the
     * attribute changed notification.
     */
    public void setChannel (ChatChannel value)
    {
        ChatChannel ovalue = this.channel;
        requestAttributeChange(
            CHANNEL, value, ovalue);
        this.channel = value;
    }

    /**
     * Requests that the specified entry be added to the
     * <code>chatters</code> set. The set will not change until the event is
     * actually propagated through the system.
     */
    public void addToChatters (ChatterInfo elem)
    {
        requestEntryAdd(CHATTERS, chatters, elem);
    }

    /**
     * Requests that the entry matching the supplied key be removed from
     * the <code>chatters</code> set. The set will not change until the
     * event is actually propagated through the system.
     */
    public void removeFromChatters (Comparable key)
    {
        requestEntryRemove(CHATTERS, chatters, key);
    }

    /**
     * Requests that the specified entry be updated in the
     * <code>chatters</code> set. The set will not change until the event is
     * actually propagated through the system.
     */
    public void updateChatters (ChatterInfo elem)
    {
        requestEntryUpdate(CHATTERS, chatters, elem);
    }

    /**
     * Requests that the <code>chatters</code> field be set to the
     * specified value. Generally one only adds, updates and removes
     * entries of a distributed set, but certain situations call for a
     * complete replacement of the set value. The local value will be
     * updated immediately and an event will be propagated through the
     * system to notify all listeners that the attribute did
     * change. Proxied copies of this object (on clients) will apply the
     * value change when they received the attribute changed notification.
     */
    public void setChatters (DSet<com.threerings.msoy.chat.data.ChatterInfo> value)
    {
        requestAttributeChange(CHATTERS, value, this.chatters);
        @SuppressWarnings("unchecked") DSet<com.threerings.msoy.chat.data.ChatterInfo> clone =
            (value == null) ? null : value.typedClone();
        this.chatters = clone;
    }

    /**
     * Requests that the <code>speakService</code> field be set to the
     * specified value. The local value will be updated immediately and an
     * event will be propagated through the system to notify all listeners
     * that the attribute did change. Proxied copies of this object (on
     * clients) will apply the value change when they received the
     * attribute changed notification.
     */
    public void setSpeakService (SpeakMarshaller value)
    {
        SpeakMarshaller ovalue = this.speakService;
        requestAttributeChange(
            SPEAK_SERVICE, value, ovalue);
        this.speakService = value;
    }
    // AUTO-GENERATED: METHODS END
}
