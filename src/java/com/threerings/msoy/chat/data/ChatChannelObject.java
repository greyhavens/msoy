//
// $Id$

package com.threerings.msoy.chat.data;

import com.threerings.presents.dobj.DObject;
import com.threerings.presents.dobj.DSet;

import com.threerings.msoy.data.VizMemberName;

import com.threerings.crowd.chat.data.SpeakMarshaller;
import com.threerings.crowd.chat.data.SpeakObject;

import com.threerings.msoy.chat.data.ChannelMessage;
import com.threerings.msoy.chat.data.ChatChannel;

/**
 * Used to distribute chat messages to all participants in a chat channel.
 */
public class ChatChannelObject extends DObject
    implements SpeakObject
{
    // AUTO-GENERATED: FIELDS START
    /** The field name of the <code>channel</code> field. */
    public static final String CHANNEL = "channel";

    /** The field name of the <code>chatters</code> field. */
    public static final String CHATTERS = "chatters";

    /** The field name of the <code>recentMessages</code> field. */
    public static final String RECENT_MESSAGES = "recentMessages";

    /** The field name of the <code>speakService</code> field. */
    public static final String SPEAK_SERVICE = "speakService";
    // AUTO-GENERATED: FIELDS END

    /** This channel's metadata (type, name). */
    public ChatChannel channel;

    /** Info for all channel participants. */
    public DSet<VizMemberName> chatters = new DSet<VizMemberName>();

    /**
     * Peer storage for recently received ChannelMessage chat message instances.
     * This field has special behavior in a peer environment: its value is not distributed;
     * instead, each peer server will listen for chat messages separately, and update its
     * own local instance of the array.
     * <p> Also, clients will not be notified about changes to this array. The data is intended
     * only to be used when initially subscribing to the distributed object, in order to obtain
     * a snapshot of the history of recent messages. After subscribing, the client will not
     * receive update notifications, instead it should listen for ChannelMessage events
     * and process them as desired.
     */
    public ChannelMessage[] recentMessages = new ChannelMessage[] { };

    /**
     * Used to generate speak requests on this place object.
     * This field has special behavior in a peer environment: its value is not distributed;
     * instead, each peer server overwrites it with a reference to its own local provider instance.
     */
    public SpeakMarshaller speakService;

    // from interface SpeakObject
    public void applyToListeners (ListenerOp op)
    {
        for (VizMemberName name : chatters) {
            op.apply(name);
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
    public void addToChatters (VizMemberName elem)
    {
        requestEntryAdd(CHATTERS, chatters, elem);
    }

    /**
     * Requests that the entry matching the supplied key be removed from
     * the <code>chatters</code> set. The set will not change until the
     * event is actually propagated through the system.
     */
    public void removeFromChatters (Comparable<?> key)
    {
        requestEntryRemove(CHATTERS, chatters, key);
    }

    /**
     * Requests that the specified entry be updated in the
     * <code>chatters</code> set. The set will not change until the event is
     * actually propagated through the system.
     */
    public void updateChatters (VizMemberName elem)
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
    public void setChatters (DSet<VizMemberName> value)
    {
        requestAttributeChange(CHATTERS, value, this.chatters);
        DSet<VizMemberName> clone = (value == null) ? null : value.typedClone();
        this.chatters = clone;
    }

    /**
     * Requests that the <code>recentMessages</code> field be set to the
     * specified value. The local value will be updated immediately and an
     * event will be propagated through the system to notify all listeners
     * that the attribute did change. Proxied copies of this object (on
     * clients) will apply the value change when they received the
     * attribute changed notification.
     */
    public void setRecentMessages (ChannelMessage[] value)
    {
        ChannelMessage[] ovalue = this.recentMessages;
        requestAttributeChange(
            RECENT_MESSAGES, value, ovalue);
        this.recentMessages = (value == null) ? null : value.clone();
    }

    /**
     * Requests that the <code>index</code>th element of
     * <code>recentMessages</code> field be set to the specified value.
     * The local value will be updated immediately and an event will be
     * propagated through the system to notify all listeners that the
     * attribute did change. Proxied copies of this object (on clients)
     * will apply the value change when they received the attribute
     * changed notification.
     */
    public void setRecentMessagesAt (ChannelMessage value, int index)
    {
        ChannelMessage ovalue = this.recentMessages[index];
        requestElementUpdate(
            RECENT_MESSAGES, index, value, ovalue);
        this.recentMessages[index] = value;
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
