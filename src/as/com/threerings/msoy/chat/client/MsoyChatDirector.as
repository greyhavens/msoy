//
// $Id$

package com.threerings.msoy.chat.client {

import com.threerings.util.ClassUtil;
import com.threerings.util.HashMap;
import com.threerings.util.MessageBundle;
import com.threerings.util.Name;

import com.threerings.crowd.chat.client.ChatDirector;
import com.threerings.crowd.chat.client.ChatDisplay;
import com.threerings.crowd.chat.data.ChatCodes;
import com.threerings.crowd.chat.data.ChatMessage;
import com.threerings.crowd.chat.data.TellFeedbackMessage;
import com.threerings.crowd.chat.data.UserMessage;

import com.threerings.msoy.client.WorldContext;
import com.threerings.msoy.data.MsoyCodes;
import com.threerings.msoy.data.all.ChannelName;
import com.threerings.msoy.data.all.GroupName;
import com.threerings.msoy.data.all.MemberName;

import com.threerings.msoy.chat.data.ChatChannel;
import com.threerings.msoy.chat.data.ChatChannelMarshaller;
import com.threerings.msoy.chat.data.ChatChannelObject;
import com.threerings.msoy.chat.data.ChatterInfo;

/**
 * Handles the dispatching of chat messages based on their "channel" (room/game, individual, or
 * actual custom channel). Manages chat history tracking for same.
 */
public class MsoyChatDirector extends ChatDirector
{
    public static const log :Log = Log.getLog(MsoyChatDirector);

    public function MsoyChatDirector (ctx :WorldContext)
    {
        super(ctx, ctx.getMessageManager(), MsoyCodes.CHAT_MSGS);
        _wctx = ctx;
        _ccpanel = new ChatChannelPanel(_wctx);

        // let the compiler know that these must be compiled into the client
        var c :Class = ChatChannelMarshaller;
        c = ChatterInfo;

        var msg :MessageBundle = _msgmgr.getBundle(_bundle);
        registerCommandHandler(msg, "action", new AvatarActionHandler(false));
        registerCommandHandler(msg, "state", new AvatarActionHandler(true));
    }

    /**
     * Makes the specified chat overlay the current target for room chat. Any previous target will
     * no longer receive chat until {@link #popRoomChatOverlay} is called to remove this overlay
     * from the top of the stack.
     */
    public function pushRoomChatOverlay (target :ChatOverlay) :void
    {
        _roomTargets.unshift(target);
    }

    /**
     * Removes the specified room chat overlay from the list.
     */
    public function popRoomChatOverlay (target :ChatOverlay) :void
    {
        var idx :int = _roomTargets.indexOf(target);
        if (idx != -1) {
            _roomTargets.splice(idx, 1);
        }
    }

    /**
     * Opens the chat interface for the supplied friend, group or private chat channel, selecting
     * the appropriate tab if said channel is already open.
     *
     * @param name either a MemberName, GroupName or ChannelName.
     */
    public function openChannel (name :Name) :void
    {
        var channel :ChatChannel;
        if (name is MemberName) {
            channel = ChatChannel.makeFriendChannel(name as MemberName);
        } else if (name is GroupName) {
            channel = ChatChannel.makeGroupChannel(name as GroupName);
        } else if (name is ChannelName) {
            channel = ChatChannel.makePrivateChannel(name as ChannelName);
        } else {
            log.warning("Requested to open unknown type of channel [name=" + name +
                        ", type=" + ClassUtil.getClassName(name) + "].");
            return;
        }

        // if this is a friend or already open channel, open/select the UI immediately
        if (channel.type == ChatChannel.FRIEND_CHANNEL ||
            _chandlers.containsKey(channel.toLocalType())) {
            _ccpanel.getChatDisplay(channel, getHistory(channel), true);
            return;
        }

        // otherwise we have to subscribe to the channel first
        var ready :Function = function () :void {
            _ccpanel.getChatDisplay(channel, getHistory(channel), true);
        };
        _chandlers.put(channel.toLocalType(), new ChannelHandler(_wctx, channel, ready));
    }

    /**
     * Returns the channel object for the specified channel or null if the channel is not open or
     * has no channel object (is a friend channel).
     */
    public function getChannelObject (channel :ChatChannel) :ChatChannelObject
    {
        var handler :ChannelHandler = _chandlers.get(channel.toLocalType()) as ChannelHandler;
        return (handler == null) ? null : handler.chanobj;
    }

    /**
     * Called when the user closes the tab associated with a particular chat channel.
     */
    public function closeChannel (channel :ChatChannel) :void
    {
        var handler :ChannelHandler = _chandlers.remove(channel.toLocalType()) as ChannelHandler;
        if (handler != null) {
            handler.shutdown();
        }
        // filter out any transient (feedback, etc.) messages from this channel's chat history
        getHistory(channel).filterTransient();
    }

    // from ChatDirector
    override public function pushChatDisplay (display :ChatDisplay) :void
    {
        if (display is ChatOverlay) {
            (display as ChatOverlay).setHistory(_roomHistory);
        }
        super.pushChatDisplay(display);
    }

    // from ChatDirector
    override public function addChatDisplay (display :ChatDisplay) :void
    {
        if (display is ChatOverlay) {
            (display as ChatOverlay).setHistory(_roomHistory);
        }
        super.addChatDisplay(display);
    }

    // from ChatDirector
    override public function dispatchMessage (msg :ChatMessage) :void
    {
        // determine which channel to which this message is targeted
        var channel :ChatChannel = determineChannel(msg);
        if (channel != null) {
            var history :HistoryList = getHistory(channel);
            history.addMessage(msg);
            // if it's a message from a friend, we open the UI even if it's not open
            if (channel.type == ChatChannel.FRIEND_CHANNEL) {
                _ccpanel.getChatDisplay(channel, history, false).displayMessage(msg, false);
            } else {
                // if not, the UI should already be open and if it's not then a message must have
                // come in between the time that we closed the UI and our unsubscribe went through
                var cd :ChatDisplay = _ccpanel.findChatDisplay(channel);
                if (cd != null) {
                    cd.displayMessage(msg, false);
                } else {
                    log.info("Dropping late arriving channel chat message [msg=" + msg + "].");
                }
            }

        } else {
            // add this message to the room chat history
            _roomHistory.addMessage(msg);
            // dispatch it normally as room chat displays are registered normally
            super.dispatchMessage(msg);
        }
    }

    override protected function suppressTooManyCaps () :Boolean
    {
        return false;
    }

    /**
     * Maps the supplied chat message to the (translatable string) name of a chat channel. Returns
     * null if the message is from the current room rather than a chat channel.
     */
    protected function determineChannel (msg :ChatMessage) :ChatChannel
    {
        if ((msg.localtype == ChatCodes.USER_CHAT_TYPE && msg is UserMessage) ||
            msg is TellFeedbackMessage) {
            var umsg :UserMessage = (msg as UserMessage);
            return ChatChannel.makeFriendChannel(umsg.getSpeakerDisplayName() as MemberName);
        }
        var handler :ChannelHandler = _chandlers.get(msg.localtype) as ChannelHandler;
        if (handler != null) {
            return handler.channel;
        }
        return null;
    }

    /**
     * Returns the chat history for the specified channel, creating it if necessary.
     */
    protected function getHistory (channel :ChatChannel) :HistoryList
    {
        var history :HistoryList = (_histories.get(channel) as HistoryList);
        if (history == null) {
            _histories.put(channel, history = new HistoryList());
        }
        return history;
    }

    protected var _wctx :WorldContext;
    protected var _ccpanel :ChatChannelPanel;

    /** Contains a mapping from chat localtype to channel handler. */
    protected var _chandlers :HashMap = new HashMap();

    protected var _roomTargets :Array = new Array();
    protected var _roomHistory :HistoryList = new HistoryList();
    protected var _histories :HashMap = new HashMap();
}
}

