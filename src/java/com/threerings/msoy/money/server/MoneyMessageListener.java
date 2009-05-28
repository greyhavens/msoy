//
// $Id$

package com.threerings.msoy.money.server;

import java.io.IOException;
import java.nio.ByteBuffer;

import java.util.List;

import com.google.common.collect.Lists;

import com.google.inject.Inject;

import com.samskivert.util.Invoker;
import com.samskivert.util.Lifecycle;

import com.threerings.presents.annotation.MainInvoker;

import com.threerings.msoy.money.data.all.MemberMoney;
import com.threerings.msoy.server.ServerConfig;
import com.threerings.msoy.server.SubscriptionLogic;
import com.threerings.msoy.server.persist.MemberRecord;
import com.threerings.msoy.server.persist.MemberRepository;

import com.threerings.messaging.ConnectedListener;
import com.threerings.messaging.DestinationAddress;
import com.threerings.messaging.IntMessage;
import com.threerings.messaging.MessageConnection;
import com.threerings.messaging.MessageListener;
import com.threerings.messaging.Replier;

import static com.threerings.msoy.Log.log;

/**
 * Responsible for receiving messages from outside systems (such as billing) and calling
 * the appropriate action in the money service.
 */
public class MoneyMessageListener
    implements Lifecycle.Component
{
    /**
     * Constructs a new receiver.  This will not automatically start.
     */
    @Inject public MoneyMessageListener (Lifecycle cycle)
    {
        cycle.addComponent(this);
    }

    // from interface Lifecycle.Component
    public void init ()
    {
        listen("messaging.whirled.subscription.address", new MessageListener() {
            public void received (final byte[] message, Replier replier)
            {
                _invoker.postUnit(new Invoker.Unit("money/subscription") {
                    @Override public boolean invoke () {
                        SubscriptionMessage sm = null;
                        try {
                            sm = new SubscriptionMessage(message);
                            _subLogic.noteSubscriptionStarted(sm.accountName, sm.endTime);
                        } catch (Exception e) {
                            log.warning("Holy Bajizzwax! We've fouled-up trying to " +
                                "note a subscription",
                                "accountName", (sm == null) ? "<unknown>" : sm.accountName, e);
                        }
                        return false;
                    }
                });
            }
        });

        // Start listening on buy bars queue.  If bars are purchased, call MoneyLogic.boughtBars().
        listen("messaging.whirled.barsBought.address", new MessageListener() {
            public void received (final byte[] message, final Replier replier)
            {
                _invoker.postUnit(new Invoker.Unit("money/barsBought") {
                    @Override public boolean invoke () {
                        BarsBoughtMessage bbm = new BarsBoughtMessage(message);
                        MemberRecord member = _memberRepo.loadMember(bbm.accountName);
                        _logic.boughtBars(member.memberId, bbm.numBars, bbm.payment);
                        return false;
                    }
                });
            }
        });

        // Start listening get bars count queue.  Send a reply with the number of bars
        listen("messaging.whirled.getBarCount.address", new MessageListener() {
            public void received (final byte[] message, final Replier replier) {
                _invoker.postUnit(new Invoker.Unit("money/getBarCount") {
                    @Override public boolean invoke () {
                        GetBarCountMessage gbcm = new GetBarCountMessage(message);
                        MemberRecord member = _memberRepo.loadMember(gbcm.accountName);

                        try {
                            MemberMoney money = _logic.getMoneyFor(member.memberId);
                            replier.reply(new IntMessage(money.bars));
                            return false;
                        } catch (IOException ioe) {
                            throw new RuntimeException(
                                "Could not send a reply for getBarCount.", ioe);
                        }
                    }
                });
            }
        });
    }

    // from interface Lifecycle.Component
    public void shutdown ()
    {
        for (ConnectedListener listener : _listeners) {
            try {
                listener.close();
            } catch (IOException ioe) {
                log.warning("Could not close money message listener.", ioe);
            }
        }
    }

    /**
     * Listens for messages on the destination address in the server configuration specified by
     * configKey.  When messages come in, they will execute the given message listener.
     */
    protected void listen (String configKey, MessageListener listener)
    {
        String source = ServerConfig.config.getValue(configKey, "");
        if ("".equals(source)) {
            log.debug("Not configured to listen", "configKey", configKey);
            return;
        }
        DestinationAddress addr = new DestinationAddress(source);
        ConnectedListener cl = _conn.listen(addr.getRoutingKey(), addr, listener);
        if (cl.isClosed()) {
            log.warning("Weird! Listener is already closed! That's bad?", "configKey", configKey);
            return;
        }

        _listeners.add(cl);
        log.info("Now listening", "configKey", configKey);
    }

    /**
     * Message to retrieve the number of bars for a particular user.
     */
    protected static final class GetBarCountMessage
    {
        public final String accountName;

        public GetBarCountMessage (byte[] bytes)
        {
            ByteBuffer buf = ByteBuffer.wrap(bytes);
            byte[] msgBuf = new byte[buf.getInt()];
            buf.get(msgBuf);
            accountName = new String(msgBuf);
        }

        @Override
        public String toString ()
        {
            return "accountName: " + accountName;
        }
    }

    /**
     * Message indicating a user purchased some number of bars.
     */
    protected static final class BarsBoughtMessage
    {
        public final String accountName;
        public final int numBars;
        public final String payment; // something like "$2.95", I'm hoping

        public BarsBoughtMessage (byte[] bytes)
        {
            ByteBuffer buf = ByteBuffer.wrap(bytes);
            byte[] msgBuf = new byte[buf.getInt()];
            buf.get(msgBuf);
            accountName = new String(msgBuf);
            numBars = buf.getInt();
            msgBuf = new byte[buf.getInt()];
            buf.get(msgBuf);
            payment = new String(msgBuf);
        }

        @Override
        public String toString ()
        {
            return "accountName: " + accountName +
                ", numBars: " + numBars +
                ", payment: " + payment;
        }
    }

    /**
     * Message indicating a subscription payment was processed.
     */
    protected static final class SubscriptionMessage
    {
        public final String accountName;
        public final long endTime;

        public SubscriptionMessage (byte[] bytes)
        {
            ByteBuffer buf = ByteBuffer.wrap(bytes);
            byte[] msgBuf = new byte[buf.getInt()];
            buf.get(msgBuf);
            accountName = new String(msgBuf);
            endTime = buf.getLong();
        }

        @Override
        public String toString ()
        {
            return "accountName: " + accountName + ", endTime: " + endTime;
        }
    }

    /** Our listeners. */
    protected List<ConnectedListener> _listeners = Lists.newArrayList();

    // dependencies
    @Inject @MainInvoker protected Invoker _invoker;
    @Inject protected MemberRepository _memberRepo;
    @Inject protected MessageConnection _conn;
    @Inject protected MoneyLogic _logic;
    @Inject protected SubscriptionLogic _subLogic;
}
