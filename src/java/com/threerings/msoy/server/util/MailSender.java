//
// $Id$

package com.threerings.msoy.server.util;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.net.URL;

import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.activation.DataHandler;
import javax.activation.URLDataSource;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;

import com.google.common.collect.Sets;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import com.samskivert.net.MailUtil;
import com.samskivert.velocity.VelocityUtil;

import org.apache.commons.io.IOUtils;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.VelocityEngine;

import com.threerings.presents.server.ShutdownManager;

import static com.threerings.msoy.Log.log;

/**
 * Handles the delivery of email. Mail is delivered asynchronously on a pool of worker threads to
 * allow high throughput and so as to avoid impact to the rest of the Whirled system.
 */
@Singleton
public class MailSender
    implements ShutdownManager.Shutdowner
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

    @Inject public MailSender (ShutdownManager shutmgr)
    {
        shutmgr.registerShutdowner(this);
    }

    // from ShutdownManager.Shutdowner
    public void shutdown ()
    {
        _executor.shutdown();
    }

    /**
     * Delivers an email using the supplied template and parameters.
     *
     * @param recip the recipient address.
     * @param sender the sender address.
     * @param template the identifier of the template to use for the body of the mail.
     * @param an alternating list of string, object which are key/value pairs for substitution into
     * the template.
     */
    public void sendTemplateEmail (String recip, String sender, String template, Object ... params)
    {
        // skip emails to placeholder addresses
        if (!isPlaceholderAddress(recip)) {
            Parameters pobj = new Parameters();
            for (int ii = 0; ii < params.length; ii += 2) {
                pobj.set((String)params[ii], params[ii+1]);
            }
            _executor.execute(new MailTask(recip, sender, template, pobj));
        }
    }

    /**
     * Delivers a preformatted email with the supplied subject and body.
     *
     * @param recip the address of the recipient.
     * @param sender the address of the sender.
     * @param headers optional additional headers to add to the mail { key, value, key, value, ... }.
     * @param subject the subject of the email.
     * @param body the body of the email.
     * @param whether or not the body is an HTML document or plain text.
     */
    public void sendEmail (String recip, String sender, String[] headers, String subject,
                           String body, boolean isHTML)
    {
        // skip emails to placeholder addresses
        if (!isPlaceholderAddress(recip)) {
            _executor.execute(new PreformattedMailTask(
                                  recip, sender, headers, subject, body, isHTML));
        }
    }

    /** Handles the formatting and delivery of a mail message. */
    protected static class MailTask implements Runnable
    {
        public MailTask (String recip, String sender, String template, Parameters params) {
            _recip = recip;
            _sender = sender;
            _template = template;
            _params = params;
        }

        public void run () {
            // create a velocity engine that we'll use to merge text into templates
            try {
                // create a mime message which will contain text and possibly HTML parts
                MimeMultipart parts = new MimeMultipart("alternative");

                // TODO: have a server language and select templates based on that
                StringWriter swout = new StringWriter();
                VelocityEngine ve = VelocityUtil.createEngine();
                ve.mergeTemplate("rsrc/email/" + _template + ".tmpl", "UTF-8",
                                 _params.getContext(), swout);

                String body = swout.toString();
                int nidx = body.indexOf("\n"); // first line is the subject
                String subject = body.substring(0, nidx);
                body = body.substring(nidx+1);
                MimeBodyPart textPart = new MimeBodyPart();
                textPart.setContent(body, "text/plain");
                parts.addBodyPart(textPart);

                // check for an HTML message template as well
                String htmlPath = "rsrc/email/" + _template + "/message.html", htmlData = null;
                try {
                    InputStream htmlIn =
                        MailSender.class.getClassLoader().getResourceAsStream(htmlPath);
                    if (htmlIn != null) {
                        htmlData = IOUtils.toString(htmlIn);
                    }
                } catch (IOException ioe) {
                    log.warning("Failed to load HTML template [path=" + htmlPath +
                                ", error=" + ioe + "].");
                }

                if (htmlData != null) {
                    MimeMultipart htmlParts = new MimeMultipart("related");

                    swout = new StringWriter();
                    ve.mergeTemplate(htmlPath, "UTF-8", _params.getContext(), swout);
                    MimeBodyPart htmlPart = new MimeBodyPart();
                    htmlPart.setContent(swout.toString(), "text/html");
                    htmlParts.addBodyPart(htmlPart);

                    // now add any images referenced in the HTML message
                    Set<String> images = Sets.newHashSet();
                    Matcher m = CID_REGEX.matcher(htmlData);
                    while (m.find()) {
                        images.add(m.group(1));
                    }

                    for (String image : images) {
                        MimeBodyPart ipart = new MimeBodyPart();
                        String ipath = "rsrc/email/" + _template + "/" + image;
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
                MailUtil.deliverMail(new String[] { _recip }, _sender, subject, message);

            } catch (Exception e) {
                log.warning("Failed to send email", "recip", _recip, "sender", _sender,
                            "template", _template, e);
            }
        }

        protected String _recip, _sender, _template;
        protected Parameters _params;
    }

    /** Handles the delivery of an email message which is preformatted. */
    protected static class PreformattedMailTask implements Runnable
    {
        public PreformattedMailTask (String recip, String sender, String[] headers, String subject,
                                     String body, boolean isHTML) {
            _recip = recip;
            _sender = sender;
            _headers = headers;
            _subject = subject;
            _body = body;
            _isHTML = isHTML;
        }

        public void run () {
            try {
                if (_isHTML) {
                    MimeMessage message = MailUtil.createEmptyMessage();
                    int hcount = (_headers != null) ? _headers.length : 0;
                    for (int ii = 0; ii < hcount; ii += 2) {
                        message.addHeader(_headers[ii], _headers[ii+1]);
                    }
                    message.setContent(_body, "text/html");
                    MailUtil.deliverMail(new String[] { _recip }, _sender, _subject, message);
                } else {
                    MailUtil.deliverMail(_recip, _sender, _subject, _body);
                }

            } catch (Exception e) {
                log.warning("Failed to send spam email", "recip", _recip, "sender", _sender,
                            "subject", _subject, e);
            }
        }

        protected String _recip, _sender, _subject, _body;
        protected String[] _headers;
        protected boolean _isHTML;
    }

    /** The executor on which we will dispatch mail sending tasks. */
    protected ExecutorService _executor = new ThreadPoolExecutor(
        CORE_POOL_SIZE, MAX_POOL_SIZE, IDLE_THREAD_LIFETIME, TimeUnit.MILLISECONDS,
        new LinkedBlockingQueue<Runnable>());

    /** Used by {@link #isPlaceholderAddress}. */
    protected static final Pattern[] PLACEHOLDER_PATTERNS = {
        Pattern.compile("[0-9]+@facebook.com"),
    };

    protected static final Pattern CID_REGEX = Pattern.compile("cid\\:(\\S+\\....)");

    protected static final int CORE_POOL_SIZE = 1; // threads
    protected static final int MAX_POOL_SIZE = 10; // threads
    protected static final int IDLE_THREAD_LIFETIME = 5000; // milliseconds
}