import com.threerings.util.MessageBundle;

import com.threerings.presents.client.ResultWrapper;
import com.threerings.presents.dobj.DObject;
import com.threerings.presents.dobj.ObjectAccessError;
import com.threerings.presents.dobj.Subscriber;
import com.threerings.presents.util.SafeSubscriber;

import com.threerings.msoy.client.WorldContext;
import com.threerings.msoy.data.MsoyCodes;

import com.threerings.msoy.chat.client.ChatChannelService;
import com.threerings.msoy.chat.data.ChatChannel;
import com.threerings.msoy.chat.data.ChatChannelObject;

class ChannelHandler implements Subscriber
{
    public var channel :ChatChannel;
    public var chanobj :ChatChannelObject;

    public function ChannelHandler (ctx :WorldContext, channel :ChatChannel, ready :Function)
    {
        this.channel = channel;
        _ctx = ctx;
        _ready = ready;
        _ccsvc = (_ctx.getClient().requireService(ChatChannelService) as ChatChannelService);

        // start by joining the chat channel
        _ccsvc.joinChannel(_ctx.getClient(), channel, new ResultWrapper(failed, gotChannelOid));
    }

    public function shutdown () :void
    {
        if (_ccsub != null) {
            _ccsub.unsubscribe(_ctx.getClient().getDObjectManager());
            _ccsub = null;
        }
        if (chanobj != null) {
            _ctx.getChatDirector().removeAuxiliarySource(chanobj);
            chanobj = null;
        }
        _ccsvc.leaveChannel(_ctx.getClient(), channel);
        _isShutdown = true;
    }

    // from Subscriber
    public function objectAvailable (obj :DObject) :void
    {
        chanobj = (obj as ChatChannelObject);
        _ctx.getChatDirector().addAuxiliarySource(chanobj, channel.toLocalType());
        _ready();
    }

    // from Subscriber
    public function requestFailed (oid :int, cause :ObjectAccessError) :void
    {
        failed(cause.message);
    }

    protected function gotChannelOid (result :Object) :void
    {
        if (_isShutdown) {
            // zoiks! we got shutdown before we got our channel oid, just leave
            _ccsvc.leaveChannel(_ctx.getClient(), channel);
        } else {
            _ccsub = new SafeSubscriber(int(result), this);
            _ccsub.subscribe(_ctx.getClient().getDObjectManager());
        }
    }

    protected function failed (cause :String) :void
    {
        var msg :String = MessageBundle.compose("m.join_channel_failed", cause);
        _ctx.displayFeedback(MsoyCodes.CHAT_MSGS, msg);
    }

    protected var _ctx :WorldContext;
    protected var _ready :Function;
    protected var _isShutdown :Boolean = false;

    protected var _ccsvc :ChatChannelService;
    protected var _ccsub :SafeSubscriber;
}
