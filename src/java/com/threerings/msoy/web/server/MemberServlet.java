//
// $Id$

package com.threerings.msoy.web.server;

import java.net.URLEncoder;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.samskivert.io.PersistenceException;
import com.samskivert.net.MailUtil;

import org.apache.velocity.VelocityContext;

import com.threerings.msoy.server.MsoyServer;
import com.threerings.msoy.server.ServerConfig;
import com.threerings.msoy.server.persist.GroupRecord;
import com.threerings.msoy.server.persist.InvitationRecord;
import com.threerings.msoy.server.persist.MemberRecord;
import com.threerings.msoy.server.util.MailSender;

import com.threerings.msoy.web.client.MemberService;
import com.threerings.msoy.chat.data.ChatChannel;
import com.threerings.msoy.data.all.FriendEntry;
import com.threerings.msoy.data.all.GroupName;
import com.threerings.msoy.data.all.MemberName;
import com.threerings.msoy.web.data.ServiceException;
import com.threerings.msoy.web.data.WebIdent;
import com.threerings.msoy.web.data.MemberInvites;
import com.threerings.msoy.web.data.InvitationResults;
import com.threerings.msoy.web.data.Invitation;

import com.threerings.msoy.data.MemberObject;
import com.threerings.msoy.data.PopularPlace.PopularGamePlace;
import com.threerings.msoy.data.PopularPlace.PopularMemberPlace;
import com.threerings.msoy.data.PopularPlace.PopularScenePlace;
import com.threerings.msoy.data.PopularPlace;

import com.threerings.msoy.item.data.all.Item;
import com.threerings.presents.data.InvocationCodes;

import static com.threerings.msoy.Log.log;

/**
 * Provides the server implementation of {@link MemberService}.
 */
