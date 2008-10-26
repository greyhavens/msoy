//
// $Id$

package com.threerings.msoy.tools;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Set;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;

import com.samskivert.jdbc.ConnectionProvider;
import com.samskivert.jdbc.depot.PersistenceContext;
import com.samskivert.util.RunQueue;
import com.samskivert.util.Tuple;

import com.threerings.presents.annotation.EventQueue;
import com.threerings.presents.server.PresentsDObjectMgr;
import com.threerings.presents.server.ShutdownManager;

import com.threerings.msoy.fora.server.persist.ForumMessageRecord;
import com.threerings.msoy.fora.server.persist.ForumRepository;
import com.threerings.msoy.fora.server.persist.ForumThreadRecord;
import com.threerings.msoy.mail.server.SpamUtil;
import com.threerings.msoy.web.gwt.MessageUtil;

import com.threerings.msoy.server.ServerConfig;
import com.threerings.msoy.server.persist.MemberRepository;
import com.threerings.msoy.server.util.MailSender;

import static com.threerings.msoy.Log.log;

/**
 * Sends out a mass mailing and handles some custom bits.
 */
public class MassMailer
{
    /** Configures dependencies needed by the Msoy servers. */
    public static class Module extends AbstractModule
    {
        @Override protected void configure () {
            bind(PersistenceContext.class).toInstance(new PersistenceContext());
            bind(RunQueue.class).annotatedWith(EventQueue.class).to(PresentsDObjectMgr.class);
        }
    }

    public static void main (String[] argvec)
        throws Exception
    {
        final Set<String> testAddrs = Sets.newHashSet();
        final Set<String> skipAddrs = Sets.newHashSet();
        final int threadId = parseArgs(argvec, skipAddrs, testAddrs);

        final Injector injector = Guice.createInjector(new Module());
        final ShutdownManager shutMgr = injector.getInstance(ShutdownManager.class);
        final MailSender mailer = injector.getInstance(MailSender.class);
        final MemberRepository memberRepo = injector.getInstance(MemberRepository.class);
        final ForumRepository forumRepo = injector.getInstance(ForumRepository.class);

        // initialize our persistence context
        final PersistenceContext perCtx = injector.getInstance(PersistenceContext.class);
        ConnectionProvider conprov = ServerConfig.createConnectionProvider();
        perCtx.init("msoy", conprov, null);
        perCtx.initializeRepositories(true);

        // load up the forum thread and message in question
        ForumThreadRecord ftr = forumRepo.loadThread(threadId);
        if (ftr == null) {
            log.warning("No forum thread with that id.", "threadId", threadId);
            System.exit(255);
        }

        List<ForumMessageRecord> msgs = forumRepo.loadMessages(ftr.threadId, 0, 1);
        if (msgs.size() == 0) {
            log.warning("Thread has no first message.", "subject", ftr.subject);
            System.exit(255);
        }

        // expand the media bits in the message into HTML
        String body = MessageUtil.expandMessage(msgs.get(0).message);

        // format the body into an HTML mail message
        body = SpamUtil.formatSpam(body);
        if (body == null) {
            System.exit(255); // formatSpam will have warned
        }

        String[] headers = SpamUtil.makeSpamHeaders(ftr.subject);
        String from = ServerConfig.getFromAddress();
        int count = 0;

        // load up the emails of everyone we want to spam
        List<Tuple<Integer, String>> emails = memberRepo.loadMemberEmailsForAnnouncement();
        for (Tuple<Integer, String> recip : emails) {
            if (skipAddrs.contains(recip.right)) {
                continue;
            }
            if (testAddrs.size() > 0 && !testAddrs.contains(recip.right)) {
                continue;
            }
            mailer.sendEmail(recip.right, from, headers, ftr.subject,
                             SpamUtil.customizeSpam(body, recip.left, recip.right), true);
            count++;
        }

        log.info("Queued up announcement email", "subject", ftr.subject, "count", count);

        // finally shutdown which will finish sending all of our mails and terminate
        shutMgr.shutdown();
    }

    protected static int parseArgs (String[] argvec, Set<String> skipAddrs, Set<String> testAddrs)
    {
        List<String> args = Lists.newArrayList(argvec);
        try {
            for (Iterator<String> iter = args.iterator(); iter.hasNext(); ) {
                String arg = iter.next();
                if (arg.equals("-skip")) {
                    iter.remove();
                    BufferedReader bin = new BufferedReader(new FileReader(iter.next()));
                    String addr;
                    while ((addr = bin.readLine()) != null) {
                        skipAddrs.add(addr);
                    }
                    iter.remove();
                } else if (arg.equals("-test")) {
                    iter.remove();
                    testAddrs.add(iter.next());
                    iter.remove();
                }
            }
            return Integer.parseInt(args.get(0));

        } catch (IOException ioe) {
            System.err.println("Error: " + ioe.getMessage());

        } catch (Exception e) {
            System.err.println(USAGE);
        }

        System.exit(255);
        return 0; // not reached
    }

    protected static final String USAGE =
        "Usage: MassMailer [-skip skipaddrs.txt] [-test foo@bar.com] threadId";
}
