//
// $Id$

package com.threerings.msoy.money.server;

import static com.threerings.msoy.Log.log;

import java.io.IOException;
import java.nio.ByteBuffer;

import com.google.inject.Inject;
import com.samskivert.util.Invoker;
import com.samskivert.util.Lifecycle;
import com.samskivert.util.StringUtil;
import com.threerings.messaging.AddressedMessageListener;
import com.threerings.messaging.DestinationAddress;
import com.threerings.messaging.InMessage;
import com.threerings.messaging.IntMessage;
import com.threerings.messaging.MessageConnection;
import com.threerings.messaging.AckingMessageListener;
import com.threerings.msoy.server.ServerConfig;
import com.threerings.msoy.server.SubscriptionLogic;
import com.threerings.msoy.server.persist.MemberRecord;
import com.threerings.msoy.server.persist.MemberRepository;
import com.threerings.presents.annotation.MainInvoker;

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
        // Handle subscription billed messages
        listen("subscriptionBilled", new AckingMessageListener() {
            public void processReceived (InMessage message) {
                SubscriptionBilledMessage ssm = null;
                try {
                    ssm = new SubscriptionBilledMessage(message.getBody());
                    log.info("Noting subscription billed: ", "accountName", ssm.accountName,
                        "months", ssm.months);
                    _subLogic.noteSubscriptionBilled(ssm.accountName, ssm.months);
                } catch (Exception e) {
                    log.warning("Fouled-up trying to note a subscription billing",
                                "accountName", (ssm == null) ? "<unknown>" : ssm.accountName, e);
                }
            }
        });

        // Handle subscription ended messages
        listen("subscriptionEnded", new AckingMessageListener() {
            public void processReceived (InMessage message) {
                SubscriptionEndedMessage sem = null;
                try {
                    sem = new SubscriptionEndedMessage(message.getBody());
                    log.info("Noting subscription ended: ", "accountName", sem.accountName);
                    _subLogic.noteSubscriptionEnded(sem.accountName);
                } catch (Exception e) {
                    log.warning("Fouled-up trying to note a subscription end",
                                "accountName", (sem == null) ? "<unknown>" : sem.accountName, e);
                }
            }
        });

        // Handle bars bought messages
        listen("barsBought", new AckingMessageListener() {
            public void processReceived (InMessage message) {
                BarsBoughtMessage bbm = new BarsBoughtMessage(message.getBody());
                log.info("Noting bars bought: ", "accountName", bbm.accountName,
                    "numBars", bbm.numBars, "payment", bbm.payment);
                MemberRecord member = _memberRepo.loadMember(bbm.accountName);
                if (member != null) {
                    _logic.boughtBars(member.memberId, bbm.numBars, bbm.payment);
                } else {
                    log.warning("Got barsBought message for unknown account",
                        "accountName", bbm.accountName);
                }
            }
        });

        // Handle get bar count messages
        listen("getBarCount", new AckingMessageListener() {
            public void processReceived (InMessage message) {
                GetBarCountMessage gbcm = new GetBarCountMessage(message.getBody());
                log.info("Getting bar count: ", "accountName", gbcm.accountName);
                MemberRecord member = _memberRepo.loadMember(gbcm.accountName);
                try {
                    int bars;
                    if (member != null) {
                        bars = _logic.getMoneyFor(member.memberId).bars;
                    } else {
                        log.warning("Got getBarCount query for unknown account",
                            "accountName", gbcm.accountName);
                        // TODO: is there another way to let billing know there's trouble?
                        bars = -1;
                    }
                    message.reply(new IntMessage(bars));
                } catch (IOException ioe) {
                    throw new RuntimeException("Could not send a reply for getBarCount.", ioe);
                }
            }
        });
    }

    // from interface Lifecycle.Component
    public void shutdown ()
    {
        // Do nothing, the connection is closed from MsoyServer.shutdown()
    }

    /**
     * Listens for messages on the destination address in the server configuration specified by
     * command.  When messages come in, they will execute the given message listener.
     */
    protected void listen (String command, AckingMessageListener listener)
    {
        if (ServerConfig.getAMQPMessageConfig() == null) {
            return; // messaging is not activated, so no listening
        }

        DestinationAddress addr = new DestinationAddress("whirled.money." + command + "@whirled");
        _conn.listen(new AddressedMessageListener(addr.getRoutingKey(), addr, listener));
    }

    /**
     * Message to retrieve the number of bars for a particular user.
     */
    protected static class GetBarCountMessage
    {
        public final String accountName;

        public GetBarCountMessage (byte[] bytes) {
            ByteBuffer buf = ByteBuffer.wrap(bytes);
            byte[] msgBuf = new byte[buf.getInt()];
            buf.get(msgBuf);
            accountName = new String(msgBuf);
        }

        @Override public String toString () {
            return StringUtil.fieldsToString(this);
        }
    }

    /**
     * Message indicating a user purchased some number of bars.
     */
    protected static class BarsBoughtMessage
    {
        public final String accountName;
        public final int numBars;
        public final String payment; // something like "$2.95", I'm hoping

        public BarsBoughtMessage (byte[] bytes) {
            ByteBuffer buf = ByteBuffer.wrap(bytes);
            byte[] msgBuf = new byte[buf.getInt()];
            buf.get(msgBuf);
            accountName = new String(msgBuf);
            numBars = buf.getInt();
            msgBuf = new byte[buf.getInt()];
            buf.get(msgBuf);
            payment = new String(msgBuf);
        }

        @Override public String toString () {
            return StringUtil.fieldsToString(this);
        }
    }

    /**
     * Message indicating a subscription payment was processed.
     */
    protected static class SubscriptionEndedMessage
    {
        public String accountName;

        public SubscriptionEndedMessage (byte[] bytes) {
            init(ByteBuffer.wrap(bytes));
        }

        public void init (ByteBuffer buf){
            byte[] msgBuf = new byte[buf.getInt()];
            buf.get(msgBuf);
            accountName = new String(msgBuf);
        }

        @Override public String toString () {
            return StringUtil.fieldsToString(this);
        }
    }

    /**
     * Message indicating a subscription payment was processed.
     */
    protected static class SubscriptionBilledMessage extends SubscriptionEndedMessage
    {
        public int months;

        public SubscriptionBilledMessage (byte[] bytes) {
            super(bytes);
        }

        @Override public void init (ByteBuffer buf) {
            super.init(buf);
            months = buf.getInt();
        }

        @Override public String toString () {
            return StringUtil.fieldsToString(this);
        }
    }

    // dependencies
    @Inject @MainInvoker protected Invoker _invoker;
    @Inject protected MemberRepository _memberRepo;
    @Inject protected MessageConnection _conn;
    @Inject protected MoneyLogic _logic;
    @Inject protected SubscriptionLogic _subLogic;
}
