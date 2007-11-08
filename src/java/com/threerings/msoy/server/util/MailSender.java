//
// $Id$

package com.threerings.msoy.server.util;

import java.io.File;
import java.io.StringWriter;
import java.util.logging.Level;
import java.util.regex.Pattern;

import javax.activation.DataHandler;
import javax.activation.FileDataSource;
import javax.activation.FileTypeMap;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;

import com.samskivert.net.MailUtil;
import com.samskivert.util.Invoker;
import com.samskivert.velocity.VelocityUtil;

import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.VelocityEngine;

import com.threerings.msoy.server.MsoyBaseServer;
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
     * Returns true if the supplied address is not actually a valid address but rather a
     * placeholder to which we should not send email. Because we use email address for our
     * authentication username, we sometimes have to generate placeholder addresses for accounts
     * created on behest of a partner who does not provide the user's actual email address.
     */
    public static boolean isPlaceholderAddress (String address)
    {
        for (Pattern pattern : PLACEHOLDER_PATTERNS) {
            if (pattern.matcher(address).matches()) {
                return true;
            }
        }
        return false;
    }

    /**
     * Delivers an email using the supplied template and context. The first line of the template
     * should be the subject and the remaining lines the body.
     *
     * @return null or a string indicating the problem in the event of failure.
     */
    public static String sendEmail (
        String recip, String sender, String template, VelocityContext ctx)
    {
        MsoyBaseServer.refuseDObjThread(); // avoid unhappy accidents

        // skip emails to placeholder addresses
        if (isPlaceholderAddress(recip)) {
            return null; // feign success
        }

        // create a velocity engine that we'll use to merge text into templates
        VelocityEngine ve;
        try {
            ve = VelocityUtil.createEngine();
        } catch (Exception e) {
            log.log(Level.WARNING, "Failed to create the velocity engine.", e);
            return ServiceException.INTERNAL_ERROR;
        }

        // create a mime message which will contain text and possibly HTML parts
        MimeMultipart parts = new MimeMultipart("alternative");

        StringWriter sw = new StringWriter();
        try {
            // TODO: have a server language and select templates based on that
            ve.mergeTemplate("rsrc/email/" + template + ".tmpl", "UTF-8", ctx, sw);

            String body = sw.toString();
            int nidx = body.indexOf("\n"); // first line is the subject
            String subject = body.substring(0, nidx);
            body = body.substring(nidx+1);
            MimeBodyPart textPart = new MimeBodyPart();
            textPart.setContent(body, "text/plain");
            parts.addBodyPart(textPart);

            // if there's a directory with the same name as the template, it's our HTML message
            // (and optional images), so put that all together
            File htmlDir = new File(ServerConfig.serverRoot, "rsrc/email/" + template);
            if (htmlDir.isDirectory()) {
                MimeMultipart htmlParts = new MimeMultipart("related");

                sw = new StringWriter();
                ve.mergeTemplate("rsrc/email/" + template + "/message.html", "UTF-8", ctx, sw);
                MimeBodyPart htmlPart = new MimeBodyPart();
                htmlPart.setContent(sw.toString(), "text/html");
                htmlParts.addBodyPart(htmlPart);

                // now add any images in the same directory
                for (File file : htmlDir.listFiles()) {
                    // we only add png and jpg images
                    if (!file.getName().endsWith(".png") && !file.getName().endsWith(".jpg")) {
                        continue;
                    }

                    MimeBodyPart ipart = new MimeBodyPart();
                    FileDataSource source = new FileDataSource(file);
                    source.setFileTypeMap(FT_MAP);
                    ipart.setDataHandler(new DataHandler(source));
                    ipart.setFileName(file.getName());
                    ipart.setContentID("<" + file.getName() + ">");
                    htmlParts.addBodyPart(ipart);
                }

                MimeBodyPart alternativePart = new MimeBodyPart();
                alternativePart.setContent(htmlParts);
                parts.addBodyPart(alternativePart);
            }

            // finally send that message off to the lucky recipient
            MimeMessage message = MailUtil.createEmptyMessage();
            message.setContent(parts);
            MailUtil.deliverMail(new String[] { recip }, sender, subject, message);
            return null;

        } catch (Exception e) {
            return e.getMessage();
        }
    }

    /** Used to map files to mimem types. */
    protected static final FileTypeMap FT_MAP = new FileTypeMap() {
        public String getContentType (File file) {
            return getContentType(file.getName());
        }
        public String getContentType (String name) {
            name = name.toLowerCase();
            if (name.endsWith(".png")) {
                return "image/png";
            } else if (name.endsWith(".jpg")) {
                return "image/jpeg";
            } else {
                return "application/octet-stream";
            }
        }
    };

    /** Used by {@link #isPlaceholderAddress}. */
    protected static final Pattern[] PLACEHOLDER_PATTERNS = {
        Pattern.compile("[0-9]+@facebook.com"),
    };
}
