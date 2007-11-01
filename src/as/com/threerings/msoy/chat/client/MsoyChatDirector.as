//
// $Id$

package com.threerings.msoy.chat.client {

import mx.core.UIComponent;

import com.threerings.presents.client.ClientEvent;
import com.threerings.presents.dobj.MessageEvent;
import com.threerings.util.ClassUtil;
import com.threerings.util.HashMap;
import com.threerings.util.MessageBundle;
import com.threerings.util.Name;

import com.threerings.crowd.data.PlaceObject;

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

import com.threerings.msoy.chat.data.ChannelMessage;
import com.threerings.msoy.chat.data.ChatChannel;
import com.threerings.msoy.chat.data.ChatChannelCodes;
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
        registerCommandHandler(msg, "away", new AwayHandler());
    }

    /**
     * Return true if we've already got a chat channel open with the specified Name.
     */
    public function hasOpenChannel (name :Name) :Boolean
    {
        return (null != _ccpanel.findChatDisplay(makeChannel(name)));
    }

    /**
     * Opens the chat interface for the supplied player, group or private chat channel, selecting
     * the appropriate tab if said channel is already open.
     *
     * @param name either a MemberName, GroupName or ChannelName.
     */
    public function openChannel (name :Name) :void
    {
        var channel :ChatChannel = makeChannel(name);

        // if this is a member or already open channel, open/select the UI immediately
        if (channel.type == ChatChannel.MEMBER_CHANNEL ||
            _chandlers.containsKey(channel.toLocalType())) {
            _ccpanel.getChatDisplay(channel, getHistory(channel), true);
            return;
        }

        // otherwise we have to subscribe to the channel first
        var showTabFn :Function = function (ccobj :ChatChannelObject) :void {
            // once the subscription went through, show the chat history
            _ccpanel.getChatDisplay(channel, getHistory(channel), true);
            // if this is a tabbed channel, make sure to update its distributed object reference
            var tab :ChannelChatTab = _ccpanel.findChatTab(channel);
            if (tab != null) {
                tab.reinit(ccobj);
            }
        };
        _chandlers.put(channel.toLocalType(), new ChannelHandler(_wctx, channel, showTabFn));

        _wctx.getTopPanel().getHeaderBar().addTab("" + name);
    }

    /**
     * Returns the channel object for the specified channel or null if the channel is not open or
     * has no channel object (is a member channel).
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

    /**
     * Displays the specified HTML page inside a tab in the chat panel. If the url points to a
     * ".html" file, and a ".css" file exists at the same location and with the same root name
     * (e.g. "foo.html" and "foo.css" exist in the same directory), the style sheet will be
     * automatically loaded as well. Once loaded, the style sheet is applied to the current and all
     * subsequent pages until replaced by another style sheet.
     *
     * If the named tab doesn't exist, it will be created, otherwise the existing tab is pushed to
     * the foreground. The tab display is not related to a chat channel - it's merely a display
     * vehicle that doesn't support chatting at all. 
     */
    public function displayPage (tabName :String, url :String) :void
    {
        _ccpanel.displayPageTab(tabName, url, true);
    }

    /**
     * Tells the chat panel to grab the room display and put it into a tab.
     */
    public function sendRoomToTab () :void
    {
        if (!_ccpanel.containsRoomTab()) {
            _ccpanel.sendRoomToTab();
        }
    }

    /**
     * Tells the chat panel to remove its room tab (giving the display back to the top panel.
     */
    public function removeRoomTab () :void
    {
        _ccpanel.removeRoomTab();
    }

    /**
     * Returns true of the room view is currently contained within a chat tab.
     */
    public function containsRoomTab () :Boolean
    {
        return _ccpanel.containsRoomTab();
    }

    /**
     * Displays the game chat sidebar.
     */
    public function displayGameChat (chatDtr :ChatDirector, playerList :UIComponent) :void
    {
        _ccpanel.displayGameChat(chatDtr, playerList);
    }

    /**
     * Clears the game chat sidebar.
     */
    public function clearGameChat () :void
    {
        _ccpanel.clearGameChat();
    }

    // from parent superclass BasicDirector
    override public function clientDidLogoff (event :ClientEvent) :void
    {
        super.clientDidLogoff(event);
        reconnectChannels(false);
    }

    // from parent superclass BasicDirector
    override public function clientDidLogon (event :ClientEvent) :void
    {
        super.clientDidLogon(event);
        reconnectChannels(true);
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
    override public function clearDisplays () :void
    {
        super.clearDisplays();
        _roomHistory.clear();
    }

    // from ChatDirector
    override public function messageReceived (event :MessageEvent) :void
    {
        // check if this is our custom chat message, unknown to the parent class
        if (ChatChannelCodes.CHAT_MESSAGE === event.getName()) {
            var msg :ChannelMessage = (event.getArgs()[0] as ChannelMessage);
            var localtype :String = getLocalType(event.getTargetOid());
            processReceivedMessage(msg, localtype);
        } else {
            // some other message - let the parent deal with it
            super.messageReceived(event);
        }
    }

    // from ChatDirector
    override protected function dispatchPreparedMessage (msg :ChatMessage) :void
    {
        // determine which channel to which this message is targeted
        var channel :ChatChannel = determineChannel(msg);
        if (channel != null) {
            var history :HistoryList = getHistory(channel);
            history.addMessage(msg);
            // if it's a message from a member, we open the UI even if it's not open
            if (channel.type == ChatChannel.MEMBER_CHANNEL) {
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
            super.dispatchPreparedMessage(msg);
        }
    }

    override protected function suppressTooManyCaps () :Boolean
    {
        return false;
    }

    /**
     * Iterates over all known channels, and either cleans up or reconnects them,
     * based on the input parameter.
     */
    protected function reconnectChannels (connected :Boolean) :void
    {
        for each (var chandler :ChannelHandler in _chandlers.values()) {
            if (connected) {
                // reconnect any open channels
                chandler.connect();
            } else {
                // we're already disconnected, so just clean up our local structures.
                // we'll fill them in again if we reconnect on a different server.
                chandler.disconnect();
            }
        }
    }
    
    /**
     * Create a ChatChannel object for the specified Name.
     */
    protected function makeChannel (name :Name) :ChatChannel
    {
        if (name is MemberName) {
            return ChatChannel.makeMemberChannel(name as MemberName);
        } else if (name is GroupName) {
            return ChatChannel.makeGroupChannel(name as GroupName);
        } else if (name is ChannelName) {
            return ChatChannel.makePrivateChannel(name as ChannelName);
        } else {
            log.warning("Requested to open unknown type of channel [name=" + name +
                        ", type=" + ClassUtil.getClassName(name) + "].");
            return null;
        }
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
            return ChatChannel.makeMemberChannel(umsg.getSpeakerDisplayName() as MemberName);
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

/**
 * The handler is a local object change dispatch for a channel. It can be active or shutdown:
 * while active, it can be connected or disconnected as the player moves between servers;
 * once shut down, it remains disconnected until destroyed. 
 */
class ChannelHandler implements Subscriber
{
    public var channel :ChatChannel;
    public var chanobj :ChatChannelObject;

    public function ChannelHandler (ctx :WorldContext, channel :ChatChannel, showTabFn :Function)
    {
        this.channel = channel;
        _ctx = ctx;
        _showTabFn = showTabFn;
        _ccsvc = (_ctx.getClient().requireService(ChatChannelService) as ChatChannelService);

        connect();
    }

    public function connect () :void
    {
        if (!_isConnected) {
            _ccsvc.joinChannel(_ctx.getClient(), channel, new ResultWrapper(failed, gotChannelOid));
        }
    }

    public function disconnect () :void
    {
        if (_isConnected) {
            if (_ccsub != null) {
                _ccsub.unsubscribe(_ctx.getClient().getDObjectManager());
                _ccsub = null;
            }
            if (chanobj != null) {
                _ctx.getChatDirector().removeAuxiliarySource(chanobj);
                chanobj = null;
            }
            _ccsvc.leaveChannel(_ctx.getClient(), channel);
            _isConnected = false;
        }
    }

    public function shutdown () :void
    {
        disconnect();
        _isShutdown = true;
    }

    // from Subscriber
    public function objectAvailable (obj :DObject) :void
    {
        chanobj = (obj as ChatChannelObject);
        _ctx.getChatDirector().addAuxiliarySource(chanobj, channel.toLocalType());
        _showTabFn(chanobj);
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
            _isConnected = true;
        }
    }

    protected function failed (cause :String) :void
    {
        var msg :String = MessageBundle.compose("m.join_channel_failed", cause);
        _ctx.displayFeedback(MsoyCodes.CHAT_MSGS, msg);
    }

    protected var _ctx :WorldContext;
    protected var _showTabFn :Function;
    protected var _isShutdown :Boolean = false;
    protected var _isConnected :Boolean = false;

    protected var _ccsvc :ChatChannelService;
    protected var _ccsub :SafeSubscriber;
}
