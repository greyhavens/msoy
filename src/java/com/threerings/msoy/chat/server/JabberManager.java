//
// $Id$

package com.threerings.msoy.chat.server;

import java.util.List;
import java.util.Map;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.inject.Inject;
import com.google.inject.Singleton;

// import org.jivesoftware.smack.Chat;
// import org.jivesoftware.smack.ChatManagerListener;
// import org.jivesoftware.smack.ConnectionConfiguration;
// import org.jivesoftware.smack.ConnectionListener;
// import org.jivesoftware.smack.MessageListener;
// import org.jivesoftware.smack.PacketResponder;
// import org.jivesoftware.smack.SmackConfiguration;
// import org.jivesoftware.smack.TimedPacketResponder;
// import org.jivesoftware.smack.XMPPConnection;
// import org.jivesoftware.smack.XMPPException;
// import org.jivesoftware.smack.filter.PacketIDFilter;
// import org.jivesoftware.smack.filter.ToContainsFilter;
// import org.jivesoftware.smack.packet.IQ;
// import org.jivesoftware.smack.packet.Message;
// import org.jivesoftware.smack.packet.Packet;
// import org.jivesoftware.smack.packet.Presence;
// import org.jivesoftware.smack.packet.Registration;
// import org.jivesoftware.smack.packet.RosterPacket;
// import org.jivesoftware.smack.util.StringUtils;

import com.samskivert.util.Interval;
import com.samskivert.util.Lifecycle;
import com.samskivert.util.ResultListener;
import com.samskivert.util.StringUtil;

import com.threerings.util.MessageBundle;
import com.threerings.util.MessageManager;
import com.threerings.util.Name;

import com.threerings.presents.annotation.EventThread;
import com.threerings.presents.data.ClientObject;
import com.threerings.presents.dobj.DSet;
import com.threerings.presents.dobj.RootDObjectManager;
import com.threerings.presents.server.ClientManager;
import com.threerings.presents.server.InvocationManager;
import com.threerings.presents.server.PresentsSession;

import com.threerings.crowd.chat.data.UserMessage;
import com.threerings.crowd.chat.server.ChatProvider;
import com.threerings.crowd.chat.server.SpeakUtil;

import com.threerings.msoy.chat.client.JabberService;
import com.threerings.msoy.chat.data.JabberMarshaller;
import com.threerings.msoy.data.MemberObject;
import com.threerings.msoy.data.MsoyCodes;
import com.threerings.msoy.data.all.ContactEntry;
import com.threerings.msoy.data.all.DeploymentConfig;
import com.threerings.msoy.data.all.GatewayEntry;
import com.threerings.msoy.data.all.JabberName;
import com.threerings.msoy.server.MemberLocator;
import com.threerings.msoy.server.ServerConfig;

import static com.threerings.msoy.Log.log;

/**
 * Manages the connection to a jabber server providing gateway access to external IM networks.
 */
