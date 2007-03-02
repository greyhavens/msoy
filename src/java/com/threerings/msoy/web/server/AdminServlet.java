//
// $Id$

package com.threerings.msoy.web.server;

import java.io.StringWriter;
import java.util.HashMap;
import java.util.logging.Level;

import com.samskivert.net.MailUtil;
import com.samskivert.util.Invoker;
import com.samskivert.util.Tuple;
import com.samskivert.velocity.VelocityUtil;

import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.VelocityEngine;

import com.threerings.msoy.data.MsoyAuthCodes;
import com.threerings.msoy.server.MsoyAuthenticator;
import com.threerings.msoy.server.MsoyServer;
import com.threerings.msoy.server.ServerConfig;
import com.threerings.msoy.server.persist.MemberRecord;

import com.threerings.msoy.web.client.AdminService;
import com.threerings.msoy.web.data.ConnectConfig;
import com.threerings.msoy.web.data.ServiceException;
import com.threerings.msoy.web.data.WebCreds;

import static com.threerings.msoy.Log.log;

/**
 * Provides the server implementation of {@link AdminService}.
 */
public class AdminServlet extends MsoyServiceServlet
    implements AdminService
{
    // from interface AdminService
    public ConnectConfig loadConnectConfig (WebCreds creds)
        throws ServiceException
    {
        MemberRecord memrec = requireAuthedUser(creds);
        if (!memrec.isAdmin()) {
            throw new ServiceException(MsoyAuthCodes.ACCESS_DENIED);
        }

        ConnectConfig config = new ConnectConfig();
        config.server = ServerConfig.serverHost;
        config.port = ServerConfig.serverPorts[0];
        return config;
    }

    // from interface AdminService
    public String[] registerAndInvite (WebCreds creds, String[] emails)
        throws ServiceException
    {
        MemberRecord memrec = requireAuthedUser(creds);
        if (!memrec.isAdmin()) {
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
                record = auth.createAccount(email, password, displayName);
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
                MailUtil.deliverMail(email, INVITE_FROM, body.substring(0, nidx),
                                     body.substring(nidx+1));
            } catch (Exception e) {
                results[ii] = e.getMessage();
            }
        }

        return results;
    }

    protected String createTempPassword ()
    {
        return "secret";
    }

    protected static final String INVITE_FROM = "peas@whirled.com";
}
