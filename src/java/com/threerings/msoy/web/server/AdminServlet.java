//
// $Id$

package com.threerings.msoy.web.server;

import java.io.StringWriter;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.logging.Level;

import com.samskivert.io.PersistenceException;
import com.samskivert.net.MailUtil;
import com.samskivert.servlet.user.UserUtil;
import com.samskivert.util.ComplainingListener;
import com.samskivert.util.Invoker;
import com.samskivert.util.RandomUtil;
import com.samskivert.util.Tuple;
import com.samskivert.velocity.VelocityUtil;

import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.VelocityEngine;

import com.threerings.msoy.data.MsoyAuthCodes;
import com.threerings.msoy.server.MsoyAuthenticator;
import com.threerings.msoy.server.MsoyServer;
import com.threerings.msoy.server.ServerConfig;
import com.threerings.msoy.server.persist.MemberRecord;
import com.threerings.msoy.server.persist.MemberInviteStatusRecord;

import com.threerings.msoy.web.client.AdminService;
import com.threerings.msoy.web.data.ConnectConfig;
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
    public ConnectConfig loadConnectConfig (WebIdent ident)
        throws ServiceException
    {
        MemberRecord memrec = requireAuthedUser(ident);
        if (!memrec.isAdmin()) {
            throw new ServiceException(MsoyAuthCodes.ACCESS_DENIED);
        }

        ConnectConfig config = new ConnectConfig();
        config.server = ServerConfig.serverHost;
        config.port = ServerConfig.serverPorts[0];
        config.httpPort = ServerConfig.httpPort;
        return config;
    }

    // from interface AdminService
    public String[] registerAndInvite (WebIdent ident, String[] emails)
        throws ServiceException
    {
        MemberRecord memrec = requireAuthedUser(ident);
        if (!memrec.isSupport()) {
            throw new ServiceException(MsoyAuthCodes.ACCESS_DENIED);
        }

        VelocityEngine ve;
        try {
            ve = VelocityUtil.createEngine();
        } catch (Exception e) {
            log.log(Level.WARNING, "Failed to create velocity engine.", e);
            throw new ServiceException(ServiceException.INTERNAL_ERROR);
        }

        MsoyAuthenticator auth = (MsoyAuthenticator)MsoyServer.conmgr.getAuthenticator();
        String[] results = new String[emails.length];
        for (int ii = 0; ii < emails.length; ii++) {
            String email = emails[ii];
            if (!MailUtil.isValidAddress(email)) {
                results[ii] = "e.invalid_address";
                continue;
            }

            // create a new account for this person
            String password = createTempPassword();
            String displayName = email.substring(0, email.indexOf("@"));
            MemberRecord record;
            try {
                record = auth.createAccount(
                    email, UserUtil.encryptPassword(password), displayName, true, 0);
            } catch (ServiceException se) {
                results[ii] = se.getMessage();
                continue;
            }

            // now send them an invitation email
            VelocityContext ctx = new VelocityContext();
            ctx.put("username", email);
            ctx.put("password", password);
            StringWriter sw = new StringWriter();
            try {
                ve.mergeTemplate("rsrc/email/invite.tmpl", "UTF-8", ctx, sw);
                String body = sw.toString();
                int nidx = body.indexOf("\n"); // first line is the subject
                MailUtil.deliverMail(email, ServerConfig.getFromAddress(), body.substring(0, nidx),
                                     body.substring(nidx+1));
            } catch (Exception e) {
                results[ii] = e.getMessage();
            }
        }

        return results;
    }

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

    protected static String createTempPassword ()
    {
        StringBuilder builder = new StringBuilder();
        for (int ii = 0; ii < 12; ii++) {
            builder.append(PASSWORD_LETTERS.charAt(RandomUtil.getInt(PASSWORD_LETTERS.length())));
        }
        return builder.toString();
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

    protected static final String PASSWORD_LETTERS = "abcdefghijklmnopqrstuvwxyz0123456789";
}
