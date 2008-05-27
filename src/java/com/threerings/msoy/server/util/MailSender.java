//
// $Id$

package com.threerings.msoy.server.util;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.net.URL;

import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.activation.DataHandler;
import javax.activation.URLDataSource;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;

import com.google.common.collect.Sets;

import com.samskivert.net.MailUtil;
import com.samskivert.util.Invoker;
import com.samskivert.velocity.VelocityUtil;

import org.apache.commons.io.IOUtils;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.VelocityEngine;

import com.threerings.presents.annotation.BlockingThread;

import com.threerings.msoy.server.MsoyBaseServer;
import com.threerings.msoy.server.ServerConfig;
import com.threerings.msoy.web.data.ServiceCodes;

import static com.threerings.msoy.Log.log;

/**
 * Utility methods for sending email, using the mail invoker thread if appropriate.
 */
public class MailSender
{
    /** Used to provide key/value pairs that are substituted into mail templates. */
    public static class Parameters
    {
        public void set (String key, Object value) {
            _ctx.put(key, value);
        }

        /*package*/ VelocityContext getContext () {
            return _ctx;
        }

        protected VelocityContext _ctx = new VelocityContext();
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
     * Delivers an email using the supplied template and parameters which are used to populate a
     * {@link Parameters} instance.
     *
     * @return null or a string indicating the problem in the event of failure.
     */
    @BlockingThread
    public static String sendEmail (String recip, String sender, String template, Object ... params)
    {
        Parameters pobj = new Parameters();
        for (int ii = 0; ii < params.length; ii += 2) {
            pobj.set((String)params[ii], params[ii+1]);
        }
        return sendEmail(recip, sender, template, pobj);
    }

    /**
     * Delivers an email using the supplied template and context. The first line of the template
     * should be the subject and the remaining lines the body.
     *
     * @return null or a string indicating the problem in the event of failure.
     */
    @BlockingThread
    public static String sendEmail (String recip, String sender, String template, Parameters params)
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
            log.warning("Failed to create the velocity engine.", e);
            return ServiceCodes.E_INTERNAL_ERROR;
        }

        // create a mime message which will contain text and possibly HTML parts
        MimeMultipart parts = new MimeMultipart("alternative");

        StringWriter sw = new StringWriter();
        try {
            // TODO: have a server language and select templates based on that
            ve.mergeTemplate("rsrc/email/" + template + ".tmpl", "UTF-8", params.getContext(), sw);

            String body = sw.toString();
            int nidx = body.indexOf("\n"); // first line is the subject
            String subject = body.substring(0, nidx);
            body = body.substring(nidx+1);
            MimeBodyPart textPart = new MimeBodyPart();
            textPart.setContent(body, "text/plain");
            parts.addBodyPart(textPart);

            // check for an HTML message template as well
            String htmlPath = "rsrc/email/" + template + "/message.html", htmlData = null;
            try {
                InputStream htmlIn = MailSender.class.getClassLoader().getResourceAsStream(htmlPath);
                if (htmlIn != null) {
                    htmlData = IOUtils.toString(htmlIn);
                }
            } catch (IOException ioe) {
                log.warning("Failed to load HTML template [path=" + htmlPath +
                            ", error=" + ioe + "].");
            }

            if (htmlData != null) {
                MimeMultipart htmlParts = new MimeMultipart("related");

                sw = new StringWriter();
                ve.mergeTemplate(htmlPath, "UTF-8", params.getContext(), sw);
                MimeBodyPart htmlPart = new MimeBodyPart();
                htmlPart.setContent(sw.toString(), "text/html");
                htmlParts.addBodyPart(htmlPart);

                // now add any images referenced in the HTML message
                Set<String> images = Sets.newHashSet();
                Matcher m = CID_REGEX.matcher(htmlData);
                while (m.find()) {
                    images.add(m.group(1));
                }

                for (String image : images) {
                    MimeBodyPart ipart = new MimeBodyPart();
                    String ipath = "rsrc/email/" + template + "/" + image;
                    URL iurl = MailSender.class.getClassLoader().getResource(ipath);
                    if (iurl == null) {
                        log.warning("Unable to find mail resource [path=" + ipath + "].");
                    } else {
                        ipart.setDataHandler(new DataHandler(new URLDataSource(iurl)));
                        ipart.setFileName(image);
                        ipart.setContentID("<" + image + ">");
                        htmlParts.addBodyPart(ipart);
                    }
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
            log.warning("Failed to send email.", e);
            return e.getMessage();
        }
    }

    /** Used by {@link #isPlaceholderAddress}. */
    protected static final Pattern[] PLACEHOLDER_PATTERNS = {
        Pattern.compile("[0-9]+@facebook.com"),
    };

    protected static final Pattern CID_REGEX = Pattern.compile("cid\\:(\\S+\\....)");
}