public class MemberServlet extends MsoyServiceServlet
    implements MemberService
{
    // from MemberService
    public boolean getFriendStatus (WebIdent ident, final int memberId)
        throws ServiceException
    {
        try {
            return MsoyServer.memberRepo.getFriendStatus(getMemberId(ident), memberId);
        } catch (PersistenceException pe) {
            log.log(Level.WARNING, "isFriend failed [memberId=" + memberId + "].", pe);
            throw new ServiceException(ServiceException.INTERNAL_ERROR);
        }
    }

    // from MemberService
    public void addFriend (final WebIdent ident, final int friendId)
        throws ServiceException
    {
        final MemberRecord memrec = requireAuthedUser(ident);
        final ServletWaiter<Void> waiter =
            new ServletWaiter<Void>("acceptFriend[" + friendId + "]");
        MsoyServer.omgr.postRunnable(new Runnable() {
            public void run () {
                MsoyServer.memberMan.alterFriend(memrec.memberId, friendId, true, waiter);
            }
        });
        waiter.waitForResult();
    }

    // from MemberService
    public void removeFriend (final WebIdent ident, final int friendId)
        throws ServiceException
    {
        final MemberRecord memrec = requireAuthedUser(ident);
        final ServletWaiter<Void> waiter =
            new ServletWaiter<Void>("removeFriend[" + friendId + "]");
        MsoyServer.omgr.postRunnable(new Runnable() {
            public void run () {
                MsoyServer.memberMan.alterFriend(memrec.memberId, friendId, false, waiter);
            }
        });
        waiter.waitForResult();
    }

    // from interface MemberService
    public List loadInventory (final WebIdent ident, final byte type)
        throws ServiceException
    {
        final MemberRecord memrec = requireAuthedUser(ident);

        // convert the string they supplied to an item enumeration
        if (Item.getClassForType(type) == null) {
            log.warning("Requested to load inventory for invalid item type " +
                        "[who=" + ident + ", type=" + type + "].");
            throw new ServiceException(ServiceException.INTERNAL_ERROR);
        }

        // load their inventory via the item manager
        final ServletWaiter<ArrayList<Item>> waiter = new ServletWaiter<ArrayList<Item>>(
            "loadInventory[" + memrec.memberId + ", " + type + "]");
        MsoyServer.omgr.postRunnable(new Runnable() {
            public void run () {
                MsoyServer.itemMan.loadInventory(memrec.memberId, type, waiter);
            }
        });
        ArrayList<Item> result = waiter.waitForResult();
        // when Item becomes a type-safe Comparable this Comparator can go away
        Collections.sort(result, new Comparator<Item>() {
            public int compare (Item one, Item two) {
                return one.compareTo(two);
            }
        });
        return result;
    }

    // from MemberService
    public String serializePopularPlaces (WebIdent ident, final int n)
        throws ServiceException
    {
        final MemberRecord mrec = getAuthedUser(ident);
        final MemberName name = (mrec == null) ? null : mrec.getName();

        // if we're logged on, fetch our friends
        final List<FriendEntry> friends;
        // and which groups we're members of
        final Set<GroupName> memberGroups = new HashSet<GroupName>();
        if (mrec != null) {
            try {
                friends = MsoyServer.memberRepo.getFriends(mrec.memberId);
                for (GroupRecord gRec : MsoyServer.groupRepo.getFullMemberships(mrec.memberId)) {
                    memberGroups.add(new GroupName(gRec.name, gRec.groupId));
                }

            } catch (PersistenceException e) {
                log.log(Level.WARNING, "Failed to list friends", e);
                throw new ServiceException(InvocationCodes.INTERNAL_ERROR);
            }
            friends.add(new FriendEntry(name, true));

        } else {
            friends = Collections.emptyList();
        }

        // then proceed to the dobj thread to get runtime state
        final ServletWaiter<String> waiter =
            new ServletWaiter<String>("serializePopularPlaces[" + n + "]");
        MsoyServer.omgr.postRunnable(new Runnable() {
            public void run () {
                JSONObject result = new JSONObject();

                try {
                    JSONArray channels = new JSONArray();
                    Iterable<ChatChannel> allChannels = MsoyServer.channelMan.getChatChannels();
                    int desiredChannels = 8;

                    // first add active channels we're members of
                    for (ChatChannel channel : allChannels) {
                        if (--desiredChannels < 0) {
                            break;
                        }
                        if (channel.type == ChatChannel.GROUP_CHANNEL &&
                            memberGroups.contains((GroupName) channel.ident)) {
                            JSONObject cObj = new JSONObject();
                            cObj.put("name", ((GroupName) channel.ident).toString());
                            cObj.put("id", ((GroupName) channel.ident).getGroupId());
                            channels.put(cObj);
                        }
                    }
                    // then fill in with the ones we're not members of, if needed
                    for (ChatChannel channel : allChannels) {
                        if (--desiredChannels < 0) {
                            break;
                        }
                        if (channel.type == ChatChannel.GROUP_CHANNEL &&
                            !memberGroups.contains((GroupName) channel.ident)) {
                            JSONObject cObj = new JSONObject();
                            cObj.put("name", ((GroupName) channel.ident).toString());
                            cObj.put("id", ((GroupName) channel.ident).getGroupId());
                            channels.put(cObj);
                        }
                    }

                    result.put("channels", channels);

                    Map<PopularPlace, Set<MemberName>> popSets =
                        new HashMap<PopularPlace, Set<MemberName>>();
                    // retrieve the location of each one of our online friends
                    for (FriendEntry entry : friends) {
                        MemberObject friend = MsoyServer.lookupMember(entry.name);
                        if (friend == null || friend.location == -1) {
                            continue;
                        }

                        // map the specific location to an owner (game, person, group) cluster
                        PopularPlace place = PopularPlace.getPopularPlace(
                            MsoyServer.plreg.getPlaceManager(friend.location));
                        if (place == null) {
                            continue;
                        }
                        Set<MemberName> set = popSets.get(place);
                        if (set == null) {
                            set = new HashSet<MemberName>();
                            popSets.put(place, set);
                        }
                        set.add(friend.memberName);
                    }

                    JSONArray homes = new JSONArray();
                    JSONArray groups = new JSONArray();
                    JSONArray games = new JSONArray();

                    // after we've enumerated our friends, we add in the top populous places too
                    int n = 3; // TODO: totally ad-hoc
                    for (PopularPlace place : MsoyServer.memberMan.getPPCache().getTopPlaces()) {
                        // make sure we didn't already include this place in the code above
                        if (popSets.containsKey(place)) {
                            continue;
                        }
                        popSets.put(place, null);
                        if (--n <= 0) {
                            break;
                        }
                    }

                    // if we're logged in and our home has people in it, pull it out so that we can
                    // set it as the "central" location; otherwise create a place for our home
                    PopularPlace home = null;
                    Set<MemberName> hfriends = null;
                    if (mrec != null) {
                        home = new PopularMemberPlace(mrec.memberId, mrec.homeSceneId);
                        hfriends = popSets.remove(home);
                    }

                    // now convert all these popular places into JSON bits
                    for (Map.Entry<PopularPlace, Set<MemberName>> entry : popSets.entrySet()) {
                        PopularPlace place = entry.getKey();
                        JSONObject obj = placeToJSON(name, place, entry.getValue());
                        if (place instanceof PopularGamePlace) {
                            games.put(obj);
                        } else if (place instanceof PopularScenePlace) {
                            obj.put("sceneId", ((PopularScenePlace)place).getSceneId());
                            if (place instanceof PopularMemberPlace) {
                                homes.put(obj);
                            } else {
                                groups.put(obj);
                            }
                        }
                    }

                    if (home != null) {
                        result.put("member", placeToJSON(name, home, hfriends));
                    }
                    result.put("friends", homes);
                    result.put("groups", groups);
                    result.put("games", games);
                    result.put("totpop", MsoyServer.memberMan.getPPCache().getPopulationCount());
                    waiter.requestCompleted(URLEncoder.encode(result.toString(), "UTF-8"));

                } catch (Exception e) {
                    waiter.requestFailed(e);
                    return;
                }
            }
        });
        return waiter.waitForResult();
    }

    // from MemberService
    public MemberInvites getInvitationsStatus (WebIdent ident)
        throws ServiceException
    {
        int memberId = getMemberId(ident);

        try {
            MemberInvites result = new MemberInvites();
            result.availableInvitations = MsoyServer.memberRepo.getInvitesGranted(memberId);
            ArrayList<Invitation> pending = new ArrayList<Invitation>();
            for (InvitationRecord iRec : MsoyServer.memberRepo.loadPendingInvites(memberId)) {
                pending.add(iRec.toInvitationObject());
            }
            result.pendingInvitations = pending;
            result.serverUrl = ServerConfig.getServerURL() + "/#invite-";
            return result;
        } catch (PersistenceException pe) {
            log.log(Level.WARNING, "getInvitationsStatus failed [id=" + memberId +"]", pe);
            throw new ServiceException(ServiceException.INTERNAL_ERROR);
        }
    }

    // from MemberService
    public InvitationResults sendInvites (WebIdent ident, List addresses, String customMessage)
        throws ServiceException
    {
        MemberRecord mrec = requireAuthedUser(ident);

        // make sure this user still has available invites
        try {
            // we already check this value in GWT land, and deal with it sensibly there
            if (MsoyServer.memberRepo.getInvitesGranted(mrec.memberId) < addresses.size()) {
                throw new ServiceException(ServiceException.INTERNAL_ERROR);
            }
        } catch (PersistenceException pe) {
            log.log(Level.WARNING, "getInvitesGranted failed [id=" + mrec.memberId +"]", pe);
            throw new ServiceException(ServiceException.INTERNAL_ERROR);
        }

        InvitationResults ir = new InvitationResults();
        ir.results = new String[addresses.size()];
        for (int ii = 0; ii < addresses.size(); ii++) {
            String email = (String)addresses.get(ii);
            ir.results[ii] = sendInvite(mrec, email, customMessage);
        }
        return ir;
    }

    // from MemberService
    public Invitation getInvitation (String inviteId, boolean viewing)
        throws ServiceException
    {
        try {
            InvitationRecord invRec = MsoyServer.memberRepo.loadInvite(inviteId, viewing);
            return (invRec == null) ? null : invRec.toInvitationObject();

        } catch (PersistenceException pe) {
            log.log(Level.WARNING, "getInvitation failed [inviteId=" + inviteId + "]", pe);
            throw new ServiceException(ServiceException.INTERNAL_ERROR);
        }
    }

    // from MemberService
    public void optOut (Invitation invite)
        throws ServiceException
    {
        try {
            if (!MsoyServer.memberRepo.inviteAvailable(invite.inviteId)) {
                throw new ServiceException(ServiceException.INTERNAL_ERROR);
            }

            MsoyServer.memberRepo.optOutInvite(invite);
        } catch (PersistenceException pe) {
            log.log(Level.WARNING, "optOut failed [inviteId=" + invite.inviteId + "]", pe);
            throw new ServiceException(ServiceException.INTERNAL_ERROR);
        }
    }

    protected JSONObject placeToJSON (MemberName who, PopularPlace place, Set<MemberName> friends)
        throws JSONException
    {
        JSONObject obj = new JSONObject();
        obj.put("name", place.getName());
        obj.put("pop", MsoyServer.memberMan.getPPCache().getPopulation(place));
        obj.put("id", place.getId());
        if (friends != null) {
            JSONArray arr = new JSONArray();
            for (MemberName bit : friends) {
                arr.put(bit.equals(who) ? "You" : bit.toString());
            }
            obj.put("friends", arr);
        }
        return obj;
    }

    /**
     * Helper function for {@link #sendInvites}.
     */
    protected String sendInvite (MemberRecord inviter, String email, String customMessage)
    {
        try {
            // make sure this address is valid
            if (!MailUtil.isValidAddress(email)) {
                return InvitationResults.INVALID_EMAIL;
            }

            // make sure this address isn't already registered
            if (MsoyServer.memberRepo.loadMember(email) != null) {
                return InvitationResults.ALREADY_REGISTERED;
            }

            // make sure this address isn't on the opt-out list
            if (MsoyServer.memberRepo.hasOptedOut(email)) {
                return InvitationResults.OPTED_OUT;
            }

            // make sure this user hasn't already invited this address
            if (MsoyServer.memberRepo.loadInvite(email, inviter.memberId) != null) {
                return InvitationResults.ALREADY_INVITED;
            }

            // find a free invite id
            String inviteId;
            int tries = 0;
            while (MsoyServer.memberRepo.loadInvite(inviteId = randomInviteId(), false) != null) {
                tries++;
            }
            if (tries > 5) {
                log.log(Level.WARNING, "InvitationRecord.inviteId space is getting " +
                        "saturated, it took " + tries + " tries to find a free id");
            }

            // create and send the invitation
            VelocityContext ctx = new VelocityContext();
            ctx.put("friend", inviter.name);
            ctx.put("custom_message", customMessage);
            ctx.put("invite_id", inviteId);
            ctx.put("server_url", ServerConfig.getServerURL());

            try {
                MailSender.sendEmail(email, inviter.accountName, "memberInvite", ctx);
            } catch (Exception e) {
                return e.getMessage();
            }

            MsoyServer.memberRepo.addInvite(email, inviter.memberId, inviteId);
            return InvitationResults.SUCCESS;

        } catch (PersistenceException pe) {
            log.log(Level.WARNING, "sendInvites failed.", pe);
            return ServiceException.INTERNAL_ERROR;
        }
    }

    protected String randomInviteId ()
    {
        String rand = "";
        for (int ii = 0; ii < INVITE_ID_LENGTH; ii++) {
            rand += INVITE_ID_CHARACTERS.charAt((int)(Math.random() *
                INVITE_ID_CHARACTERS.length()));
        }
        return rand;
    }

    protected static final int INVITE_ID_LENGTH = 10;
    protected static final String INVITE_ID_CHARACTERS = "ABCDEFGHIJKLMNOPQRSTUVWXYZ01234567890";
}
