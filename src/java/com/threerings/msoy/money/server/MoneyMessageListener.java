//
// $Id$

package com.threerings.msoy.money.server;

import java.io.IOException;
import java.nio.ByteBuffer;

import com.google.inject.Inject;

import com.samskivert.util.Invoker;

import com.threerings.presents.annotation.MainInvoker;
import com.threerings.presents.server.ShutdownManager;
import com.threerings.presents.server.ShutdownManager.Shutdowner;

import com.threerings.msoy.money.data.all.MemberMoney;
import com.threerings.msoy.server.ServerConfig;
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
 *
 * @author Kyle Sampson <kyle@threerings.net>
 */
public class MoneyMessageListener
    implements Shutdowner
{
    /**
     * Constructs a new receiver.  This will not automatically start.
     *
     * @param conn Connection to listen for messages on.
     * @param logic MoneyLogic implementation to call.
     */
    @Inject public MoneyMessageListener (ShutdownManager sm)
    {
        sm.registerShutdowner(this);
    }

    /**
     * Begins listening for incoming messages.
     */
    public void start ()
    {
        // Start listening on buy bars queue.  If bars are purchased, call MoneyLogic.boughtBars().
        _barsBoughtListener = listen("messaging.whirled.barsBought.address", new MessageListener() {
            public void received (final byte[] message, final Replier replier)
            {
                _invoker.postUnit(new Invoker.Unit("money/barsBought") {
                    @Override
                    public boolean invoke ()
                    {
                        final BarsBoughtMessage bbm = new BarsBoughtMessage(message);
                        final MemberRecord member = _memberRepo.loadMember(bbm.accountName);
                        _logic.boughtBars(member.memberId, bbm.numBars, bbm.payment);
                        return false;
                    }
                });
            }
        });
        if (_barsBoughtListener != null && !_barsBoughtListener.isClosed()) {
            log.info("Now listening for bars bought messages.");
        }

        // Start listening get bars count queue.  Send a reply with the number of bars
        _getBarCountListener = listen("messaging.whirled.getBarCount.address", new MessageListener() {
            public void received (final byte[] message, final Replier replier)
            {
                _invoker.postUnit(new Invoker.Unit("money/getBarCount") {
                    @Override
                    public boolean invoke ()
                    {
                        final GetBarCountMessage gbcm = new GetBarCountMessage(message);
                        final MemberRecord member = _memberRepo.loadMember(gbcm.accountName);

                        try {
                            final MemberMoney money = _logic.getMoneyFor(member.memberId);
                            replier.reply(new IntMessage(money.bars));
                            return false;
                        } catch (final IOException ioe) {
                            throw new RuntimeException(
                                "Could not send a reply for getBarCount.", ioe);
                        }
                    }
                });
            }
        });
        if (_barsBoughtListener != null && !_getBarCountListener.isClosed()) {
            log.info("Now listening for get bar count messages.");
        }
    }

    /**
     * Closes any listeners when shutting down.
     */
    public void shutdown ()
    {
        if (_barsBoughtListener != null) {
            try {
                _barsBoughtListener.close();
            } catch (final IOException ioe) {
                log.warning("Could not close bars bought listener.", ioe);
            }
        }
        if (_getBarCountListener != null) {
            try {
                _getBarCountListener.close();
            } catch (final IOException ioe) {
                log.warning("Could not close get bar count listener.", ioe);
            }
        }
    }

    /**
     * Listens for messages on the destination address in the server configuration specified by
     * configKey.  When messages come in, they will execute the given message listener.
     */
    protected ConnectedListener listen (final String configKey, final MessageListener listener)
    {
        final String barsBoughtStr = ServerConfig.config.getValue(
            configKey, "");
        if (!"".equals(barsBoughtStr)) {
            final DestinationAddress addr = new DestinationAddress(barsBoughtStr);
            return _conn.listen(addr.getRoutingKey(), addr, listener);
        }
        return null;
    }

    /**
     * Message to retrieve the number of bars for a particular user.
     */
    protected static final class GetBarCountMessage
    {
        public final String accountName;

        public GetBarCountMessage (final byte[] bytes)
        {
            final ByteBuffer buf = ByteBuffer.wrap(bytes);
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

        public BarsBoughtMessage (final byte[] bytes)
        {
            final ByteBuffer buf = ByteBuffer.wrap(bytes);
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

    protected ConnectedListener _barsBoughtListener;
    protected ConnectedListener _getBarCountListener;

    // dependencies
    @Inject protected MoneyLogic _logic;
    @Inject protected MemberRepository _memberRepo;
    @Inject @MainInvoker protected Invoker _invoker;
    @Inject protected MessageConnection _conn;
}
