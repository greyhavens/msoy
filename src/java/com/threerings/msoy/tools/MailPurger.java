package com.threerings.msoy.tools;

import java.sql.Timestamp;
import java.util.List;

import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Inject;
import com.google.inject.Injector;

import com.samskivert.util.Calendars;

import com.samskivert.jdbc.ConnectionProvider;

import com.samskivert.depot.Ops;
import com.samskivert.depot.PersistenceContext;
import com.samskivert.depot.clause.Where;

import com.threerings.msoy.data.all.Friendship;
import com.threerings.msoy.mail.server.persist.ConvMessageRecord;
import com.threerings.msoy.mail.server.persist.ConversationRecord;
import com.threerings.msoy.mail.server.persist.MailRepository;
import com.threerings.msoy.mail.server.persist.ParticipantRecord;
import com.threerings.msoy.server.ServerConfig;
import com.threerings.msoy.server.persist.MemberRepository;

import static com.threerings.msoy.Log.log;

/**
 * This class purges old friend invite mails, specifically threads that consist exclusively of
 * generic invites and responses. The natural extension would be to also purge generic game
 * invitations without replies. Anything a player has actually written we keep.
 */
public class MailPurger
{
    public static void main (String[] args)
        throws Exception
    {
        Injector injector = Guice.createInjector(new AbstractModule() {
            @Override protected void configure () {
                bind(PersistenceContext.class).toInstance(new PersistenceContext());
            }
        });

        int days = OLD_MAIL_DAYS;
        if (args.length > 0) {
            days = Integer.parseInt(args[0]);
            if (days < OLD_MAIL_DAYS) {
                log.warning("Refusing to delete mail younger than " + OLD_MAIL_DAYS + " days.");
                days = OLD_MAIL_DAYS;
            }
        }

        injector.getInstance(MailPurger.class).execute(days);
    }

    public void execute (int days)
        throws Exception
    {
        Timestamp old = Calendars.now().zeroTime().addDays(-days).toTimestamp();

        ConnectionProvider conprov = ServerConfig.createConnectionProvider();
        _perCtx.init("msoy", conprov, null);
        _perCtx.initializeRepositories(true);

        log.info("Finding old friend invite threads without replies...");
        List<ConversationRecord> recs = _mailRepo.findAll(
            ConversationRecord.class, new Where(Ops.and(
                ConversationRecord.LAST_SNIPPET.in(GENERIC_REPLY_SNIPPET, GENERIC_INVITE_SNIPPET),
                ConversationRecord.LAST_SENT.lessEq(old))));

        log.info("Deleting " + recs.size() + " threads and messages...");
        int participants = 0, messages = 0, threads = 0, friendships = 0;
        for (ConversationRecord rec : recs) {
            participants += _mailRepo.deleteAll(ParticipantRecord.class, new Where(
                ParticipantRecord.CONVERSATION_ID.eq(rec.conversationId)));
            messages += _mailRepo.deleteAll(ConvMessageRecord.class, new Where(
                ConvMessageRecord.CONVERSATION_ID.eq(rec.conversationId)));
            threads += _mailRepo.delete(ConversationRecord.getKey(rec.conversationId));
            if (!GENERIC_INVITE_SNIPPET.equals(rec.lastSnippet)) {
                continue;
            }
            // if we're deleting a friend invitation that was never answered, cancel it
            if (Friendship.INVITED == _memberRepo.getFriendship(rec.initiatorId, rec.targetId)) {
                friendships ++;
                _memberRepo.clearFriendship(rec.initiatorId, rec.targetId);
            }
        }
        log.info("Deleted " + threads + " threads, " + participants + " participants, " +
            messages + " messages, and revoked " + friendships + " friendship invites.");
    }

    @Inject protected PersistenceContext _perCtx;
    @Inject protected MemberRepository _memberRepo;
    @Inject protected MailRepository _mailRepo;

    protected static final String GENERIC_INVITE_SNIPPET = "Let's be buddies!";
    protected static final String GENERIC_REPLY_SNIPPET = "Very well, I accept!";
    protected static final int OLD_MAIL_DAYS = 90;
}
