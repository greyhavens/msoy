//
// $Id$

package com.threerings.msoy.chat.server;

import com.threerings.crowd.chat.data.ChatCodes;
import com.threerings.crowd.chat.server.SpeakProvider;
import com.threerings.crowd.chat.server.SpeakUtil;
import com.threerings.msoy.chat.data.ChatterInfo;
import com.threerings.msoy.data.MemberObject;
import com.threerings.msoy.peer.client.PeerChatService;
import com.threerings.presents.data.ClientObject;
import com.threerings.util.MessageManager;

import static com.threerings.msoy.Log.log;

public abstract class ChannelSpeakHandler
    implements SpeakProvider
{
    public ChannelSpeakHandler (ChannelWrapper wrapper)
    {
        _ch = wrapper;
    }

    public abstract void speak (ClientObject caller, String message, byte mode);

    /**
     * Ensures that the chat object is initialized, and the speaker has access to it.
     */
    protected boolean validateSpeaker (ClientObject speaker, byte mode)
    {
        // if it's not a member object, don't even bother
        if (! (speaker instanceof MemberObject)) {
            return false;
        }

        // does the speaker have the rights for this action?
        MemberObject member = (MemberObject)speaker;
        String errmsg = member.checkAccess(ChatCodes.CHAT_ACCESS, null);
        if (errmsg != null) {
            SpeakUtil.sendFeedback(member, MessageManager.GLOBAL_BUNDLE, errmsg);
            return false;
        }

        // is the speaker a member of this channel?
        if (!_ch.hasMember(member)) {
            log.warning("Refusing to send channel chat from a non-member [caller=" + member.who() +
                        ", channel=" + _ch.getChannel() + "].");
            return false;
        }

        // is the speech mode supported by this channel?
        if (mode == ChatCodes.BROADCAST_MODE) {
            log.warning("Refusing speak request with invalid mode [caller=" + member.who() +
                        ", channel=" + _ch.getChannel() + ", mode=" + mode + "].");
            return false;
        }            

        // the speaker passed the gauntlet.
        return true;
    }

    /**
     * No-op, error-reporting listener
     */
    protected class ReportListener implements PeerChatService.ConfirmListener
    {
        public ReportListener (ChatterInfo userInfo) {
            _userInfo = userInfo;
        }
        public void requestProcessed () {
            // nothing to do
        }
        public void requestFailed (String cause) {
            log.info("Subscription channel: channel action failed [channel=" + _ch.getChannel() +
                     ", user=" + _userInfo.name + ", cause = " + cause + "].");
        }
        protected ChatterInfo _userInfo;
    };

    /** Reference to our channel wrapper. */
    protected ChannelWrapper _ch;
}
