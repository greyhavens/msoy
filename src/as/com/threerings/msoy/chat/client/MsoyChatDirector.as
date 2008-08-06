//
// $Id$

package com.threerings.msoy.chat.client {

import mx.core.UIComponent;

import mx.events.CloseEvent;

import com.threerings.presents.client.ClientEvent;
import com.threerings.presents.client.InvocationAdapter;
import com.threerings.presents.client.ResultWrapper;
import com.threerings.presents.dobj.MessageEvent;

import com.whirled.ui.PlayerList;

import com.threerings.util.ClassUtil;
import com.threerings.util.HashMap;
import com.threerings.util.Log;
import com.threerings.util.MessageBundle;
import com.threerings.util.Name;
import com.threerings.util.StringUtil;

import com.threerings.crowd.data.PlaceObject;

import com.threerings.crowd.chat.client.ChatDirector;
import com.threerings.crowd.chat.client.ChatDisplay;
import com.threerings.crowd.chat.client.SpeakService;

import com.threerings.crowd.chat.data.ChatCodes;
import com.threerings.crowd.chat.data.ChatMessage;
import com.threerings.crowd.chat.data.SystemMessage;
import com.threerings.crowd.chat.data.TellFeedbackMessage;
import com.threerings.crowd.chat.data.UserMessage;

import com.threerings.whirled.data.Scene;

import com.threerings.msoy.client.ControlBar;
import com.threerings.msoy.client.Msgs;
import com.threerings.msoy.client.MsoyContext;
import com.threerings.msoy.client.MsoyController;
import com.threerings.msoy.data.MsoyCodes;
import com.threerings.msoy.data.all.ChannelName;
import com.threerings.msoy.data.all.GroupName;
import com.threerings.msoy.data.all.JabberName;
import com.threerings.msoy.data.all.MemberName;
import com.threerings.msoy.data.all.RoomName;

import com.threerings.msoy.chat.data.ChannelMessage;
import com.threerings.msoy.chat.data.ChatChannel;
import com.threerings.msoy.chat.data.ChatChannelMarshaller;
import com.threerings.msoy.chat.data.ChatChannelObject;
import com.threerings.msoy.chat.data.JabberMarshaller;
import com.threerings.msoy.chat.client.JabberService;

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
        registerCommandHandler(msg, "away", new AwayHandler(true));
        registerCommandHandler(msg, "back", new AwayHandler(false));

        addChatDisplay(_chatHistory = new HistoryList());
    }

    /**
     * Whoever creates the chat tab bar is responsible for setting it here, so that we can properly
     * handle chat channels.
     */
    public function setChatTabs (tabs :ChatTabBar) :void
    {
        _chatTabs = tabs;
        addChatDisplay(_chatTabs);
    }

    /** 
     * Retrieve the global chat history that contains every message received on this client, 
     * within a history list size limit.
     */
    public function getHistoryList () :HistoryList
    {
        return _chatHistory;
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
                _channelControllers.containsKey(channel.toLocalType())) {
            _chatTabs.selectChannelTab(channel, inFront);
            return;
        }

        // otherwise we have to subscribe to the channel first
        var displayChat :Boolean = true;
        var showTabFn :Function = function () :void {
            // once the subscription went through, show the chat history the first time.
            if (displayChat) {
                _chatTabs.selectChannelTab(channel, inFront);
                displayChat = false;
            }
        };
        _channelControllers.put(channel.toLocalType(), 
            new ChatChannelController(_wctx, channel, showTabFn));
    }

    /**
     * Called when the user closes the tab associated with a particular chat channel.
     */
    public function tabClosed (localtype :String) :void
    {
        var controller :ChatChannelController = 
            _channelControllers.remove(localtype) as ChatChannelController;
        if (controller != null) {
            controller.shutdown();
        }

        _displays.apply(function (disp :ChatDisplay) :void {
            if (disp is TabbedChatDisplay) {
                (disp as TabbedChatDisplay).tabClosed(localtype);
            }
        });
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
    public function requestJabber (target :JabberName, msg :String, feedbackLocaltype :String) :void
    {
        var svc :JabberService = (_wctx.getClient().requireService(JabberService) as JabberService);
        svc.sendMessage(_wctx.getClient(), target, msg, new ResultWrapper(
            function (cause :String) :void {
                var msg :String = MessageBundle.compose(
                    "e.im_tell_failed", MessageBundle.taint(cause));
                displaySystem(MsoyCodes.CHAT_MSGS, msg, SystemMessage.FEEDBACK, feedbackLocaltype);
            },
            function (result :Object) :void {
                if (result != null && result is String) {
                    displaySystem(MsoyCodes.CHAT_MSGS, (result as String), SystemMessage.FEEDBACK,
                                  feedbackLocaltype);
                }
                dispatchMessage(new TellFeedbackMessage(target, msg), feedbackLocaltype);
            }));
    }

    public function getPlayerList (localtype :String) :PlayerList
    {
        var controller :ChatChannelController = 
            _channelControllers.get(localtype) as ChatChannelController;
        return controller == null ? null : controller.occupantList;
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
    override public function requestChat (speakSvc :SpeakService, text :String,
        record :Boolean) :String
    {
        if (speakSvc != null) {
            // this came from someone who knows what they want... pass on the request
            return super.requestChat(speakSvc, text, record);
        }

        var localtype :String = _chatTabs.getCurrentLocalType();
        var controller :ChatChannelController = _channelControllers.get(localtype);
    
        if (controller == null) {
            var channeltype :int = ChatChannel.typeOf(localtype);
            var channel :ChatChannel
            if (channeltype == ChatChannel.MEMBER_CHANNEL) {
                try {
                    channel = ChatChannel.makeMemberChannel(new MemberName(
                        _chatTabs.getName(localtype), 
                        StringUtil.parseInteger(ChatChannel.infoOf(localtype))));
                } catch (err :ArgumentError) {
                    // NOOP
                }
            } else if (channeltype == ChatChannel.JABBER_CHANNEL) {
                channel = ChatChannel.makeJabberChannel(new JabberName(
                    ChatChannel.infoOf(localtype),
                    _chatTabs.getName(localtype)));
            }

            if (channel != null) {
                controller = new ChatChannelController(_wctx, channel);
                _channelControllers.put(localtype, controller);
            }
        }

        if (controller == null) {
            // this really is going to the room chat.
            return super.requestChat(speakSvc, text, record);
        }

        // let the controller format the message as necessary, and handle its own error reporting.
        controller.sendChat(text);
        // this prevents the ChatControl from reporting errors on its own.
        return ChatCodes.SUCCESS;
    }

    override public function dispatchMessage (message :ChatMessage, localtype :String) :void
    {
        if ((message is UserMessage && localtype == ChatCodes.USER_CHAT_TYPE) || 
            message is TellFeedbackMessage) {
            // use a more specific localtype
            var member :MemberName = (message as UserMessage).getSpeakerDisplayName() as MemberName;
            localtype = ChatChannel.makeMemberChannel(member).toLocalType();
        }

        super.dispatchMessage(message, localtype);
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
        for each (var controller :ChatChannelController in _channelControllers.values()) {
            if (connected &&
                (_chatTabs == null || _chatTabs.shouldReconnectChannel(controller.channel))) {
                // reconnect any open channels
                controller.connect();
            } else {
                // we're already disconnected, so just clean up our local structures.
                // we'll fill them in again if we reconnect on a different server.
                controller.disconnect();
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
            log.warning("Requested to create unknown type of channel [name=" + name +
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
        var controller :ChatChannelController = 
            _channelControllers.get(msg.localtype) as ChatChannelController;
        if (controller != null) {
            return controller.channel;
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

    /** Contains a mapping from chat localtype to channel controller. */
    protected var _channelControllers :HashMap = new HashMap();

    protected var _chatHistory :HistoryList;
}
}
