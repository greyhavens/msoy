//
// $Id$

package com.threerings.msoy.server;

import java.util.Arrays;

import java.util.logging.Level;

import com.samskivert.io.PersistenceException;
import com.samskivert.jdbc.RepositoryUnit;
import com.samskivert.jdbc.RepositoryListenerUnit;

import com.samskivert.util.Interval;
import com.samskivert.util.Invoker;
import com.samskivert.util.ObjectUtil;
import com.samskivert.util.ResultListener;
import com.threerings.util.MessageBundle;

import com.threerings.presents.client.InvocationService;
import com.threerings.presents.data.ClientObject;
import com.threerings.presents.data.InvocationCodes;
import com.threerings.presents.dobj.AttributeChangeListener;
import com.threerings.presents.dobj.AttributeChangedEvent;
import com.threerings.presents.dobj.DSet;
import com.threerings.presents.server.InvocationException;
import com.threerings.presents.util.ConfirmAdapter;
import com.threerings.presents.util.PersistingUnit;

import com.threerings.crowd.data.OccupantInfo;
import com.threerings.crowd.chat.server.SpeakUtil;
import com.threerings.crowd.server.BodyProvider;
import com.threerings.crowd.server.PlaceManager;

import com.threerings.msoy.group.server.persist.GroupRecord;
import com.threerings.msoy.group.server.persist.GroupRepository;
import com.threerings.msoy.item.data.all.Avatar;
import com.threerings.msoy.item.data.all.Item;
import com.threerings.msoy.item.data.all.ItemIdent;
import com.threerings.msoy.notify.data.LevelUpNotification;
import com.threerings.msoy.notify.data.NotifyMessage;
import com.threerings.msoy.person.data.FriendInvitePayload;
import com.threerings.msoy.person.util.FeedMessageType;
import com.threerings.msoy.world.data.MsoySceneModel;

import com.threerings.msoy.data.MemberLocation;
import com.threerings.msoy.data.MemberObject;
import com.threerings.msoy.data.MsoyBodyObject;
import com.threerings.msoy.data.MsoyCodes;
import com.threerings.msoy.data.UserAction;
import com.threerings.msoy.data.all.FriendEntry;
import com.threerings.msoy.data.all.MemberName;
import com.threerings.msoy.server.persist.MemberFlowRecord;
import com.threerings.msoy.server.persist.MemberNameRecord;
import com.threerings.msoy.server.persist.MemberRecord;
import com.threerings.msoy.server.persist.MemberRepository;
import com.threerings.msoy.world.server.persist.SceneRecord;

import static com.threerings.msoy.Log.log;

/**
 * Manage msoy members.
 */
