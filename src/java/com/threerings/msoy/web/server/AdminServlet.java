//
// $Id$

package com.threerings.msoy.web.server;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.logging.Level;

import com.samskivert.io.PersistenceException;
import com.samskivert.net.MailUtil;
import com.samskivert.util.ArrayIntSet;
import com.samskivert.util.ComplainingListener;
import com.samskivert.util.IntSet;

import com.threerings.msoy.data.MsoyAuthCodes;
import com.threerings.msoy.server.MsoyServer;
import com.threerings.msoy.server.ServerConfig;
import com.threerings.msoy.server.persist.MemberInviteStatusRecord;
import com.threerings.msoy.server.persist.MemberRecord;

import com.threerings.msoy.web.client.AdminService;
import com.threerings.msoy.web.data.MemberInviteResult;
import com.threerings.msoy.web.data.MemberInviteStatus;
import com.threerings.msoy.web.data.ServiceCodes;
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
            throw new ServiceException(ServiceCodes.E_INTERNAL_ERROR);
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
            throw new ServiceException(ServiceCodes.E_INTERNAL_ERROR);
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
            throw new ServiceException(ServiceCodes.E_INTERNAL_ERROR);
        }

        return res;
    }

    // from interface AdminService
    public int[] spamPlayers (WebIdent ident, String subject, String body, int startId, int endId)
        throws ServiceException
    {
        MemberRecord memrec = requireAuthedUser(ident);
        if (!memrec.isAdmin()) {
            throw new ServiceException(MsoyAuthCodes.ACCESS_DENIED);
        }

        log.info("Spamming the players [spammer=" + memrec.who() + ", subject=" + subject +
                 ", startId=" + startId + ", endId=" + endId + "].");

        // TODO: if we want to continue to use this mechanism to send mass emails to our members,
        // we will need to farm out the mail deliver task to all nodes in the network so that we
        // don't task one node with sending out a million email messages

        // start with member 1 if we weren't given a higher starting id
        startId = Math.max(startId, 1);

        // if we don't have an endId, go all the way
        if (endId <= 0) {
            endId = Integer.MAX_VALUE;
        }

        // we'll track the number of sent, failed and opted out accounts
        int[] results = new int[] { 0, 0, 0 };

        // loop through 100 members at a time and load up their record and send emails
        String from = ServerConfig.getFromAddress();
        int found;
        try {
            do {
                IntSet memIds = new ArrayIntSet();
                for (int ii = 0; ii < MEMBERS_PER_LOOP; ii++) {
                    int memberId = ii + startId;
                    if (memberId > endId) {
                        break;
                    }
                    memIds.add(memberId);
                }
                if (memIds.size() == 0) {
                    break;
                }

                found = 0;
                for (MemberRecord mrec : MsoyServer.memberRepo.loadMembers(memIds)) {
                    found++;

                    if (mrec.isSet(MemberRecord.Flag.NO_ANNOUNCE_EMAIL)) {
                        results[2]++;
                        continue;
                    }

                    try {
                        MailUtil.deliverMail(new String[] { mrec.accountName }, from, subject, body);
                        results[0]++;
                    } catch (Exception e) {
                        results[1]++;
                        log.warning("Failed to spam member [subject=" + subject +
                                    ", email=" + mrec.accountName + ", error=" + e + "].");
                        // roll on through and try the next one
                    }
                }

                startId += MEMBERS_PER_LOOP;
            } while (startId < endId && found > 0);

            return results;

        } catch (PersistenceException pe) {
            log.log(Level.WARNING, "spamPlayers failed [subject=" + subject +
                    ", startId=" + startId + "]", pe);
            throw new ServiceException(ServiceCodes.E_INTERNAL_ERROR);
        }
    }

    protected void sendGotInvitesMail (int senderId, int recipientId, int number)
        throws PersistenceException
    {
        String subject = MsoyServer.msgMan.getBundle("server").get("m.got_invites_subject", number);
        String body = MsoyServer.msgMan.getBundle("server").get("m.got_invites_body", number);
        MsoyServer.mailRepo.startConversation(recipientId, senderId, subject, body, null);
    }

    protected static final int MEMBERS_PER_LOOP = 100;
}
