//
// $Id$

package com.threerings.msoy.server.util;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.net.URL;

import java.util.List;
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
import com.samskivert.util.Tuple;
import com.samskivert.velocity.VelocityUtil;

import org.apache.commons.io.IOUtils;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.VelocityEngine;

import com.threerings.msoy.data.all.MemberMailUtil;
import com.threerings.msoy.mail.server.SpamUtil;
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
     * @param params an alternating list of string, object which are key/value pairs for
     * substitution into the template.
     */
    public void sendTemplateEmail (String recip, String sender, String template, Object ... params)
    {
        Parameters pobj = new Parameters();
        for (int ii = 0; ii < params.length; ii += 2) {
            pobj.set((String)params[ii], params[ii+1]);
        }
        sendTemplateEmail(recip, sender, template, pobj);
    }

    /**
     * Delivers an email using the supplied template and parameters.
     *
     * @param recip the recipient address.
     * @param sender the sender address.
     * @param template the identifier of the template to use for the body of the mail.
     * @param params a filled-in Parameters instance.
     */
    public void sendTemplateEmail (String recip, String sender, String template, Parameters params)
    {
        // skip emails to placeholder addresses
        if (!isPlaceholderAddress(recip)) {
            _executor.execute(new TemplateMailTask(recip, sender, template, params));
        }
    }

    /**
     * Delivers a mass mailing email with the supplied subject and body to the supplied list of
     * recipients.
     *
     * @param recips {memberId, email} for each of the recipients.
     * @param sender the address of the sender.
     * @param headers optional additional headers to add to the mail { key, value, key, value,
     * ... }.
     * @param subject the subject of the email.
     * @param body the body of the email.
     */
    public void sendSpam (List<Tuple<Integer, String>> recips, String sender, String[] headers,
                          String subject, String body)
    {
        _executor.execute(new SpamTask(recips, sender, headers, subject, body));
    }

    /**
     * Sends a text email to the supplied recipient.
     */
    public void sendEmail (String recip, String sender, String subject, String body)
    {
        if (!isPlaceholderAddress(recip)) {
            _executor.execute(new MailTask(recip, sender, subject, body));
        }
    }

    /** Handles the formatting and delivery of a mail message. */
    protected static class TemplateMailTask implements Runnable
    {
        public TemplateMailTask (String recip, String sender, String template, Parameters params) {
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
                textPart.setText(body, "UTF-8");
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
                    htmlPart.setText(swout.toString(), "UTF-8", "html");
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

    /** Handles the delivery of a mass email message. */
    protected static class SpamTask implements Runnable
    {
        public SpamTask (List<Tuple<Integer, String>> recips, String sender, String[] headers,
                         String subject, String body) {
            _recips = recips;
            _sender = sender;
            _headers = headers;
            _subject = subject;
            _body = body;
        }

        public void run () {
            // grind through and send email to each of our recipients
            for (Tuple<Integer, String> recip : _recips) {
                String body = SpamUtil.customizeSpam(_body, recip.left, recip.right);
                try {
                    MimeMessage message = MailUtil.createEmptyMessage();
                    int hcount = (_headers == null) ? 0 : _headers.length;
                    for (int ii = 0; ii < hcount; ii += 2) {
                        message.addHeader(_headers[ii], _headers[ii+1]);
                    }
                    message.setText(body, "UTF-8", "html");
                    MailUtil.deliverMail(new String[] { recip.right }, _sender, _subject, message);

                } catch (com.sun.mail.smtp.SMTPAddressFailedException badAddress) {
                    // TODO: why does javax.mail refuse some domain names?
                    log.info("Could not spam address", "address", recip.right);

                } catch (Exception e) {
                    log.warning("Failed to send spam email", "recip", recip, "sender", _sender,
                                "subject", _subject, e);
                }
            }
        }

        protected List<Tuple<Integer, String>> _recips;
        protected String _sender, _subject, _body;
        protected String[] _headers;
    }

    /** Handles the delivery of a text mail message. */
    protected static class MailTask implements Runnable
    {
        public MailTask (String recip, String sender, String subject, String body) {
            _recip = recip;
            _sender = sender;
            _subject = subject;
            _body = body;
        }

        public void run () {
            try {
                MailUtil.deliverMail(_recip, _sender, _subject, _body);
            } catch (Exception e) {
                log.warning("Failed to send simple email", "recip", _recip, "sender", _sender,
                            "subject", _subject, e);
            }
        }

        protected String _recip, _sender, _subject, _body;
    }

    /** The executor on which we will dispatch mail sending tasks. */
    protected ExecutorService _executor = new ThreadPoolExecutor(
        CORE_POOL_SIZE, MAX_POOL_SIZE, IDLE_THREAD_LIFETIME, TimeUnit.MILLISECONDS,
        new LinkedBlockingQueue<Runnable>());

    /**
     * An optimized version of {@link MemberMailUtil#isPlaceholderAddress}.
     */
    protected static boolean isPlaceholderAddress (String address)
    {
        for (Pattern pattern : PLACEHOLDER_PATTERNS) {
            if (pattern.matcher(address).matches()) {
                return true;
            }
        }
        return false;
    }

    /** Used by {@link #isPlaceholderAddress}. */
    protected static final Pattern[] PLACEHOLDER_PATTERNS;
    static {
        PLACEHOLDER_PATTERNS = new Pattern[MemberMailUtil.PLACEHOLDER_PATTERNS.length];
        for (int ii = 0; ii < PLACEHOLDER_PATTERNS.length; ii++) {
            PLACEHOLDER_PATTERNS[ii] = Pattern.compile(MemberMailUtil.PLACEHOLDER_PATTERNS[ii]);
        }
    }

    protected static final Pattern CID_REGEX = Pattern.compile("cid\\:(\\S+\\....)");

    protected static final int CORE_POOL_SIZE = 1; // threads
    protected static final int MAX_POOL_SIZE = 10; // threads
    protected static final int IDLE_THREAD_LIFETIME = 5000; // milliseconds
}
