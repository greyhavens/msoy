//
// $Id$

package com.threerings.msoy.server.util;

import java.io.StringWriter;
import java.util.logging.Level;

import com.samskivert.net.MailUtil;
import com.samskivert.util.Invoker;
import com.samskivert.velocity.VelocityUtil;

import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.VelocityEngine;

import com.threerings.msoy.server.ServerConfig;
import com.threerings.msoy.web.data.ServiceException;

import static com.threerings.msoy.Log.log;

/**
 * Utility methods for sending email, using the mail invoker thread if appropriate.
 */
public class MailSender
{
    /**
     * Use this to send email on the {@link MsoyServer#mailInvoker}.
     */
    public static abstract class Unit extends Invoker.Unit
    {
        public Unit (String recipient, String template)
        {
            this(recipient, ServerConfig.getFromAddress(), template);
        }

        public Unit (String recipient, String sender, String template)
        {
            super("MailSender.Unit(" + recipient + ", " + template + ")");
            _recipient = recipient;
            _sender = sender;
            _template = template;
        }

        public boolean invoke () {
            try {
                VelocityContext ctx = new VelocityContext();
                populateContext(ctx);
                sendEmail(_recipient, _sender, _template, ctx);
            } catch (Exception e) {
                log.log(Level.WARNING, "Failed to send mail [sender=" + _sender +
                        ", recip=" + _recipient + ", template=" + _template + "].", e);
            }
            return false;
        }

        protected abstract void populateContext (VelocityContext ctx);

        protected String _recipient, _sender, _template;
    }

    /**
     * Delivers an email using the supplied template and context. The first line of the template
     * should be the subject and the remaining lines the body.
     *
     * @return null or a string indicating the problem in the event of failure.
     */
    public static String sendEmail (String recip, String sender, String template,
                                    VelocityContext ctx)
    {
        VelocityEngine ve;
        try {
            ve = VelocityUtil.createEngine();
        } catch (Exception e) {
            log.log(Level.WARNING, "Failed to create the velocity engine.", e);
            return ServiceException.INTERNAL_ERROR;
        }

        StringWriter sw = new StringWriter();
        try {
            // TODO: have a server language and select templates based on that
            ve.mergeTemplate("rsrc/email/" + template + ".tmpl", "UTF-8", ctx, sw);
            String body = sw.toString();
            int nidx = body.indexOf("\n"); // first line is the subject
            MailUtil.deliverMail(recip, sender, body.substring(0, nidx), body.substring(nidx+1));
            return null;

        } catch (Exception e) {
            return e.getMessage();
        }
    }
}