public class MemberManager
    implements MemberProvider
{
    public MemberManager ()
    {
        // intialize our internal array of memoized flow values per level.  Start with 256
        // calculated levels
        _levelForFlow = new int[256];
        for (int ii = 0; ii < BEGINNING_FLOW_LEVELS.length; ii++) {
            _levelForFlow[ii] = BEGINNING_FLOW_LEVELS[ii];
        }
        calculateLevelsForFlow(BEGINNING_FLOW_LEVELS.length);
    }

    /**
     * Prepares our member manager for operation.
     */
    public void init (MemberRepository memberRepo, GroupRepository groupRepo)
    {
        _memberRepo = memberRepo;
        _groupRepo = groupRepo;

        MsoyServer.invmgr.registerDispatcher(new MemberDispatcher(this), MsoyCodes.MEMBER_GROUP);

        _ppSnapshot = new PopularPlacesSnapshot();
        _ppInvalidator = new Interval(MsoyServer.omgr) {
            public void expired() {
                // the constructor computes a current popular places snapshot
                PopularPlacesSnapshot newSnapshot = new PopularPlacesSnapshot();
                synchronized(this) {
                    _ppSnapshot = newSnapshot;
                }
            }
        };
        _ppInvalidator.schedule(POP_PLACES_REFRESH_PERIOD, true);
    }

    /**
     * Returns the most recently generated popular places snapshot.
     */
    public PopularPlacesSnapshot getPPSnapshot ()
    {
        synchronized(this) {
            return _ppSnapshot;
        }
    }

    /**
     * Update the user's occupant info.
     */
    public void updateOccupantInfo (MemberObject user)
    {
        PlaceManager pmgr = MsoyServer.plreg.getPlaceManager(user.getPlaceOid());
        if (pmgr != null) {
            pmgr.updateOccupantInfo(user.createOccupantInfo(pmgr.getPlaceObject()));
        }
    }

    /**
     * Fetch the home ID for a member and return it.
     */
    public void getHomeId (final byte ownerType, final int ownerId, ResultListener<Integer> listener)
    {
        MsoyServer.invoker.postUnit(new RepositoryListenerUnit<Integer>(listener) {
            public Integer invokePersistResult () throws PersistenceException {
                switch (ownerType) {
                case MsoySceneModel.OWNER_TYPE_MEMBER:
                    MemberRecord member = _memberRepo.loadMember(ownerId);
                    return (member == null) ? null : member.homeSceneId;

                case MsoySceneModel.OWNER_TYPE_GROUP:
                    GroupRecord group = _groupRepo.loadGroup(ownerId);
                    return (group == null) ? null : group.homeSceneId;

                default:
                    log.warning("Unknown ownerType provided to getHomeId " +
                        "[ownerType=" + ownerType +
                        ", ownerId=" + ownerId + "].");
                    return null;
                }
            }
            public void handleSuccess () {
                if (_result == null) {
                    handleFailure(new InvocationException("m.no_such_user"));
                } else {
                    super.handleSuccess();
                }
            }
        });
    }

    /**
     * Called when a member logs onto this server.
     */
    public void memberLoggedOn (final MemberObject member)
    {
        if (member.isGuest()) {
            return;
        }

        //  add a listener for changes to accumulated flow so that the member's level can be
        // updated as necessary
        member.addListener(new AttributeChangeListener() {
            public void attributeChanged (AttributeChangedEvent event) {
                if (MemberObject.ACC_FLOW.equals(event.getName())) {
                    checkCurrentLevel(member);
                }
            }
        });

        // check their current level now in case they got flow while they were offline
        checkCurrentLevel(member);
    }

    // from interface MemberProvider
    public void inviteToBeFriend (ClientObject caller, int friendId,
                                  InvocationService.ConfirmListener lner)
        throws InvocationException
    {
        MemberObject user = (MemberObject) caller;
        ensureNotGuest(user);

        // in GWT land, we just send a mail message, so do the same here
        String subject = MsoyServer.msgMan.getBundle("server").get("m.friend_invite_subject");
        String body = MsoyServer.msgMan.getBundle("server").get("m.friend_invite_body");
        MsoyServer.mailMan.deliverMessage(
            user.getMemberId(), friendId, subject, body, new FriendInvitePayload(), false,
            new ConfirmAdapter(lner));
    }

    // from interface MemberProvider
    public void getHomeId (ClientObject caller, byte ownerType, int ownerId,
                           final InvocationService.ResultListener listener)
        throws InvocationException
    {
        ResultListener<Integer> rl = new ResultListener<Integer>() {
            public void requestCompleted (Integer result) {
                listener.requestProcessed(result);
            }
            public void requestFailed (Exception cause) {
                listener.requestFailed(cause.getMessage());
            }
        };
        getHomeId(ownerType, ownerId, rl);
    }

    // from interface MemberProvider
    public void getCurrentMemberLocation (ClientObject caller, int memberId,
                                          InvocationService.ResultListener listener)
        throws InvocationException
    {
        MemberObject user = (MemberObject) caller;

        // ensure that the other member is a full friend
        FriendEntry entry = user.friends.get(memberId);
        if (null == entry) {
            throw new InvocationException("e.not_a_friend");
        }

        MemberLocation memloc = MsoyServer.peerMan.getMemberLocation(memberId);
        if (memloc == null) {
            throw new InvocationException(MessageBundle.tcompose("e.not_online", entry.name));
        }
        listener.requestProcessed(memloc);
    }

    // from interface MemberProvider
    public void updateAvailability (ClientObject caller, int availability)
    {
        MemberObject user = (MemberObject) caller;
        user.setAvailability(availability);
    }

    // from interface MemberProvider
    public void inviteToFollow (ClientObject caller, int memberId,
                                InvocationService.ConfirmListener listener)
        throws InvocationException
    {
        MemberObject user = (MemberObject) caller;

        // if they want to clear their followers, do that
        if (memberId == 0) {
            for (MemberName follower : user.followers) {
                MemberObject fmo = MsoyServer.lookupMember(follower.getMemberId());
                if (fmo != null) {
                    fmo.setFollowing(null);
                }
            }
            user.setFollowers(new DSet<MemberName>());
            listener.requestProcessed();
            return;
        }

        // make sure the target member is online and in the same room as the requester
        MemberObject target = MsoyServer.lookupMember(memberId);
        if (target == null || !ObjectUtil.equals(user.location, target.location)) {
            throw new InvocationException("m.follow_not_in_room");
        }

        // make sure the target is accepting invitations from the requester
        if (!target.isAvailableTo(user.getMemberId())) {
            throw new InvocationException("m.follow_not_available");
        }

        // issue the follow invitation to the target
        String msg = MessageBundle.tcompose("m.follow_invite", user.memberName, user.getMemberId());
        SpeakUtil.sendMessage(target, new NotifyMessage(msg));

        // add this player to our followers set, if they ratify the follow request before we leave
        // our current location, the wiring up will be complete; if we leave the room before they
        // ratify the request (or if they never do), we'll remove them from our set
        if (!user.followers.containsKey(target.getMemberId())) {
            log.info("Adding follower " + target.memberName + " to " + user.memberName + ".");
            user.addToFollowers(target.memberName);
        } // else: what to do about repeat requests? ignore them? send again?
        listener.requestProcessed();
    }

    // from interface MemberProvider
    public void followMember (ClientObject caller, int memberId,
                              InvocationService.ConfirmListener listener)
        throws InvocationException
    {
        MemberObject user = (MemberObject) caller;

        // if the caller is requesting to clear their follow, do so
        if (memberId == 0) {
            if (user.following != null) {
                MemberObject followee = MsoyServer.lookupMember(user.following.getMemberId());
                if (followee != null) {
                    followee.removeFromFollowers(user.getMemberId());
                }
                user.setFollowing(null);
            }
            listener.requestProcessed();
            return;
        }

        // otherwise they're accepting a follow request, make sure it's still valid
        MemberObject target = MsoyServer.lookupMember(memberId);
        if (target == null || !target.followers.containsKey(user.getMemberId())) {
            throw new InvocationException("m.follow_invite_expired");
        }

        // finish the loop by setting them as our followee
        user.setFollowing(target.memberName);
        listener.requestProcessed();
    }

    // from interface MemberProvider
    public void setAway (ClientObject caller, boolean away, String message)
        //throws InvocationException
    {
        final MemberObject user = (MemberObject) caller;
        user.setAwayMessage(away ? message : null);
        BodyProvider.updateOccupantStatus(user, user.location,
            away ? MsoyBodyObject.AWAY : OccupantInfo.ACTIVE);
    }

    // from interface MemberProvider
    public void setAvatar (ClientObject caller, int avatarItemId, final float newScale,
                           final InvocationService.ConfirmListener listener)
        throws InvocationException
    {
        final MemberObject user = (MemberObject) caller;
        ensureNotGuest(user);

        if (avatarItemId == 0) {
            // a request to return to the default avatar
            finishSetAvatar(user, null, newScale, listener);
            return;
        }

        // otherwise, make sure it exists and we own it
        final ItemIdent ident = new ItemIdent(Item.AVATAR, avatarItemId);
        MsoyServer.itemMan.getItem(ident, new ResultListener<Item>() {
            public void requestCompleted (Item item) {
                Avatar avatar = (Avatar) item;
                if (user.getMemberId() != avatar.ownerId) { // ensure that they own it
                    String errmsg = "Not user's avatar [ownerId=" + avatar.ownerId + "]";
                    requestFailed(new Exception(errmsg));
                } else {
                    finishSetAvatar(user, avatar, newScale, listener);
                }
            }
            public void requestFailed (Exception cause) {
                log.log(Level.WARNING, "Unable to setAvatar [for=" + user.getMemberId() +
                        ", avatar=" + ident + "].", cause);
                listener.requestFailed(InvocationCodes.INTERNAL_ERROR);
            }
        });
    }

    // from interface MemberProvider
    public void setDisplayName (ClientObject caller, final String name,
                                InvocationService.InvocationListener listener)
        throws InvocationException
    {
        final MemberObject user = (MemberObject) caller;
        ensureNotGuest(user);

        // TODO: verify entered string

        String uname = "setDisplayName(" + user.who() + ", " + name + ")";
        MsoyServer.invoker.postUnit(new PersistingUnit(uname, listener) {
            public void invokePersistent () throws Exception {
                _memberRepo.configureDisplayName(user.getMemberId(), name);
            }
            public void handleSuccess () {
                user.updateDisplayName(name);
                updateOccupantInfo(user);
            }
        });
    }

    // from interface MemberProvider
    public void getDisplayName (ClientObject caller, final int memberId,
                                InvocationService.ResultListener listener)
        throws InvocationException
    {
        String uname = "getDisplayName(" + memberId + ")";
        MsoyServer.invoker.postUnit(new PersistingUnit(uname, listener) {
            public void invokePersistent () throws Exception {
                MemberNameRecord rec = _memberRepo.loadMemberName(memberId);
                _displayName = (rec == null) ? "" : rec.name;
            }
            public void handleSuccess () {
                reportRequestProcessed(_displayName);
            }
            protected String _displayName;
        });
    }

    // from interface MemberProvider
    public void getGroupName (ClientObject caller, final int groupId,
                              InvocationService.ResultListener listener)
    {
        MsoyServer.invoker.postUnit(new PersistingUnit("getGroupName(" + groupId + ")", listener) {
            public void invokePersistent () throws Exception {
                GroupRecord rec = _groupRepo.loadGroup(groupId);
                _groupName = (rec == null) ? "" : rec.name;
            }
            public void handleSuccess () {
                reportRequestProcessed(_groupName);
            }
            protected String _groupName;
        });
    }

    // from interface MemberProvider
    public void acknowledgeNotifications (ClientObject caller, int[] ids,
                                          InvocationService.InvocationListener listener)
        throws InvocationException
    {
        MemberObject user = (MemberObject) caller;

        user.startTransaction();
        try {
            for (int id : ids) {
                user.acknowledgeNotification(id);
            }
        } finally {
            user.commitTransaction();
        }
    }

    // from interface MemberProvider
    public void issueInvitation (ClientObject caller, final MemberName guest,
                                 InvocationService.ResultListener listener)
        throws InvocationException
    {
        final MemberObject member = (MemberObject) caller;
        ensureNotGuest(member);

        String uname = "issueInvitation(" + member.getMemberId() + ")";
        MsoyServer.invoker.postUnit(new PersistingUnit(uname, listener) {
            public void invokePersistent () throws Exception {
                _invitesAvailable = _memberRepo.getInvitesGranted(member.getMemberId());
                if (_invitesAvailable <= 0) {
                    throw new InvocationException("e.not_enough_invites");
                }
                _memberRepo.addInvite(guest.toString(), member.getMemberId(),
                                      _inviteId = _memberRepo.generateInviteId());
            }
            public void handleSuccess () {
                MsoyServer.notifyMan.notifyIssuedInvitation(guest, _inviteId);
                reportRequestProcessed(--_invitesAvailable);
            }
            protected int _invitesAvailable;
            protected String _inviteId;
        });
    }

    // from interface MemberProvider
    public void setHomeSceneId (ClientObject caller, final int sceneId,
                                InvocationService.ConfirmListener listener)
        throws InvocationException
    {
        final MemberObject member = (MemberObject) caller;
        ensureNotGuest(member);

        String uname = "setHomeSceneId(" + member.getMemberId() + ")";
        MsoyServer.invoker.postUnit(new PersistingUnit(uname, listener) {
            public void invokePersistent () throws Exception {
                int memberId = member.getMemberId();
                SceneRecord scene = MsoyServer.sceneRepo.loadScene(sceneId);
                if (scene.ownerType == MsoySceneModel.OWNER_TYPE_MEMBER &&
                    scene.ownerId == memberId) {
                    _memberRepo.setHomeSceneId(memberId, sceneId);
                } else {
                    throw new InvocationException("e.not_room_owner");
                }
            }
            public void handleSuccess () {
                member.setHomeSceneId(sceneId);
                reportRequestProcessed();
            }
        });
    }

    /**
     * Grant a member some flow, categorized and optionally metatagged with an action type and a
     * detail String. The member's {@link MemberRecord} is updated, as is the {@link
     * DailyFlowGrantedRecord}. A {@link MemberActionLogRecord} is recorded for the supplied grant
     * action. Finally, a line is written to the flow grant log.
     */
    public void grantFlow (final int memberId, final int amount,
                           final UserAction grantAction, final String details)
    {
        MsoyServer.invoker.postUnit(new RepositoryUnit("grantFlow") {
            public void invokePersist () throws PersistenceException {
                _flowRec = _memberRepo.getFlowRepository().grantFlow(
                    memberId, amount, grantAction, details);
            }
            public void handleSuccess () {
                MemberNodeActions.flowUpdated(_flowRec);
            }
            public void handleFailure (Exception pe) {
                log.log(Level.WARNING, "Unable to grant flow [memberId=" + memberId +
                        ", action=" + grantAction + ", amount=" + amount +
                        ", details=" + details + "]", pe);
            }
            protected MemberFlowRecord _flowRec;
        });
    }

    /**
     * Debit a member some flow, categorized and optionally metatagged with an action type and a
     * detail String. The member's {@link MemberRecord} is updated, as is the {@link
     * DailyFlowSpentRecord}. A {@link MemberActionLogRecord} is recorded for the supplied spend
     * action. Finally, a line is written to the flow grant log.
     */
    public void spendFlow (final int memberId, final int amount,
                           final UserAction spendAction, final String details)
    {
        MsoyServer.invoker.postUnit(new RepositoryUnit("spendFlow") {
            public void invokePersist () throws PersistenceException {
                _flowRec = _memberRepo.getFlowRepository().spendFlow(
                    memberId, amount, spendAction, details);
            }
            public void handleSuccess () {
                MemberNodeActions.flowUpdated(_flowRec);
            }
            public void handleFailure (Exception pe) {
                log.log(Level.WARNING, "Unable to spend flow [memberId=" + memberId +
                        ", action=" + spendAction + ", amount=" + amount +
                        ", details=" + details + "]", pe);
            }
            protected MemberFlowRecord _flowRec;
        });
    }

    /**
     * Register and log an action taken by a specific user for humanity assessment and conversion
     * analysis purposes. Some actions grant flow as a result of being taken, this method handles
     * that granting and updating the member's flow if they are online.
     */
    public void logUserAction (MemberName member, final UserAction action, final String details)
    {
        final int memberId = member.getMemberId();
        MsoyServer.invoker.postUnit(new RepositoryUnit("takeAction") {
            public void invokePersist () throws PersistenceException {
                // record that that took the action
                _flowRec = _memberRepo.getFlowRepository().logUserAction(memberId, action, details);
            }
            public void handleSuccess () {
                if (_flowRec != null) {
                    MemberNodeActions.flowUpdated(_flowRec);
                }
            }
            public void handleFailure (Exception pe) {
                log.warning("Unable to note user action [memberId=" + memberId +
                            ", action=" + action + ", details=" + details + "]");
            }
            protected MemberFlowRecord _flowRec;
        });
    }

    /**
     * Check if the member's accumulated flow level matches up with their current level, and update
     * their current level if necessary
     */
    public void checkCurrentLevel (final MemberObject member)
    {
        int level = Arrays.binarySearch(_levelForFlow, member.accFlow);
        if (level < 0) {
            level = -1 * level - 1;
            int length = _levelForFlow.length;
            // if the _levelForFlow array isn't big enough, double its size and flesh out the new
            // half
            if (level == length) {
                int[] temp = new int[length*2];
                for (int ii = 0; ii < length; ii++) {
                    temp[ii] = _levelForFlow[ii];
                }
                _levelForFlow = temp;
                calculateLevelsForFlow(length);
                checkCurrentLevel(member);
                return;
            }
            // level was equal to what would be the insertion point of accFlow, which is actually
            // one greater than the real level.
            level--;
        }
        // the flow value at array index ii cooresponds to level ii+1
        level++;

        if (member.level != level) {
            // update their level now so that we don't come along and do this again while the
            // invoker is off writing things to the database
            member.setLevel(level);

            final int newLevel = level;
            MsoyServer.invoker.postUnit(new RepositoryUnit("updateLevel") {
                public void invokePersist () throws PersistenceException {
                    int memberId = member.getMemberId();
                    // record the new level, and grant a new invite
                    _memberRepo.setUserLevel(memberId, newLevel);
                    _memberRepo.grantInvites(memberId, 1);
                    // mark the level gain in their feed
                    MsoyServer.feedRepo.publishMemberMessage(
                        memberId, FeedMessageType.FRIEND_GAINED_LEVEL, String.valueOf(newLevel));
                }
                public void handleSuccess () {
                    member.notify(new LevelUpNotification(newLevel));
                }
                public void handleFailure (Exception pe) {
                    log.warning("Unable to set user level [memberId=" +
                        member.getMemberId() + ", level=" + newLevel + "]");
                }
            });
        }
    }

    /**
     * Update a member's persistent AVR membership in the database, and in dobj-land
     * as well.
     */
    public void updateAVRGameId (final MemberObject member, final int gameId)
    {
        // assumes member != null i.e. we're on the right world server
        MsoyServer.invoker.postUnit(new Invoker.Unit("updateAVRGameId") {
            public boolean invoke () {
                try {
                    MsoyServer.memberRepo.setAVRGameId(member.getMemberId(), gameId);
                } catch (PersistenceException pe) {
                    log.log(Level.WARNING, "Failed to update member's AVR game id [member=" +
                            member + ", gameId=" + gameId + "]", pe);
                    return false;
                }
                return true;
            }
            public void handleResult () {
                member.setAvrGameId(gameId);
            }
        });
    }

    /**
     * Convenience method to ensure that the specified caller is not a guest.
     */
    protected void ensureNotGuest (MemberObject caller)
        throws InvocationException
    {
        if (caller.isGuest()) {
            throw new InvocationException(InvocationCodes.ACCESS_DENIED);
        }
    }

    /**
     * Finish configuring the user's avatar.
     *
     * @param avatar may be null to revert to the default member avatar.
     */
    protected void finishSetAvatar (
        final MemberObject user, final Avatar avatar, final float newScale,
        final InvocationService.ConfirmListener listener)
    {
        MsoyServer.invoker.postUnit(new RepositoryUnit("setAvatarPt2") {
            public void invokePersist () throws PersistenceException {
                _memberRepo.configureAvatarId(user.getMemberId(),
                    (avatar == null) ? 0 : avatar.itemId);
                if (newScale != 0 && avatar != null && avatar.scale != newScale) {
                    MsoyServer.itemMan.getAvatarRepository().updateScale(avatar.itemId, newScale);
                }
            }

            public void handleSuccess () {
                if (newScale != 0 && avatar != null) {
                    avatar.scale = newScale;
                    MsoyServer.itemMan.updateUserCache(avatar);
                }
                MsoyServer.itemMan.updateItemUsage(
                    user.getMemberId(), user.avatar, avatar, new ResultListener.NOOP<Object>() {
                    public void requestFailed (Exception cause) {
                        log.warning("Unable to update usage from an avatar change.");
                    }
                });
                user.setAvatar(avatar);
                user.actorState = null; // clear out their state
                updateOccupantInfo(user);
                listener.requestProcessed();
            }

            public void handleFailure (Exception pe) {
                log.warning("Unable to set avatar [user=" + user.which() +
                            ", avatar='" + avatar + "', " + "error=" + pe + "].");
                log.log(Level.WARNING, "", pe);
                listener.requestFailed(InvocationCodes.INTERNAL_ERROR);
            }
        });

    }

    protected void calculateLevelsForFlow (int fromIndex)
    {
        // This equation governs the total flow requirement for a given level (n):
        // flow(n) = flow(n-1) + ((n-1) * 17.8 - 49) * (3000 / 60)
        // where (n-1) * 17.8 - 49 is the equation discovered by PARC researchers that correlates
        // to the time (in minutes) it takes the average WoW player to get from level n-1 to level
        // n, and 3000 is the expected average flow per hour that we hope to drive our system on.
        for (int ii = fromIndex; ii < _levelForFlow.length; ii++) {
            // this array gets filled here with values for levels 1 through _levelForFlow.length...
            // the flow requirement for level n is at array index n-1.  Also, this function will
            // never be called before _levelForFlow has been inialized with 1+ entries.
            _levelForFlow[ii] = _levelForFlow[ii-1] + (int)((ii * 17.8 - 49) * (3000 / 60));
        }
    }

    /** An interval that updates the popular places snapshot every so often. */
    protected Interval _ppInvalidator;

    /** The most recent summary of popular places in the whirled. */
    protected PopularPlacesSnapshot _ppSnapshot;

    /** Provides access to persistent member data. */
    protected MemberRepository _memberRepo;

    /** Provides access to persistent group data. */
    protected GroupRepository _groupRepo;

    /** The array of memoized flow values for each level.  The first few levels are hard coded, the
     * rest are calculated according to the equation in calculateLevelsForFlow() */
    protected int[] _levelForFlow;

    /** The required flow for the first few levels is hard-coded */
    protected static final int[] BEGINNING_FLOW_LEVELS = { 0, 300, 900, 1800, 3000, 5100, 8100 };

    /** The frequency with which we recalculate our popular places snapshot. */
    protected static final long POP_PLACES_REFRESH_PERIOD = 5*1000;
}
