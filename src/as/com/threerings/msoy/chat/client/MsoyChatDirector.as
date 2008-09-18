//
// $Id$

package com.threerings.msoy.chat.client {

import mx.core.UIComponent;

import mx.events.CloseEvent;

import com.threerings.util.ClassUtil;
import com.threerings.util.Log;
import com.threerings.util.MessageBundle;
import com.threerings.util.Name;
import com.threerings.util.StringUtil;

import com.threerings.presents.client.Client;
import com.threerings.presents.client.ClientEvent;
import com.threerings.presents.client.InvocationAdapter;
import com.threerings.presents.client.ResultAdapter;
import com.threerings.presents.dobj.MessageEvent;

import com.threerings.crowd.data.PlaceObject;

import com.threerings.crowd.chat.client.ChannelSpeakService;
import com.threerings.crowd.chat.client.ChatDirector;
import com.threerings.crowd.chat.client.ChatDisplay;
import com.threerings.crowd.chat.client.SpeakService;
import com.threerings.crowd.chat.data.ChannelSpeakMarshaller;
import com.threerings.crowd.chat.data.ChatChannel;
import com.threerings.crowd.chat.data.ChatCodes;
import com.threerings.crowd.chat.data.ChatMessage;
import com.threerings.crowd.chat.data.SystemMessage;
import com.threerings.crowd.chat.data.TellFeedbackMessage;
import com.threerings.crowd.chat.data.UserMessage;

import com.whirled.ui.PlayerList;

import com.threerings.msoy.client.ControlBar;
import com.threerings.msoy.client.DeploymentConfig;
import com.threerings.msoy.client.Msgs;
import com.threerings.msoy.client.MsoyContext;
import com.threerings.msoy.client.MsoyController;
import com.threerings.msoy.data.MsoyCodes;
import com.threerings.msoy.data.all.ChannelName;
import com.threerings.msoy.data.all.GroupName;
import com.threerings.msoy.data.all.JabberName;
import com.threerings.msoy.data.all.MemberName;
import com.threerings.msoy.data.all.RoomName;

import com.threerings.msoy.chat.data.MsoyChatChannel;
import com.threerings.msoy.chat.data.JabberMarshaller;
import com.threerings.msoy.chat.client.JabberService;

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
        _mctx = ctx;

        // let the compiler know that these must be compiled into the client
        var c :Class = JabberMarshaller;
        c = ChannelSpeakMarshaller;

        var msg :MessageBundle = _msgmgr.getBundle(_bundle);
        registerCommandHandler(msg, "away", new AwayHandler(true));
        registerCommandHandler(msg, "back", new AwayHandler(false));
        if (DeploymentConfig.devDeployment) {
            registerCommandHandler(msg, "badges", new BadgesHandler());
        }

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
     * @param name either a MemberName, GroupName, ChannelName or RoomName.
     */
    public function openChannel (name :Name) :void
    {
        _chatTabs.openChannelTab(makeChannel(name), true);
    }

    /**
     * Called when the user closes the tab associated with a particular chat channel.
     */
    public function tabClosed (localtype :String) :void
    {
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
        var svc :JabberService = (_mctx.getClient().requireService(JabberService) as JabberService);
        svc.registerIM(_mctx.getClient(), gateway, username, password, new InvocationAdapter(
            function (cause :String) :void {
                var msg :String = MessageBundle.compose(
                    "e.im_register_failed", "m." + gateway, cause);
                _mctx.displayFeedback(MsoyCodes.CHAT_MSGS, msg);
            }));
    }

    /**
     * Unregisters a user with an IM gateway, effectively logging them off.
     */
    public function unregisterIM (gateway :String) :void
    {
        var svc :JabberService = (_mctx.getClient().requireService(JabberService) as JabberService);
        svc.unregisterIM(_mctx.getClient(), gateway, new InvocationAdapter(
            function (cause :String) :void {
                var msg :String = MessageBundle.compose(
                    "e.im_unregister_failed", "m." + gateway, MessageBundle.taint(cause));
                _mctx.displayFeedback(MsoyCodes.CHAT_MSGS, msg);
            }));
    }


    /**
     * Requests that a tell message be delivered to the specified jabber user.
     */
    public function requestJabber (target :JabberName, msg :String, feedbackLocaltype :String) :void
    {
        _jservice.sendMessage(_mctx.getClient(), target, msg, new ResultAdapter(
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

    /**
     * Requests that a message be delivered to the specified channel.
     */
    public function requestChannelSpeak (channel :MsoyChatChannel, msg :String) :void
    {
        _csservice.speak(_mctx.getClient(), channel, msg, 0); // TODO: mode?
    }

    public function getPlayerList (ltype :String) :PlayerList
    {
        return (ltype == ChatCodes.PLACE_CHAT_TYPE) ? _roomOccList : null;
    }

    // from ChatDirector
    override public function requestChat (
        speakSvc :SpeakService, message :String, record :Boolean) :String
    {
        if (speakSvc != null) {
            // this came from someone who knows what they want... pass on the request
            return super.requestChat(speakSvc, message, record);
        }

        var channel :MsoyChatChannel = _chatTabs.getCurrentChannel();
        if (channel == null || channel.type == MsoyChatChannel.ROOM_CHANNEL) {
            // this is place chat, then we don't need anything special
            return super.requestChat(speakSvc, message, record);

        } else if (channel.type == MsoyChatChannel.MEMBER_CHANNEL) {
            requestTell(channel.ident as Name, message, null, channel.toLocalType());

        } else if (channel.type == MsoyChatChannel.JABBER_CHANNEL) {
            requestJabber(channel.ident as JabberName, message, channel.toLocalType());

        } else {
            requestChannelSpeak(channel, message);
        }
        return ChatCodes.SUCCESS;
    }

    // from ChatDirector
    override public function dispatchMessage (message :ChatMessage, localtype :String) :void
    {
        if ((message is UserMessage && localtype == ChatCodes.USER_CHAT_TYPE) ||
            message is TellFeedbackMessage) {
            // use a more specific localtype
            var member :MemberName = (message as UserMessage).getSpeakerDisplayName() as MemberName;
            localtype = MsoyChatChannel.makeMemberChannel(member).toLocalType();
        }

        super.dispatchMessage(message, localtype);
    }

    // from ChatDirector
    override public function locationDidChange (place :PlaceObject) :void
    {
        super.locationDidChange(place);

        // clear out our old room occupant list
        if (_roomOccList != null) {
            _roomOccList.shutdown();
            _roomOccList = null;
        }
        // create a new one if appropriate
        if (place != null) {
            _roomOccList = new RoomOccupantList(_mctx, place);
        }
    }

    // from ChatDirector
    override protected function suppressTooManyCaps () :Boolean
    {
        return false;
    }

    // from ChatDirector
    override protected function clearChatOnClientExit () :Boolean
    {
        return false; // TODO: we need this because on msoy we "exit" when change servers
    }

    // from ChatDirector
    override protected function getChannelLocalType (channel :ChatChannel) :String
    {
        var mchannel :MsoyChatChannel = (channel as MsoyChatChannel);
        // this is called by the ChatDirector when a message arrives, we sneak in here and make
        // sure we have a chat channel tab available for this channel type so that when the chat
        // director goes to dispatch the message, we're all ready to go; if the tab already exists,
        // openChannelTab basically NOOPs
        _chatTabs.openChannelTab(mchannel, false);
        return mchannel.toLocalType();
    }

    // from ChatDirector
    override protected function fetchServices (client :Client) :void
    {
        super.fetchServices(client);
        _csservice = (client.requireService(ChannelSpeakService) as ChannelSpeakService);
        _jservice = (client.requireService(JabberService) as JabberService);
    }

    /**
     * Create a ChatChannel object for the specified Name.
     */
    protected function makeChannel (name :Name) :MsoyChatChannel
    {
        if (name is MemberName) {
            return MsoyChatChannel.makeMemberChannel(name as MemberName);
        } else if (name is GroupName) {
            return MsoyChatChannel.makeGroupChannel(name as GroupName);
        } else if (name is ChannelName) {
            return MsoyChatChannel.makePrivateChannel(name as ChannelName);
        } else if (name is RoomName) {
            return MsoyChatChannel.makeRoomChannel(name as RoomName);
        } else if (name is JabberName) {
            return MsoyChatChannel.makeJabberChannel(name as JabberName);
        } else {
            log.warning("Requested to create unknown type of channel [name=" + name +
                        ", type=" + ClassUtil.getClassName(name) + "].");
            return null;
        }
    }

    protected var _mctx :MsoyContext;
    protected var _chatTabs :ChatTabBar;
    protected var _chatHistory :HistoryList;
    protected var _roomOccList :RoomOccupantList;

    protected var _csservice :ChannelSpeakService;
    protected var _jservice :JabberService;
}
}
