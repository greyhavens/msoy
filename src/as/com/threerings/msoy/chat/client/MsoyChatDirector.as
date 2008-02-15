//
// $Id$

package com.threerings.msoy.chat.client {

import mx.core.UIComponent;

import com.threerings.presents.client.ClientEvent;
import com.threerings.presents.client.InvocationAdapter;
import com.threerings.presents.client.ResultWrapper;
import com.threerings.presents.dobj.MessageEvent;
import com.threerings.util.ClassUtil;
import com.threerings.util.HashMap;
import com.threerings.util.Log;
import com.threerings.util.MessageBundle;
import com.threerings.util.Name;

import com.threerings.crowd.data.PlaceObject;

import com.threerings.crowd.chat.client.ChatDirector;
import com.threerings.crowd.chat.client.ChatDisplay;
import com.threerings.crowd.chat.client.SpeakService;
import com.threerings.crowd.chat.data.ChatCodes;
import com.threerings.crowd.chat.data.ChatMessage;
import com.threerings.crowd.chat.data.TellFeedbackMessage;
import com.threerings.crowd.chat.data.UserMessage;

import com.threerings.whirled.data.Scene;

import com.threerings.msoy.client.MsoyContext;
import com.threerings.msoy.data.MsoyCodes;
import com.threerings.msoy.data.all.ChannelName;
import com.threerings.msoy.data.all.GroupName;
import com.threerings.msoy.data.all.JabberName;
import com.threerings.msoy.data.all.MemberName;
import com.threerings.msoy.data.all.RoomName;

import com.threerings.msoy.chat.data.ChannelMessage;
import com.threerings.msoy.chat.data.ChatChannel;
import com.threerings.msoy.chat.data.ChatChannelCodes;
import com.threerings.msoy.chat.data.ChatChannelMarshaller;
import com.threerings.msoy.chat.data.ChatChannelObject;
import com.threerings.msoy.chat.data.JabberMarshaller;
import com.threerings.msoy.chat.client.JabberService;

import com.threerings.msoy.notify.data.NotifyMessage;

import com.threerings.msoy.world.client.WorldContext;

/**
 * Handles the dispatching of chat messages based on their "channel" (room/game, individual, or
 * actual custom channel). Manages chat history tracking for same.
 */
public class MsoyChatDirector extends ChatDirector
{
    public static const log :Log = Log.getLog(MsoyChatDirector);

    public function MsoyChatDirector (ctx :MsoyContext)
    {
        super(ctx, ctx.getMessageManager(), MsoyCodes.CHAT_MSGS);
        _wctx = ctx;

        // let the compiler know that these must be compiled into the client
        var c :Class = ChatChannelMarshaller;
        c = JabberMarshaller;

        var msg :MessageBundle = _msgmgr.getBundle(_bundle);
        registerCommandHandler(msg, "away", new AwayHandler());
    }

    /**
     * Whoever creates the chat tab bar is responsible for setting it here, so that we can properly
     * handle chat channels.
     */
    public function setChatTabs (tabs :ChatTabBar) :void
    {
        _chatTabs = tabs;
    }

    /**
     * Return true if we've already got a chat channel open with the specified Name.
     */
    public function hasOpenChannel (name :Name) :Boolean
    {
        return _chatTabs.containsTab(makeChannel(name));
    }

    /**
     * Opens the chat interface for the supplied player, group or private chat channel, selecting
     * the appropriate tab if said channel is already open.
     *
     * @param name either a MemberName, GroupName, ChannelName or RoomName
     */
    public function openChannel (name :Name, inFront :Boolean = false) :void
    {
        var channel :ChatChannel = makeChannel(name);

        // if this is a member or already open channel, open/select the UI immediately
        if (channel.type == ChatChannel.MEMBER_CHANNEL ||
            channel.type == ChatChannel.JABBER_CHANNEL ||
                _chandlers.containsKey(channel.toLocalType())) {
            _chatTabs.displayChat(channel, getHistory(channel), inFront);
            return;
        }

        // otherwise we have to subscribe to the channel first
        var displayChat :Boolean = true;
        var showTabFn :Function = function (ccobj :ChatChannelObject) :void {
            // once the subscription went through, show the chat history the first time.
            if (displayChat) {
                _chatTabs.displayChat(channel, getHistory(channel), inFront);
                displayChat = false;
            }
            // if this is a tabbed channel, make sure to update its distributed object reference
            _chatTabs.reinitController(channel, ccobj);
        };
        _chandlers.put(channel.toLocalType(), new ChannelHandler(_wctx, channel, showTabFn));
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
     * Displays the game chat sidebar.
     */
    public function displayGameChat (chatDtr :ChatDirector, playerList :UIComponent) :void
    {
        _wctx.getTopPanel().setRightPanel(new GameChatContainer(_wctx, chatDtr, playerList));
    }

    /**
     * Clears the game chat sidebar.
     */
    public function clearGameChat () :void
    {
        var gameChat :GameChatContainer = _wctx.getTopPanel().getRightPanel() as GameChatContainer;
        if (gameChat != null) {
            gameChat.shutdown();
            _wctx.getTopPanel().clearRightPanel();
        }
    }

    /**
     * Registers a user with an IM gateway and logs them in for chatting.
     */
    public function registerIM (gateway :String, username :String, password :String) :void
    {
        var svc :JabberService = (_wctx.getClient().requireService(JabberService) as JabberService);
        svc.registerIM(_wctx.getClient(), gateway, username, password, new InvocationAdapter(
            function (cause :String) :void {
                var msg :String = MessageBundle.compose(
                    "e.im_register_failed", "m." + gateway, cause);
                _wctx.displayFeedback(MsoyCodes.CHAT_MSGS, msg);
            }));
    }

    /**
     * Unregisters a user with an IM gateway, effectively logging them off.
     */
    public function unregisterIM (gateway :String) :void
    {
        var svc :JabberService = (_wctx.getClient().requireService(JabberService) as JabberService);
        svc.unregisterIM(_wctx.getClient(), gateway, new InvocationAdapter(
            function (cause :String) :void {
                var msg :String = MessageBundle.compose(
                    "e.im_unregister_failed", "m." + gateway, MessageBundle.taint(cause));
                _wctx.displayFeedback(MsoyCodes.CHAT_MSGS, msg);
            }));
    }


    /**
     * Requests that a tell message be delivered to the specified jabber user.
     */
    public function requestJabber (target :JabberName, msg :String) :void
    {
        var svc :JabberService = (_wctx.getClient().requireService(JabberService) as JabberService);
        svc.sendMessage(_wctx.getClient(), target, msg, new ResultWrapper(
            function (cause :String) :void {
            },
            function (result :Object) :void {
                dispatchMessage(new TellFeedbackMessage(target, msg), ChatCodes.PLACE_CHAT_TYPE);
            }));
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
            _wctx.getTopPanel().setActiveOverlay(display as ChatOverlay);
            _chatTabs.displayActiveChat(_roomHistory);

            if (display is ComicOverlay) {
                // show any waiting notifications
                displayNotifications(display as ComicOverlay);
            }
        }
        super.pushChatDisplay(display);
    }

    // from ChatDirector
    override public function addChatDisplay (display :ChatDisplay) :void
    {
        if (display is ChatOverlay) {
            _wctx.getTopPanel().setActiveOverlay(display as ChatOverlay);
            _chatTabs.displayActiveChat(_roomHistory);

            if (display is ComicOverlay) {
                // show any waiting notificatiosn
                displayNotifications(display as ComicOverlay);
            }
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
    override public function requestChat (speakSvc :SpeakService, text :String,
        record :Boolean) :String
    {
        if (speakSvc != null) {
            // this came from someone who knows what they want... pass on the request
            return super.requestChat(speakSvc, text, record);
        }

        var controller :ChatChannelController = _chatTabs.getCurrentController();
        if (controller == null) {
            // this really is going to the room chat.
            return super.requestChat(speakSvc, text, record);
        }

        // let the controller handle its own error reporting.
        controller.sendChat(text);
        // this prevents the ChatControl from reporting errors on its own.
        return ChatCodes.SUCCESS;
    }

    /**
     * Returns the chat history for the specified channel, creating it if necessary.
     */
    public function getHistory (channel :ChatChannel) :HistoryList
    {
        var history :HistoryList = (_histories.get(channel) as HistoryList);
        if (history == null) {
            _histories.put(channel, history = new HistoryList(channel.ident));
        }
        return history;
    }

    protected function displayNotifications (overlay :ComicOverlay) :void
    {
        while (_notifications.length > 0) {
            overlay.displayMessage((_notifications.shift() as NotifyMessage), false);
        }
    }

    // from ChatDirector
    override protected function dispatchPreparedMessage (msg :ChatMessage) :void
    {
        if (msg is NotifyMessage) {
            // notify messages don't go into the chat history, which is what happens when you
            // pass it off to the chat tabs
            var displayed :Boolean = false;
            _displays.apply(function (overlay :Object) :void {
                if (overlay is ComicOverlay &&
                    (overlay as ComicOverlay).displayMessage(msg, displayed)) {
                    displayed = true;
                }
            });
            if (!displayed) {
                _notifications.push(msg);
            }
        } else {
            _chatTabs.addMessage(determineChannel(msg), msg);

            if (getCurrentRoomChannel() != null &&
                getCurrentRoomChannel().equals(determineChannel(msg))) {
                super.dispatchPreparedMessage(msg);
            }
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
                if (_chatTabs == null || _chatTabs.shouldReconnectChannel(chandler.channel)) {
                    chandler.connect();
                }
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
        } else if (name is RoomName) {
            return ChatChannel.makeRoomChannel(name as RoomName);
        } else if (name is JabberName) {
            return ChatChannel.makeJabberChannel(name as JabberName);
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
            if (umsg.getSpeakerDisplayName() is JabberName) {
                return ChatChannel.makeJabberChannel(umsg.getSpeakerDisplayName() as JabberName);
            }
            return ChatChannel.makeMemberChannel(umsg.getSpeakerDisplayName() as MemberName);
        }
        var handler :ChannelHandler = _chandlers.get(msg.localtype) as ChannelHandler;
        if (handler != null) {
            return handler.channel;
        }
        return null;
    }

    protected function getCurrentRoomChannel () :ChatChannel
    {
        if (!(_wctx is WorldContext)) {
            return null;
        }

        var scene :Scene = (_wctx as WorldContext).getSceneDirector().getScene();
        if (scene == null) {
            return null;
        }

        return ChatChannel.makeRoomChannel(new RoomName(scene.getName(), scene.getId()));
    }

    protected var _wctx :MsoyContext;
    protected var _chatTabs :ChatTabBar;

    /** Contains a mapping from chat localtype to channel handler. */
    protected var _chandlers :HashMap = new HashMap();

    protected var _roomHistory :HistoryList = new HistoryList();
    protected var _histories :HashMap = new HashMap();

    protected var _notifications :Array = [];
}
}

import com.threerings.util.Log;
import com.threerings.util.MessageBundle;

import com.threerings.presents.client.ResultWrapper;
import com.threerings.presents.dobj.DObject;
import com.threerings.presents.dobj.ObjectAccessError;
import com.threerings.presents.dobj.Subscriber;
import com.threerings.presents.util.SafeSubscriber;

import com.threerings.msoy.client.MsoyContext;
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

    public function ChannelHandler (ctx :MsoyContext, channel :ChatChannel, showTabFn :Function)
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

    private static const log :Log = Log.getLog(ChannelHandler);

    protected var _ctx :MsoyContext;
    protected var _showTabFn :Function;
    protected var _isShutdown :Boolean = false;
    protected var _isConnected :Boolean = false;

    protected var _ccsvc :ChatChannelService;
    protected var _ccsub :SafeSubscriber;
}
