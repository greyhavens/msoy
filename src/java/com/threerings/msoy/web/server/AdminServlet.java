//
// $Id$

package com.threerings.msoy.web.server;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.logging.Level;

import com.samskivert.io.PersistenceException;
import com.samskivert.util.ComplainingListener;

import com.threerings.msoy.data.MsoyAuthCodes;
import com.threerings.msoy.server.MsoyServer;
import com.threerings.msoy.server.persist.MemberRecord;
import com.threerings.msoy.server.persist.MemberInviteStatusRecord;

import com.threerings.msoy.web.client.AdminService;
import com.threerings.msoy.web.data.MemberInviteResult;
import com.threerings.msoy.web.data.MemberInviteStatus;
import com.threerings.msoy.web.data.ServiceException;
import com.threerings.msoy.web.data.WebIdent;

import static com.threerings.msoy.Log.log;

/**
 * Provides the server implementation of {@link AdminService}.
 */
public class AdminServlet extends MsoyServiceServlet
    implements AdminService
{
    // from interface AdminService
    public void grantInvitations (WebIdent ident, int numberInvitations, Date activeSince)
        throws ServiceException
    {
        MemberRecord memrec = requireAuthedUser(ident);
        if (!memrec.isAdmin()) {
            throw new ServiceException(MsoyAuthCodes.ACCESS_DENIED);
        }

        try {
            Timestamp since = activeSince != null ? new Timestamp(activeSince.getTime()) : null;
            for (int memberId : MsoyServer.memberRepo.grantInvites(numberInvitations, since)) {
                sendGotInvitesMail(memrec.memberId, memberId, numberInvitations);
            }

        } catch (PersistenceException pe) {
            log.log(Level.WARNING, "grantInvitations failed [num=" + numberInvitations + 
                ", activeSince=" + activeSince + "]", pe);
            throw new ServiceException(pe.getMessage());
        }
    }

    // from interface AdminService
    public void grantInvitations (WebIdent ident, int numberInvitations, int memberId)
        throws ServiceException
    {
        MemberRecord memrec = requireAuthedUser(ident);
        if (!memrec.isAdmin()) {
            throw new ServiceException(MsoyAuthCodes.ACCESS_DENIED);
        }

        try {
            MsoyServer.memberRepo.grantInvites(memberId, numberInvitations);
            sendGotInvitesMail(memrec.memberId, memberId, numberInvitations);

        } catch (PersistenceException pe) {
            log.log(Level.WARNING, "grantInvitations failed [num=" + numberInvitations +
                ", memberId=" + memberId + "]", pe);
            throw new ServiceException(pe.getMessage());
        }
    }

    // from interface AdminService
    public MemberInviteResult getPlayerList (WebIdent ident, int inviterId)
        throws ServiceException
    {
        MemberRecord memrec = requireAuthedUser(ident);
        if (!memrec.isAdmin()) {
            throw new ServiceException(MsoyAuthCodes.ACCESS_DENIED);
        }

        MemberInviteResult res = new MemberInviteResult();
        try {
            MemberRecord memRec = inviterId == 0 ? null : 
                MsoyServer.memberRepo.loadMember(inviterId);
            if (memRec != null) {
                res.name = memRec.permaName == null || memRec.permaName.equals("") ?
                    memRec.name : memRec.permaName;
                res.memberId = inviterId;
                res.invitingFriendId = memRec.invitingFriendId;
            }

            List<MemberInviteStatus> players = new ArrayList<MemberInviteStatus>();
            for (MemberInviteStatusRecord rec : 
                    MsoyServer.memberRepo.getMembersInvitedBy(inviterId)) {
                players.add(rec.toWebObject());
            }
            res.invitees = players;

        } catch (PersistenceException pe) {
            log.log(Level.WARNING, "getPlayerList failed [inviterId=" + inviterId + "]", pe);
            throw new ServiceException(pe.getMessage());
        }

        return res;
    }

    protected void sendGotInvitesMail (int senderId, int recipientId, int number)
    {
        String subject = MsoyServer.msgMan.getBundle("server").get("m.got_invites_subject", number);
        String body = MsoyServer.msgMan.getBundle("server").get("m.got_invites_body", number);
        MsoyServer.mailMan.deliverMessage(
            senderId, recipientId, subject, body, null,
            new ComplainingListener<Void>(log, "Send got invites mail failed [sid=" + senderId +
                                          ", rid=" + recipientId + "]"));
    }
}
