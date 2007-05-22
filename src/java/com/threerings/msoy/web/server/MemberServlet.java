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

import com.threerings.util.MessageBundle;

import com.threerings.msoy.server.MsoyServer;
import com.threerings.msoy.server.ServerConfig;
import com.threerings.msoy.server.persist.InvitationRecord;
import com.threerings.msoy.server.persist.MemberRecord;

import com.threerings.msoy.web.client.MemberService;
import com.threerings.msoy.data.all.FriendEntry;
import com.threerings.msoy.data.all.MemberName;
import com.threerings.msoy.web.data.ServiceException;
import com.threerings.msoy.web.data.WebCreds;
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
    public boolean getFriendStatus (WebCreds creds, final int memberId)
        throws ServiceException
    {
        try {
            return MsoyServer.memberRepo.getFriendStatus(getMemberId(creds), memberId);
        } catch (PersistenceException pe) {
            log.log(Level.WARNING, "isFriend failed [memberId=" + memberId + "].", pe);
            throw new ServiceException(ServiceException.INTERNAL_ERROR);
        }
    }

    // from MemberService
    public void addFriend (final WebCreds creds, final int friendId)
        throws ServiceException
    {
        final MemberRecord memrec = requireAuthedUser(creds);
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
    public void removeFriend (final WebCreds creds, final int friendId)
        throws ServiceException
    {
        final MemberRecord memrec = requireAuthedUser(creds);
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
    public ArrayList loadInventory (final WebCreds creds, final byte type)
        throws ServiceException
    {
        final MemberRecord memrec = requireAuthedUser(creds);

        // convert the string they supplied to an item enumeration
        if (Item.getClassForType(type) == null) {
            log.warning("Requested to load inventory for invalid item type " +
                        "[who=" + creds + ", type=" + type + "].");
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
    public String serializePopularPlaces (final WebCreds creds, final int n)
        throws ServiceException
    {
        // if we're logged on, fetch our friends list, on the servlet thread
        final List<FriendEntry> friends;
        if (creds != null) {
            try {
                friends = MsoyServer.memberRepo.getFriends(creds.getMemberId());
            } catch (PersistenceException e) {
                log.log(Level.WARNING, "Failed to list friends");
                throw new ServiceException(InvocationCodes.INTERNAL_ERROR);
            }
        } else {
            friends = new ArrayList<FriendEntry>();
        }

        final ServletWaiter<String> waiter =
            new ServletWaiter<String>("serializePopularPlaces[" + n + "]");

        // then proceed to the dobj thread to get runtime state
        MsoyServer.omgr.postRunnable(new Runnable() {
            public void run () {
                try {
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

                    for (Map.Entry<PopularPlace, Set<MemberName>> entry : popSets.entrySet()) {
                        filePopularPlace(
                            creds.name, entry.getKey(), entry.getValue(), homes, groups, games);
                    }

                    // after we've enumerated our friends, we add in the top populous places too 
                    int n = 3; // TODO: totally ad-hoc
                    for (PopularPlace place : MsoyServer.memberMan.getPPCache().getTopPlaces()) {
                        // make sure we didn't already include this place in the code above
                        if (popSets.containsKey(place)) {
                            continue;
                        }
                        filePopularPlace(creds.name, place, null, homes, groups, games);
                        if (--n <= 0) {
                            break;
                        }
                    }

                    JSONObject result = new JSONObject();
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
    public MemberInvites getInvitationsStatus (WebCreds creds) 
        throws ServiceException
    {
        int memberId = getMemberId(creds);

        try {
            MemberInvites result = new MemberInvites();
            result.availableInvitations = MsoyServer.memberRepo.getInvitesGranted(memberId);
            ArrayList<Invitation> pending = new ArrayList<Invitation>();
            for (InvitationRecord iRec : MsoyServer.memberRepo.loadPendingInvites(memberId)) {
                pending.add(iRec.toInvitationObject());
            }
            result.pendingInvitations = pending;
            int port = ServerConfig.getHttpPort();
            result.serverUrl = "http://" + ServerConfig.serverHost + 
                (port != 80 ? ":" + port : "") + "/#invite-";
            return result;
        } catch (PersistenceException pe) {
            log.log(Level.WARNING, "getInvitationsStatus failed [id=" + memberId +"]", pe);
            throw new ServiceException(ServiceException.INTERNAL_ERROR);
        }
    }

    // from MemberService
    public InvitationResults sendInvites (WebCreds creds, List addresses, String customMessage) 
        throws ServiceException
    {
        int memberId = getMemberId(creds);

        // make sure this user still has available invites
        try {
            // we already check this value in GWT land, and deal with it sensibly there
            if (MsoyServer.memberRepo.getInvitesGranted(memberId) < addresses.size()) {
                throw new ServiceException(ServiceException.INTERNAL_ERROR);
            }
        } catch (PersistenceException pe) {
            log.log(Level.WARNING, "getInvitesGranted failed [id=" + memberId +"]", pe);
            throw new ServiceException(ServiceException.INTERNAL_ERROR);
        }

        InvitationResults ir = new InvitationResults();
        ir.results = new String[addresses.size()];
        for (int ii = 0; ii < addresses.size(); ii++) {
            String email = (String)addresses.get(ii);
            ir.results[ii] = sendInvite(memberId, email, customMessage);
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

    protected void filePopularPlace (MemberName who, PopularPlace place, Set<MemberName> friends,
                                     JSONArray homes, JSONArray groups, JSONArray games)
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
        if (place instanceof PopularGamePlace) {
            games.put(obj);
        } else {
            obj.put("sceneId", ((PopularScenePlace) place).getSceneId());
            if (place instanceof PopularMemberPlace) {
                homes.put(obj);
            } else {
                groups.put(obj);
            }
        }
    }

    /**
     * Helper function for {@link #sendInvites}.
     */
    protected String sendInvite (int memberId, String email, String customMessage)
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
            if (MsoyServer.memberRepo.loadInvite(email, memberId) != null) {
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
            ctx.put("custom_message", customMessage);
            ctx.put("invite_id", inviteId);
            ctx.put("server_url", ServerConfig.getServerURL());

            try {
                sendEmail(email, ServerConfig.getFromAddress(), "memberInvite", ctx);
            } catch (Exception e) {
                return e.getMessage();
            }

            MsoyServer.memberRepo.addInvite(email, memberId, inviteId);
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