@Singleton @EventThread
public class JabberManager
    implements Lifecycle.Component, JabberProvider/*, ClientManager.ClientObserver, ConnectionListener*/
{
    public static boolean DEBUG = false;

    @Inject public JabberManager (Lifecycle cycle, ClientManager clmgr, InvocationManager invmgr)
    {
        cycle.addComponent(this);
        // clmgr.addClientObserver(this);
        invmgr.registerProvider(this, JabberMarshaller.class, MsoyCodes.WORLD_GROUP);
    }

    // from interface Lifecycle.Component
    public void init ()
    {
        // String host = ServerConfig.config.getValue("jabber.host", "localhost");
        // int port = ServerConfig.config.getValue("jabber.port", 5275);
        // String gatewayList = ServerConfig.config.getValue("jabber.gateways", "");

        // ConnectionConfiguration config = new ConnectionConfiguration(host, port);
        // _conn = new XMPPConnection(config, _omgr);

        // // For now, only try and connect to the Jabber server if we're on a development server
        // // and have at least one gateway configured
        // if (!DeploymentConfig.devDeployment || StringUtil.isBlank(gatewayList)) {
        //     return;
        // }

        // try {
        //     _conn.connect(ServerConfig.nodeName);
        // } catch (XMPPException e) {
        //     log.warning("Unable to connect to jabber server [host=" + host + ", error=" +
        //             e + ", cause=" + e.getCause() + "].");
        //     attemptReconnect();
        //     return;
        // }
        // handshake();
    }

    // from interface Lifecycle.Component
    public void shutdown ()
    {
        // if (_reconnectInterval != null) {
        //     _reconnectInterval.cancel();
        //     _reconnectInterval = null;
        // }
        // if (_conn.isConnected()) {
        //     _conn.removeConnectionListener(this);
        // }
        // cleanup();
        // if (_conn.isConnected()) {
        //     _conn.disconnect();
        // }
    }

    // // from interface ConnectionListener
    // public void connectionClosed ()
    // {
    //     // we should never be closing the connection willingly unless we're shutting down
    // }

    // // from interface ConnectionListener
    // public void connectionClosedOnError (Exception e)
    // {
    //     log.info("Jabber server connection failed [error=" + e + ", cause=" + e.getCause() + "].");
    //     cleanup();
    //     attemptReconnect();
    // }

    // // from interface ConnectionListener
    // public void reconnectingIn (int seconds)
    // {
    //     // ignored
    // }

    // // from interface ConnectionListener
    // public void reconnectionSuccessful ()
    // {
    // }

    // // from interface ConnectionListener
    // public void reconnectionFailed (Exception e)
    // {
    //     // attemptReconnect();
    // }

    // // from interface ClientManager.ClientObserver
    // public void clientSessionDidStart (PresentsSession client)
    // {
    //     MemberObject user = _locator.lookupMember(client.getClientObject());
    //     if (user == null) {
    //         return;
    //     }
    //     user.startTransaction();
    //     try {
    //         for (String gateway : _gateways) {
    //             user.addToGateways(new GatewayEntry(gateway));
    //         }
    //     } finally {
    //         user.commitTransaction();
    //     }
    // }

    // // from interface ClientManager.ClientObserver
    // public void clientSessionDidEnd (PresentsSession client)
    // {
    //     MemberObject user = _locator.lookupMember(client.getClientObject());
    //     if (user == null) {
    //         return;
    //     }
    //     logoffUser(user.username);
    //     removePacketResponder(user);
    //     _users.remove(user.username);
    // }

    // from interface JabberProvider
    public void registerIM (ClientObject caller, final String gateway, final String username,
            String password, final JabberService.InvocationListener listener)
    {
        listener.requestFailed("m.no_longer_supported");
        // if (!_conn.isConnected() || !_conn.isAuthenticated() || !_gateways.contains(gateway)) {
        //     listener.requestFailed("IM service not currently available");
        //     return;
        // }
        // final MemberObject user = _locator.requireMember(caller);
        // Registration reg = new Registration();
        // reg.setTo(gateway + "." + _conn.getServiceName());
        // String uJID = getJID(user);
        // reg.setFrom(uJID);
        // reg.setType(IQ.Type.SET);
        // Map<String, String> map = Maps.newHashMap();
        // map.put("username", username);
        // map.put("password", password);
        // log.info("Jabber register [uJID=" + uJID +
        //         ", to=" + gateway + "." + _conn.getServiceName() + "].");
        // reg.setAttributes(map);
        // TimedPacketResponder responder = new TimedPacketResponder(_conn) {
        //     public void handlePacket (Packet packet) {
        //         IQ iq = (IQ)packet;
        //         if (iq == null) {
        //             listener.requestFailed("No response from IM service");
        //         } else if (iq.getType() == IQ.Type.ERROR) {
        //             listener.requestFailed("Failed to register IM account: " +
        //                     iq.getError().toString());
        //         } else {
        //             loginUser(user, gateway, username);
        //         }
        //     }
        // };
        // responder.init(new PacketIDFilter(reg.getPacketID()));
        // _conn.sendPacket(reg);
        // responder.nextResult(SmackConfiguration.getPacketReplyTimeout());
    }

    // from interface JabberProvider
    public void unregisterIM (ClientObject caller, String gateway,
            JabberService.InvocationListener listener)
    {
        listener.requestFailed("m.no_longer_supported");
        // if (!_conn.isConnected() || !_conn.isAuthenticated() || !_gateways.contains(gateway)) {
        //     listener.requestFailed("IM service not currently available");
        //     return;
        // }
        // MemberObject user = _locator.requireMember(caller);
        // if (!user.gateways.containsKey(gateway)) {
        //     listener.requestFailed("You are not currently logged into this IM service");
        //     return;
        // }
        // logoffUser(user, gateway);
    }

    // from interface JabberProvider
    public void sendMessage (ClientObject caller, JabberName name, String message,
            final JabberService.ResultListener listener)
    {
        listener.requestFailed("m.no_longer_supported");
        // if (!_conn.isConnected() || !_conn.isAuthenticated()) {
        //     listener.requestFailed("IM service not currently available");
        //     return;
        // }
        // MemberObject user = _locator.requireMember(caller);
        // Chat chat = getChat(user, name);
        // chat.sendMessage(message);
        // String result = null;
        // ContactEntry ce = user.imContacts.get(name);
        // if (ce == null || !ce.online) {
        //     result = "m.im_offline";
        // }
        // listener.requestProcessed(result);
    }

    // /**
    //  * Attempts to reconnect (if allowed) if we lose connection unexpectedly.
    //  */
    // protected void attemptReconnect ()
    // {
    //     if (_conn.isConnected() || !_conn.getConfiguration().isReconnectionAllowed()) {
    //         return;
    //     }
    //     _reconnectInterval = new Interval(_omgr) {
    //         public void expired () {
    //             try {
    //                 _conn.reconnect();
    //             } catch (XMPPException e) {
    //                 log.warning("Unable to reconnect to jabber server [host=" +
    //                         _conn.getConfiguration().getHost() + ", error=" +
    //                         e + ", cause=" + e.getCause() + "].");
    //                 attemptReconnect();
    //                 return;
    //             }
    //             _reconnectInterval.cancel();
    //             _reconnectInterval = null;
    //             handshake();
    //         }
    //     };
    //     _reconnectInterval.schedule(RECONNECT_TIMEOUT);
    // }

    // /**
    //  * Handshakes with the server after connection.
    //  */
    // protected void handshake ()
    // {
    //     _conn.addConnectionListener(this);
    //     String secret = ServerConfig.config.getValue("jabber.secret", "");
    //     _conn.handshake(secret, new ResultListener<Object>() {
    //         public void requestCompleted (Object result) {
    //             onAuthorization();
    //         }

    //         public void requestFailed (Exception cause) {
    //             log.warning("Failed handshake with jabber server [error=" + cause + ", cause=" +
    //                 cause.getCause() + "].");
    //             _conn.disconnect();
    //         }
    //     });
    // }

    // protected void onAuthorization ()
    // {
    //     log.info("Successfully authorized on Jabber Server [host=" +
    //             _conn.getConfiguration().getHost() + "].");
    //     String gatewayList = ServerConfig.config.getValue("jabber.gateways", "");
    //     _gateways.clear();
    //     for (String gateway : StringUtil.split(gatewayList, ",")) {
    //         _gateways.add(gateway);
    //     }
    //     if (_chatListener != null) {
    //         return;
    //     }
    //     _chatListener = new ChatManagerListener() {
    //         public void chatCreated (Chat chat, boolean createdLocally) {
    //             if (createdLocally) {
    //                 return;
    //             }
    //             if (DEBUG) {
    //                 log.info("Remote chat creation [from=" + chat.getParticipant() + ", to=" +
    //                         chat.getUser() + "].");
    //             }
    //             JabberUser juser = _users.get(fromJID(chat.getUser()));
    //             if (juser == null) {
    //                 log.warning("No user found for incoming chat");
    //                 return;
    //             }
    //             if (juser.chats == null) {
    //                 juser.chats = Maps.newHashMap();
    //             }
    //             if (juser.messageListener == null) {
    //                 juser.messageListener = new UserMessageListener(juser);
    //             }
    //             chat.addMessageListener(juser.messageListener);
    //             juser.chats.put(new JabberName(
    //                         StringUtils.parseBareAddress(chat.getParticipant())), chat);
    //         }
    //     };
    //     _conn.getChatManager().addChatListener(_chatListener);
    // }

    // /**
    //  * Cleans up user information on disconnection.
    //  */
    // protected void cleanup ()
    // {
    //     for (Name name : _users.keySet()) {
    //         logoffUser(name);
    //     }
    // }

    // protected void loginUser (MemberObject user, String gateway, String username)
    // {
    //     String ujid = getJID(user);
    //     createPacketResponder(user);
    //     Presence presence = new Presence(Presence.Type.available);
    //     presence.setFrom(ujid);
    //     presence.setTo(gateway + "." + _conn.getServiceName());
    //     _conn.packetWriter.sendPacket(presence);
    //     if (DEBUG) {
    //         log.info("Jabber login [uJID=" + ujid +
    //                 ", to=" + gateway + "." + _conn.getServiceName() + "].");
    //     }

    //     GatewayEntry gent = user.gateways.get(gateway);
    //     if (gent == null) {
    //         gent = new GatewayEntry(gateway, true, username);
    //         user.addToGateways(gent);
    //     } else {
    //         gent.online = true;
    //         gent.username = username;
    //         user.updateGateways(gent);
    //     }
    // }

    // protected void logoffUser (Name name)
    // {
    //     JabberUser juser = _users.get(name);
    //     if (juser == null || juser.user == null) {
    //         return;
    //     }
    //     juser.user.startTransaction();
    //     try {
    //         for (GatewayEntry gent : juser.user.gateways) {
    //             if (!gent.online) {
    //                 continue;
    //             }
    //             logoffUser(juser.user, gent.gateway);
    //         }
    //     } finally {
    //         juser.user.commitTransaction();
    //     }
    // }

    // protected void logoffUser (MemberObject user, String gateway)
    // {
    //     String uJID = getJID(user);
    //     if (DEBUG) {
    //         log.info("Jabber logoff [uJID=" + uJID +
    //                 ", to=" + gateway + "." + _conn.getServiceName() + "].");
    //     }
    //     if (_conn.isConnected()) {
    //         Registration reg = new Registration();
    //         reg.setTo(gateway + "." + _conn.getServiceName());
    //         reg.setFrom(uJID);
    //         reg.setType(IQ.Type.SET);
    //         Map<String, String> map = Maps.newHashMap();
    //         map.put("remove", "");
    //         reg.setAttributes(map);
    //         _conn.packetWriter.sendPacket(reg);
    //     }
    //     GatewayEntry gent = user.gateways.get(gateway);
    //     if (gent != null) {
    //         gent.online = false;
    //         gent.username = null;
    //         user.updateGateways(gent);
    //     }
    // }

    // protected String getJID (MemberObject user)
    // {
    //     return getJID(user.username);
    // }

    // protected String getJID (Name name)
    // {
    //     return StringUtils.escapeNode(name.getNormal()) + "@" + ServerConfig.nodeName +
    //             "." + _conn.getServiceName() + "/" + ServerConfig.nodeName;
    // }

    // protected Name fromJID (String jid)
    // {
    //     return new Name(StringUtils.unescapeNode(StringUtils.parseName(jid)));
    // }

    // protected void createPacketResponder (MemberObject user)
    // {
    //     JabberUser juser = getJabberUser(user, true);
    //     if (juser.packetResponder == null) {
    //         juser.packetResponder = new UserPacketResponder(user);
    //         _conn.addPacketListener(juser.packetResponder,
    //                 new ToContainsFilter(StringUtils.parseBareAddress(getJID(user))));
    //     }
    // }

    // protected void removePacketResponder (MemberObject user)
    // {
    //     JabberUser juser = getJabberUser(user, false);
    //     if (juser != null && juser.packetResponder != null) {
    //         _conn.removePacketListener(juser.packetResponder);
    //         juser.packetResponder = null;
    //     }
    // }

    // protected Chat getChat (MemberObject user, JabberName name)
    // {
    //     JabberUser juser = getJabberUser(user, true);
    //     if (juser.chats == null) {
    //         juser.chats = Maps.newHashMap();
    //     }
    //     Chat chat = juser.chats.get(name);
    //     if (chat == null) {
    //         if (juser.messageListener == null) {
    //             juser.messageListener = new UserMessageListener(juser);
    //         }
    //         chat = _conn.getChatManager().createChat(
    //                 getJID(user), name.toJID(), juser.messageListener);
    //         juser.chats.put(name, chat);
    //     }
    //     return chat;
    // }

    // protected JabberUser getJabberUser (MemberObject user, boolean create)
    // {
    //     JabberUser juser = _users.get(user.username);
    //     if (juser == null && create) {
    //         juser = new JabberUser(user);
    //         _users.put(user.username, juser);
    //     }
    //     return juser;
    // }

    // protected class UserPacketResponder extends PacketResponder
    // {
    //     public UserPacketResponder (MemberObject user)
    //     {
    //         super(_conn.queue);
    //         _user = user;
    //         _user.setImContacts(new DSet<ContactEntry>());
    //     }

    //     @Override // from PacketResponder
    //     public void handlePacket (Packet packet)
    //     {
    //         if (DEBUG) {
    //             log.info("incoming jabber packet [user=" + _user.username +
    //                     ", xml=" + packet.toXML() + "].");
    //         }
    //         String from = packet.getFrom();
    //         if (!from.endsWith("." + _conn.getServiceName())) {
    //             return;
    //         }
    //         String gateway = StringUtils.parseServer(from);
    //         gateway = gateway.substring(0, gateway.indexOf("."));
    //         if (!_user.gateways.containsKey(gateway)) {
    //             return;
    //         }
    //         if (packet instanceof Presence) {
    //             handlePresence((Presence)packet, gateway);
    //         } else if (packet instanceof RosterPacket) {
    //             handleRoster((RosterPacket)packet, gateway);
    //         }
    //     }

    //     protected void handlePresence (Presence presence, String gateway)
    //     {
    //         if (StringUtil.isBlank(StringUtils.parseName(presence.getFrom()))) {
    //             return;
    //         }

    //         JabberName jname = new JabberName(presence.getFrom());
    //         ContactEntry entry = _user.imContacts.get(jname);
    //         if (presence.getType() == Presence.Type.available ||
    //                 presence.getType() == Presence.Type.unavailable) {
    //             boolean online = presence.getType() == Presence.Type.available;
    //             if (entry == null) {
    //                 entry = new ContactEntry(jname, online);
    //                 _user.addToImContacts(entry);
    //             } else if (online != entry.online) {
    //                 entry.online = online;
    //                 _user.updateImContacts(entry);
    //             }
    //         }
    //     }

    //     protected void handleRoster (RosterPacket roster, String gateway)
    //     {
    //         _user.startTransaction();
    //         try {
    //             for (RosterPacket.Item item : roster.getRosterItems()) {
    //                 if (StringUtil.isBlank(StringUtils.parseName(item.getUser()))) {
    //                     continue;
    //                 }
    //                 JabberName jname = new JabberName(
    //                     item.getUser(), StringUtil.getOr(item.getName(), null));
    //                 ContactEntry entry = _user.imContacts.get(jname);
    //                 if (entry == null) {
    //                     entry = new ContactEntry(jname, false);
    //                     _user.addToImContacts(entry);
    //                 } else if (item.getName() != null &&
    //                         !item.getName().equals(entry.name.getDisplayName())) {
    //                     entry.name = jname;
    //                     _user.updateImContacts(entry);
    //                 }
    //             }
    //         } finally {
    //             _user.commitTransaction();
    //         }
    //     }

    //     protected MemberObject _user;
    // }

    // protected class UserMessageListener
    //     implements MessageListener
    // {
    //     public UserMessageListener (JabberUser juser)
    //     {
    //         _user = juser.user;
    //     }

    //     // from MessageListener
    //     public void processMessage (Chat chat, Message message)
    //     {
    //         String from = chat.getParticipant();
    //         if (StringUtil.isBlank(StringUtils.parseName(from))) {
    //             SpeakUtil.sendFeedback(_user, MessageManager.GLOBAL_BUNDLE,
    //                     MessageBundle.taint(message.getBody()));
    //             return;
    //         }
    //         JabberName jname = new JabberName(from);
    //         ContactEntry entry = _user.imContacts.get(jname);
    //         if (entry != null) {
    //             jname = entry.name;
    //         }
    //         _chatprov.deliverTell(_user, UserMessage.create(jname, message.getBody()));
    //     }

    //     protected MemberObject _user;
    // }

    // protected class JabberUser
    // {
    //     public MemberObject user;
    //     public UserPacketResponder packetResponder;
    //     public UserMessageListener messageListener;
    //     public Map<JabberName, Chat> chats;

    //     public JabberUser (MemberObject user)
    //     {
    //         this.user = user;
    //     }
    // }

    // /** Reference to our XMPP connection. */
    // protected XMPPConnection _conn;

    // /** Our chat listener. */
    // protected ChatManagerListener _chatListener;

    // /** Our interval for reconnecting to the jabber server. */
    // protected Interval _reconnectInterval;

    // /** Mapping of username to runtime jabber information for that user. */
    // protected Map<Name, JabberUser> _users = Maps.newHashMap();

    // /** The available gateways. */
    // protected List<String> _gateways = Lists.newArrayList();

    // // dependencies
    // @Inject protected ChatProvider _chatprov;
    // @Inject protected MemberLocator _locator;
    // @Inject protected RootDObjectManager _omgr;

    /** The time between reconnection attempts. */
    protected static final long RECONNECT_TIMEOUT = 60 * 1000L;
}
