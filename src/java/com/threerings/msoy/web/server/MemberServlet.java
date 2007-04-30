//
// $Id$

package com.threerings.msoy.web.server;

import java.io.StringWriter;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.samskivert.io.PersistenceException;
import com.samskivert.net.MailUtil;
import com.samskivert.velocity.VelocityUtil;

import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.VelocityEngine;

import com.threerings.msoy.server.MsoyServer;
import com.threerings.msoy.server.ServerConfig;
import com.threerings.msoy.server.persist.GroupMembershipRecord;
import com.threerings.msoy.server.persist.GroupRecord;
import com.threerings.msoy.server.persist.InvitationRecord;
import com.threerings.msoy.server.persist.MemberRecord;
import com.threerings.msoy.server.persist.NeighborFriendRecord;

import com.threerings.msoy.web.client.MemberService;
import com.threerings.msoy.data.all.GroupName;
import com.threerings.msoy.data.all.MemberName;
import com.threerings.msoy.web.data.ServiceException;
import com.threerings.msoy.web.data.WebCreds;
import com.threerings.msoy.web.data.MemberInvites;
import com.threerings.msoy.web.data.InvitationResults;

import com.threerings.msoy.data.Neighborhood;
import com.threerings.msoy.data.UserAction;
import com.threerings.msoy.data.Neighborhood.NeighborGroup;
import com.threerings.msoy.data.Neighborhood.NeighborMember;
import com.threerings.msoy.item.data.all.Item;
import com.threerings.msoy.item.data.all.MediaDesc;
import com.threerings.msoy.world.data.MsoySceneModel;
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
        return waiter.waitForResult();
    }

    // from MemberService
    public String serializePopularPlaces (WebCreds creds, int n)
        throws ServiceException
    {
        ServletWaiter<String> waiter =
            new ServletWaiter<String>("serializePopularPlaces[" + n + "]");
        MsoyServer.memberMan.serializePopularPlaces(n, waiter);
        return waiter.waitForResult();
    }

    // from MemberService
    public String serializeNeighborhood (WebCreds creds, int id, final boolean forGroup)
        throws ServiceException
    {
        final ServletWaiter<String> waiter =
            new ServletWaiter<String>("serializeNeighborhood[" + id + ", " + forGroup + "]");

        final Neighborhood hood;
        try {
            hood = forGroup ? getGroupNeighborhood(id) : getMemberNeighborhood(id);
             if (hood == null) {
                 return null;
             }
        } catch (PersistenceException e) {
            log.log(Level.WARNING, "Failed to create neighborhood [id=" + id +
                    ", forGroup=" + forGroup + "]", e);
            throw new ServiceException(InvocationCodes.INTERNAL_ERROR);
        }

        MsoyServer.omgr.postRunnable(new Runnable() {
            public void run () {
                try {
                    finalizeNeighborhood(hood);
                    String data = URLEncoder.encode(toJSON(hood).toString(), "UTF-8");
                    waiter.requestCompleted(data);
                } catch (Exception e) {
                    waiter.requestFailed(e);
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
            ArrayList<String> emails = new ArrayList<String>();
            for (InvitationRecord iRec : MsoyServer.memberRepo.loadPendingInvites(memberId)) {
                emails.add(iRec.inviteeEmail);
            }
            result.pendingInvitations = emails;
            return result;
        } catch (PersistenceException pe) {
            log.log(Level.WARNING, "getInvitationsStatus failed [id=" + memberId +"]", pe);
            throw new ServiceException(ServiceException.INTERNAL_ERROR);
        }
    }

    // from MemberService
    @SuppressWarnings("unchecked")
    public InvitationResults sendInvites (WebCreds creds, List addresses, String customMessage) 
        throws ServiceException
    {
        int memberId = getMemberId(creds);

        InvitationResults results = new InvitationResults();

        VelocityEngine ve;
        try {
            ve = VelocityUtil.createEngine();
        } catch (Exception e) {
            log.log(Level.WARNING, "Failed to create the velocity engine.", e);
            throw new ServiceException(ServiceException.INTERNAL_ERROR);
        }

        int port = ServerConfig.getHttpPort();
        String host = ServerConfig.serverHost + (port != 80 ? ":" + port : "");

        try {
            for (String email : (List<String>)addresses) {
                // make sure this user still has available invites
                if (MsoyServer.memberRepo.getInvitesGranted(memberId) <= 0) {
                    // silently stop trying to send addresses, since we already check this value
                    // in GWT land, and deal with it sensibly there.
                    break;
                }

                // make sure this address is valid
                if (!MailUtil.isValidAddress(email)) {
                    if (results.invalid == null) {
                        results.invalid = new ArrayList();
                    }
                    results.invalid.add(email);
                    continue;
                }

                // make sure this address isn't already registered
                if (MsoyServer.memberRepo.loadMember(email) != null) {
                    if (results.alreadyRegistered == null) {
                        results.alreadyRegistered = new ArrayList();
                    }
                    results.alreadyRegistered.add(email);
                    continue;
                }

                // make sure this user hasn't already invited this address
                if (MsoyServer.memberRepo.loadInvite(email, memberId) != null) {
                    if (results.alreadyInvited == null) {
                        results.alreadyInvited = new ArrayList();
                    }
                    results.alreadyInvited.add(email);
                    continue;
                }

                // find a free invite id
                String inviteId;
                int tries = 0;
                while (MsoyServer.memberRepo.loadInvite(inviteId = randomInviteId()) != null) {
                    tries++;
                }
                if (tries > 5) {
                    log.log(Level.WARNING, "InvitationRecord.inviteId space is getting " +
                        "saturated, it took " + tries + " tries to find a free id");
                }

                // create and send the invitation
                VelocityContext ctx = new VelocityContext();
                ctx.put("custom_message", customMessage);
                ctx.put("server_host", host);
                ctx.put("invite_id", inviteId);
                StringWriter sw = new StringWriter();
                try {
                    ve.mergeTemplate("rsrc/email/memberInvite.tmpl", "UTF-8", ctx, sw);
                    String body = sw.toString();
                    int nidx = body.indexOf("\n"); // first line is the subject
                    MailUtil.deliverMail(email, INVITE_FROM, body.substring(0, nidx),
                        body.substring(nidx+1));
                } catch (Exception e) {
                    e.printStackTrace();
                    if (results.failed == null) {
                        results.failed = new ArrayList();
                    }
                    results.failed.add(email + e.getMessage());
                    continue;
                }
                MsoyServer.memberRepo.addInvite(email, memberId, inviteId);
                if (results.successful == null) {
                    results.successful = new ArrayList();
                }
                results.successful.add(email);
            }
        } catch (PersistenceException pe) {
            log.log(Level.WARNING, "sendInvites failed.", pe);
            throw new ServiceException(ServiceException.INTERNAL_ERROR);
        }

        return results;
    }

    /**
     * Constructs and returns a {@link Neighborhood} record for a given member.
     */
    protected Neighborhood getMemberNeighborhood (int memberId)
        throws PersistenceException
    {
        Neighborhood hood = new Neighborhood();
        // first load the center member data
        MemberRecord mRec = MsoyServer.memberRepo.loadMember(memberId);
        if (mRec == null) {
            return null;
        }
        hood.member = makeNeighborMember(mRec);

        // then all the data for the groups
        Collection<GroupMembershipRecord> gmRecs = MsoyServer.groupRepo.getMemberships(memberId);
        int[] groupIds = new int[gmRecs.size()];
        int ii = 0;
        for (GroupMembershipRecord gmRec : gmRecs) {
            groupIds[ii ++] = gmRec.groupId;
        }
        List<NeighborGroup> nGroups = new ArrayList<NeighborGroup>();
        for (GroupRecord gRec : MsoyServer.groupRepo.loadGroups(groupIds)) {
            nGroups.add(makeNeighborGroup(gRec));
        }
        hood.neighborGroups = nGroups.toArray(new NeighborGroup[0]);

        // finally the friends
        List<NeighborMember> members = new ArrayList<NeighborMember>();
        for (NeighborFriendRecord fRec : MsoyServer.memberRepo.getNeighborhoodFriends(memberId)) {
            members.add(makeNeighborMember(fRec));
        }
        hood.neighborMembers = members.toArray(new NeighborMember[0]);
        return hood;
    }

    /**
     * Constructs and returns a {@link Neighborhood} record for a given group.
     */
    protected Neighborhood getGroupNeighborhood (int groupId)
        throws PersistenceException
    {
        Neighborhood hood = new Neighborhood();
        // first load the center group data
        GroupRecord gRec = MsoyServer.groupRepo.loadGroup(groupId);
        if (gRec == null) {
            // if there is no such group, there is no neighborhood
            return null;
        }
        hood.group = makeNeighborGroup(gRec);
        hood.group.members = MsoyServer.groupRepo.countMembers(groupId);

        // we have no other groups
        hood.neighborGroups = new NeighborGroup[0];

        // but we're including all the group's members, so load'em
        Collection<GroupMembershipRecord> gmRecs = MsoyServer.groupRepo.getMembers(groupId);
        int[] memberIds = new int[gmRecs.size()];
        int ii = 0;
        for (GroupMembershipRecord gmRec : gmRecs) {
            memberIds[ii ++] = gmRec.memberId;
        }

        List<NeighborMember> members = new ArrayList<NeighborMember>();
        for (NeighborFriendRecord fRec : MsoyServer.memberRepo.getNeighborhoodMembers(memberIds)) {
            members.add(makeNeighborMember(fRec));
        }
        hood.neighborMembers = members.toArray(new NeighborMember[0]);
        return hood;
    }

    protected NeighborGroup makeNeighborGroup (GroupRecord gRec) throws PersistenceException
    {
        NeighborGroup nGroup = new NeighborGroup();
        nGroup.group = new GroupName(gRec.name, gRec.groupId);
        nGroup.homeSceneId = gRec.homeSceneId;
        if (gRec.logoMediaHash != null) {
            nGroup.logo = new MediaDesc(gRec.logoMediaHash, gRec.logoMimeType);
        }
        nGroup.members = MsoyServer.groupRepo.countMembers(gRec.groupId);
        return nGroup;
    }

    // convert a {@link NeighborFriendRecord} to a {@link NeighborMember}.
    protected NeighborMember makeNeighborMember (NeighborFriendRecord fRec)
    {
        NeighborMember nFriend = new NeighborMember();
        nFriend.member = new MemberName(fRec.name, fRec.memberId);
        nFriend.created = new Date(fRec.created.getTime());
        nFriend.flow = fRec.flow;
        nFriend.homeSceneId = fRec.homeSceneId;
        nFriend.lastSession = fRec.lastSession;
        nFriend.sessionMinutes = fRec.sessionMinutes;
        nFriend.sessions = fRec.sessions;
        return nFriend;
    }

    // convert a {@link MemberRecord} to a {@link NeighborMember}.
    protected NeighborMember makeNeighborMember (MemberRecord mRec)
    {
        NeighborMember nFriend = new NeighborMember();
        nFriend.member = new MemberName(mRec.name, mRec.memberId);
        nFriend.created = new Date(mRec.created.getTime());
        nFriend.flow = mRec.flow;
        nFriend.homeSceneId = mRec.homeSceneId;
        nFriend.lastSession = mRec.lastSession;
        nFriend.sessionMinutes = mRec.sessionMinutes;
        nFriend.sessions = mRec.sessions;
        return nFriend;
    }

    /**
     * Figures out the population of the various rooms associated with a neighborhood; a group's
     * and a member's home scenes. This must be called on the dobj thread.
     */
    protected void finalizeNeighborhood (Neighborhood hood)
    {
        if (hood.member != null) {
            finalizeEntity(hood.member);
        }
        if (hood.group != null) {
            finalizeEntity(hood.group);
        }
        for (NeighborGroup group : hood.neighborGroups) {
            finalizeEntity(group);
        }
        for (NeighborMember friend : hood.neighborMembers) {
            finalizeEntity(friend);
        }
    }

    /** Sets the population of a neighbour group. */
    protected void finalizeEntity (NeighborGroup group)
    {
        MsoyServer.memberMan.fillIn(group);
    }

    /** Sets the population of a neighbour friend and figure out if it's online. */
    protected void finalizeEntity (NeighborMember friend)
    {
        int memberId = friend.member.getMemberId();
        MsoyServer.memberMan.fillIn(friend);
        friend.isOnline = MsoyServer.lookupMember(memberId) != null;
    }

    /** Performs handcrafted JSON serialization, to minimize the overhead. */
    protected JSONObject toJSON (Neighborhood hood)
        throws JSONException
    {
        JSONObject obj = new JSONObject();
        if (hood.member != null) {
            obj.put("member", toJSON(hood.member));
        }
        if (hood.group != null) {
            obj.put("group", toJSON(hood.group));
        }
        JSONArray jArr = new JSONArray();
        for (NeighborMember friend : hood.neighborMembers) {
            jArr.put(toJSON(friend));
        }
        obj.put("friends", jArr);
        jArr = new JSONArray();
        for (NeighborGroup group : hood.neighborGroups) {
            jArr.put(toJSON(group));
        }
        obj.put("groups", jArr);
        return obj;
    }

    protected JSONObject toJSON (NeighborMember member)
        throws JSONException
    {
        JSONObject obj = new JSONObject();
        obj.put("name", member.member.toString());
        obj.put("id", member.member.getMemberId());
        obj.put("isOnline", member.isOnline);
        obj.put("pop", member.popCount);
        if (member.popSet != null) {
            obj.put("peeps", toJSON(member.popSet));
        }
        obj.put("created", member.created.getTime());
        obj.put("sNum", member.sessions);
        obj.put("sMin", member.sessionMinutes);
        obj.put("lastSess", member.lastSession.getTime());
        return obj;
    }

    protected JSONObject toJSON (NeighborGroup group)
        throws JSONException
    {
        JSONObject obj = new JSONObject();
        obj.put("name", group.group.toString());
        obj.put("id", group.group.getGroupId());
        obj.put("members", group.members);
        obj.put("pop", group.popCount);
        if (group.popSet != null) {
            obj.put("peeps", toJSON(group.popSet));
        }
        if (group.logo != null) {
            obj.put("logo", group.logo.toString());
        }
        return obj;
    }

    protected <T> JSONArray toJSON (Set<T> set)
    {
        JSONArray arr = new JSONArray();
        for (T bit : set) {
            arr.put(bit);
        }
        return arr;
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

    protected static final String INVITE_FROM = "peas@whirled.com";
    protected static final int INVITE_ID_LENGTH = 10;
    protected static final String INVITE_ID_CHARACTERS = "ABCDEFGHIJKLMNOPQRSTUVWXYZ01234567890";
}
