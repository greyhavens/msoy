//
// $Id$

package com.threerings.msoy.server;

import java.util.Arrays;
import java.util.Map;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import com.samskivert.io.PersistenceException;

import com.samskivert.jdbc.RepositoryUnit;

import com.samskivert.util.Interval;
import com.samskivert.util.Invoker;
import com.samskivert.util.ObjectUtil;
import com.samskivert.util.ResultListener;
import com.samskivert.util.StringUtil;
import com.threerings.util.MessageBundle;

import com.threerings.presents.annotation.EventThread;
import com.threerings.presents.annotation.MainInvoker;
import com.threerings.presents.client.InvocationService;
import com.threerings.presents.data.ClientObject;
import com.threerings.presents.data.InvocationCodes;
import com.threerings.presents.dobj.AttributeChangeListener;
import com.threerings.presents.dobj.AttributeChangedEvent;
import com.threerings.presents.dobj.DSet;
import com.threerings.presents.server.ClientManager;
import com.threerings.presents.server.InvocationException;
import com.threerings.presents.server.InvocationManager;
import com.threerings.presents.server.PresentsClient;
import com.threerings.presents.server.PresentsDObjectMgr;
import com.threerings.presents.util.PersistingUnit;

import com.threerings.crowd.data.OccupantInfo;
import com.threerings.crowd.server.BodyManager;
import com.threerings.crowd.server.PlaceManager;
import com.threerings.crowd.server.PlaceRegistry;

import com.threerings.msoy.group.server.persist.GroupRecord;
import com.threerings.msoy.group.server.persist.GroupRepository;
import com.threerings.msoy.person.server.MailLogic;
import com.threerings.msoy.person.server.persist.FeedRepository;
import com.threerings.msoy.person.server.persist.ProfileRepository;
import com.threerings.msoy.person.util.FeedMessageType;

import com.threerings.msoy.item.data.all.Avatar;
import com.threerings.msoy.item.data.all.Item;
import com.threerings.msoy.item.data.all.ItemIdent;
import com.threerings.msoy.item.server.ItemManager;

import com.threerings.msoy.badge.data.BadgeSet;
import com.threerings.msoy.badge.server.BadgeManager;
import com.threerings.msoy.badge.server.ServerStatSet;
import com.threerings.msoy.notify.data.LevelUpNotification;
import com.threerings.msoy.notify.server.NotificationManager;
import com.threerings.msoy.peer.server.MsoyPeerManager;
import com.threerings.msoy.profile.gwt.Profile;
import com.threerings.msoy.underwire.server.SupportLogic;

import com.threerings.msoy.data.MemberLocation;
import com.threerings.msoy.data.MemberObject;
import com.threerings.msoy.data.MsoyBodyObject;
import com.threerings.msoy.data.MsoyCodes;
import com.threerings.msoy.data.PlayerMetrics;
import com.threerings.msoy.data.UserActionDetails;

import com.threerings.msoy.data.all.FriendEntry;
import com.threerings.msoy.data.all.MemberName;
import com.threerings.msoy.data.all.ReferralInfo;

import com.threerings.msoy.server.persist.FlowRepository;
import com.threerings.msoy.server.persist.MemberFlowRecord;
import com.threerings.msoy.server.persist.MemberRecord;
import com.threerings.msoy.server.persist.MemberRepository;

import com.threerings.msoy.server.util.MailSender;

import com.threerings.msoy.room.data.MsoySceneModel;
import com.threerings.msoy.room.server.persist.MsoySceneRepository;
import com.threerings.msoy.room.server.persist.SceneRecord;

import static com.threerings.msoy.Log.log;

/**
 * Manage msoy members.
 */
@Singleton @EventThread
public class MemberManager
    implements MemberProvider
{
    @Inject public MemberManager (InvocationManager invmgr, MsoyPeerManager peerMan)
    {
        invmgr.registerDispatcher(new MemberDispatcher(this), MsoyCodes.MEMBER_GROUP);

        // intialize our internal array of memoized flow values per level.  Start with 256
        // calculated levels
        _levelForFlow = new int[256];
        for (int ii = 0; ii < BEGINNING_FLOW_LEVELS.length; ii++) {
            _levelForFlow[ii] = BEGINNING_FLOW_LEVELS[ii];
        }
        calculateLevelsForFlow(BEGINNING_FLOW_LEVELS.length);

        // register a member forward participant that handles our transient bits
        peerMan.registerMemberForwarder(new MsoyPeerManager.MemberForwarder() {
            public void packMember (MemberObject memobj, Map<String,Object> data) {
                // flush the transient bits in our metrics as we will snapshot and send this data
                // before we depart our current room (which is when the are normally saved)
                memobj.metrics.save(memobj);

                // store our transient bits in the additional data map
                data.put("MO.actorState", memobj.actorState);
                data.put("MO.metrics", memobj.metrics);
                data.put("MO.badges", memobj.badges);
                data.put("MO.stats", memobj.stats);
            }

            public void unpackMember (MemberObject memobj, Map<String,Object> data) {
                // grab and reinstate our bits
                memobj.actorState = (String)data.get("MO.actorState");
                memobj.metrics = (PlayerMetrics)data.get("MO.metrics");
                memobj.badges = (BadgeSet)data.get("MO.badges");
                ServerStatSet stats = (ServerStatSet)data.get("MO.stats");
                stats.setMemberObject(memobj);
                memobj.stats = stats;
            }
        });
    }

    /**
     * Prepares our member manager for operation.
     */
    public void init ()
    {
        _ppSnapshot = PopularPlacesSnapshot.takeSnapshot(_omgr, _peerMan);
        _ppInvalidator = new Interval(_omgr) {
            public void expired() {
                PopularPlacesSnapshot newSnapshot =
                    PopularPlacesSnapshot.takeSnapshot(_omgr, _peerMan);
                synchronized(MemberManager.this) {
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
        PlaceManager pmgr = _placeReg.getPlaceManager(user.getPlaceOid());
        if (pmgr != null) {
            pmgr.updateOccupantInfo(user.createOccupantInfo(pmgr.getPlaceObject()));
        }
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

        // update badges
        _badgeMan.updateBadges(member);
    }

    // from interface MemberProvider
    public void inviteToBeFriend (ClientObject caller, final int friendId,
                                  InvocationService.ConfirmListener listener)
        throws InvocationException
    {
        final MemberObject user = (MemberObject) caller;
        ensureNotGuest(user);
        _invoker.postUnit(new PersistingUnit("inviteToBeFriend", listener) {
            public void invokePersistent () throws Exception {
                _mailLogic.sendFriendInvite(user.getMemberId(), friendId);
            }
            public void handleSuccess () {
                ((InvocationService.ConfirmListener)_listener).requestProcessed();
            }
        });
    }

    // from interface MemberProvider
    public void bootFromPlace (
        ClientObject caller, int booteeId, InvocationService.ConfirmListener listener)
        throws InvocationException
    {
        MemberObject user = (MemberObject) caller;
        if (user.location == null) {
            throw new InvocationException(InvocationCodes.INTERNAL_ERROR);
//            // TEST: let's pretend that we KNOW that they're in a game... just move them home
//            MemberObject bootee = _locator.lookupMember(booteeId);
//            _screg.moveBody(bootee, bootee.getHomeSceneId());
//            listener.requestProcessed();
//            return;
        }

        PlaceManager pmgr = _placeReg.getPlaceManager(user.location.placeOid);
        if (!(pmgr instanceof BootablePlaceManager)) {
            throw new InvocationException(InvocationCodes.INTERNAL_ERROR);
        }

        String response = ((BootablePlaceManager) pmgr).bootFromPlace(user, booteeId);
        if (response == null) {
            listener.requestProcessed();
        } else {
            listener.requestFailed(response);
        }
    }

    // from interface MemberProvider
    public void getHomeId (ClientObject caller, final byte ownerType, final int ownerId,
                           InvocationService.ResultListener listener)
        throws InvocationException
    {
        _invoker.postUnit(new PersistingUnit(listener) {
            public void invokePersistent () throws PersistenceException {
                _homeId = _memberLogic.getHomeId(ownerType, ownerId);
            }
            public void handleSuccess () {
                if (_homeId == null) {
                    handleFailure(new InvocationException("m.no_such_user"));
                } else {
                    ((InvocationService.ResultListener)_listener).requestProcessed(_homeId);
                }
            }
            protected Integer _homeId;
        });
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

        MemberLocation memloc = _peerMan.getMemberLocation(memberId);
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
                MemberObject fmo = _locator.lookupMember(follower.getMemberId());
                if (fmo != null) {
                    fmo.setFollowing(null);
                }
            }
            user.setFollowers(new DSet<MemberName>());
            listener.requestProcessed();
            return;
        }

        // make sure the target member is online and in the same room as the requester
        MemberObject target = _locator.lookupMember(memberId);
        if (target == null || !ObjectUtil.equals(user.location, target.location)) {
            throw new InvocationException("m.follow_not_in_room");
        }

        // make sure the target is accepting invitations from the requester
        if (!target.isAvailableTo(user.getMemberId())) {
            throw new InvocationException("m.follow_not_available");
        }

        // issue the follow invitation to the target
        _notifyMan.notifyFollowInvite(target, user.memberName.toString(), user.getMemberId());

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
                MemberObject followee = _locator.lookupMember(user.following.getMemberId());
                if (followee != null) {
                    followee.removeFromFollowers(user.getMemberId());
                }
                user.setFollowing(null);
            }
            listener.requestProcessed();
            return;
        }

        // otherwise they're accepting a follow request, make sure it's still valid
        MemberObject target = _locator.lookupMember(memberId);
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
        _bodyMan.updateOccupantStatus(
            user, user.location, away ? MsoyBodyObject.AWAY : OccupantInfo.ACTIVE);
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
        _itemMan.getItem(ident, new ResultListener<Item>() {
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
                log.warning("Unable to setAvatar [for=" + user.getMemberId() +
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
        _invoker.postUnit(new PersistingUnit(uname, listener) {
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
        _invoker.postUnit(new PersistingUnit(uname, listener) {
            public void invokePersistent () throws Exception {
                _displayName = String.valueOf(_memberRepo.loadMemberName(memberId));
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
        _invoker.postUnit(new PersistingUnit("getGroupName(" + groupId + ")", listener) {
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
    public void acknowledgeWarning (ClientObject caller)
    {
        final MemberObject user = (MemberObject) caller;
        _invoker.postUnit(new Invoker.Unit("acknowledgeWarning") {
            public boolean invoke () {
                try {
                    _memberRepo.clearMemberWarning(user.getMemberId());
                } catch (PersistenceException pe) {
                    log.warning("Failed to clean member warning [for=" +
                        user.getMemberId() + "].", pe);
                }
                return false;
            }
        });
    }

    // from interface MemberProvider
    public void setHomeSceneId (ClientObject caller, final int ownerType, final int ownerId,
                                final int sceneId, InvocationService.ConfirmListener listener)
        throws InvocationException
    {
        final MemberObject member = (MemberObject) caller;
        ensureNotGuest(member);

        String uname = "setHomeSceneId(" + member.getMemberId() + ")";
        _invoker.postUnit(new PersistingUnit(uname, listener) {
            public void invokePersistent () throws Exception {
                int memberId = member.getMemberId();
                SceneRecord scene = _sceneRepo.loadScene(sceneId);
                if (scene.ownerType == MsoySceneModel.OWNER_TYPE_MEMBER) {
                    if (scene.ownerId == memberId) {
                        _memberRepo.setHomeSceneId(memberId, sceneId);
                    } else {
                        throw new InvocationException("e.not_room_owner");
                    }
                } else if (scene.ownerType == MsoySceneModel.OWNER_TYPE_GROUP) {
                    if (member.isGroupManager(scene.ownerId)) {
                        _groupRepo.setHomeSceneId(scene.ownerId, sceneId);
                    } else {
                        throw new InvocationException("e.not_room_manager");
                    }
                } else {
                    log.warning("Unknown scene model owner type [sceneId=" +
                        scene.sceneId + ", ownerType=" + scene.ownerType + "]");
                    throw new InvocationException(InvocationCodes.INTERNAL_ERROR);
                }
            }
            public void handleSuccess () {
                if (ownerType == MsoySceneModel.OWNER_TYPE_MEMBER) {
                    member.setHomeSceneId(sceneId);
                }
                reportRequestProcessed();
            }
        });
    }

    // from interface MemberProvider
    public void getGroupHomeSceneId (ClientObject caller, final int groupId,
                                     InvocationService.ResultListener listener)
        throws InvocationException
    {
        String uname = "getHomeSceneId(" + groupId + ")";
        _invoker.postUnit(new PersistingUnit(uname, listener) {
            public void invokePersistent () throws Exception {
                _homeSceneId = _groupRepo.getHomeSceneId(groupId);
            }
            public void handleSuccess () {
                reportRequestProcessed(_homeSceneId);
            }
            protected int _homeSceneId;
        });
    }

    // from interface MemberProvider
    public void complainMember (ClientObject caller, int memberId, String complaint)
    {
        _supportLogic.addComplaint((MemberObject)caller, memberId, complaint);
    }

    // from interface MemberProvider
    public void updateStatus (ClientObject caller, String status,
                              InvocationService.InvocationListener listener)
        throws InvocationException
    {
        final MemberObject member = (MemberObject) caller;
        ensureNotGuest(member);

        final String commitStatus = StringUtil.truncate(status, Profile.MAX_STATUS_LENGTH);
        String uname = "updateStatus(" + member.getMemberId() + ")";
        _invoker.postUnit(new PersistingUnit(uname, listener) {
            public void invokePersistent () throws Exception {
                _profileRepo.updateHeadline(member.getMemberId(), commitStatus);
            }
            public void handleSuccess () {
                member.setHeadline(commitStatus);
                MemberNodeActions.updateFriendEntries(member);
            }
        });
    }

    // from interface MemberProvider
    public void trackReferralCreation (ClientObject caller, ReferralInfo info)
    {
        _eventLog.referralCreated(info);
    }

    // from interface MemberProvider
    public void emailShare (ClientObject caller, final int sceneId, final String[] emails,
                            final String message, final InvocationService.ConfirmListener cl)
    {
        final MemberObject memObj = (MemberObject) caller;

        _invoker.postUnit(new RepositoryUnit("emailShare") {
            public void invokePersist () throws PersistenceException {
                _sender = _memberRepo.loadMember(memObj.getMemberId());
            }
            public void handleSuccess () {
                String from = (_sender == null) ?
                    "no-reply@whirled.com" : _sender.accountName;
                String name = (_sender == null) ? null : _sender.name;
                String url = ServerConfig.getServerURL() + "/#world-s" + sceneId;
                for (String recip : emails) {
                    MailSender.sendEmail(recip, from, "shareInvite", "name", name,
                        "message", message, "link", url);
                }
                cl.requestProcessed();
            }
            protected MemberRecord _sender;
        });
    }

    // from interface MemberProvider
    public void getABTestGroup (
        ClientObject caller, final ReferralInfo info, final String testName,
        final boolean logEvent, InvocationService.ResultListener listener)
    {
        _invoker.postUnit(new PersistingUnit(listener) {
            public void invokePersistent () throws PersistenceException {
                _testGroup = new Integer(_memberLogic.getABTestGroup(testName, info, logEvent));
            }
            public void handleSuccess () {
                ((InvocationService.ResultListener)_listener).requestProcessed(_testGroup);
            }
            protected Integer _testGroup;
        });
    }

    // from interface MemberProvider
    public void trackClientAction (
        ClientObject caller, ReferralInfo info, String actionName, String details)
    {
        if (info == null) {
            log.warning(
                "Failed to log client action with null referral", "actionName", actionName);
            return;
        }

        _eventLog.clientAction(info.tracker, actionName, details);
    }

    // from interface MemberProvider
    public void trackTestAction (
        ClientObject caller, ReferralInfo info, String actionName, String testName)
    {
        if (info == null) {
            log.warning(
                "Failed to log test action with null referral", "actionName", actionName);
            return;
        }
        int abTestGroup = -1;
        if (testName != null) {
            // grab the group without logging a tracking event about it
            abTestGroup = _memberLogic.getABTestGroup(testName, info, false);
        } else {
            testName = "";
        }
        _eventLog.testAction(info.tracker, actionName, testName, abTestGroup);
    }

    /**
     * Grants flow to the member identified in the supplied user action details.
     *
     * @see FlowRepository#grantFlow(UserActionDetails,int)
     */
    public void grantFlow (final UserActionDetails info, final int amount)
    {
        _invoker.postUnit(new RepositoryUnit("grantFlow") {
            public void invokePersist () throws PersistenceException {
                _flowRec = _memberRepo.getFlowRepository().grantFlow(info, amount);
            }
            public void handleSuccess () {
                MemberNodeActions.flowUpdated(_flowRec);
            }
            public void handleFailure (Exception pe) {
                log.warning("Unable to grant flow [memberId=" + info.memberId +
                        ", action=" + info.action + ", amount=" + amount + "]", pe);
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
    public void spendFlow (final UserActionDetails info, final int amount)
    {
        _invoker.postUnit(new RepositoryUnit("spendFlow") {
            public void invokePersist () throws PersistenceException {
                _flowRec = _memberRepo.getFlowRepository().spendFlow(info, amount);
            }
            public void handleSuccess () {
                MemberNodeActions.flowUpdated(_flowRec);
            }
            public void handleFailure (Exception pe) {
                log.warning("Unable to spend flow [amount=" + amount +
                        ", info=" + info + "]", pe);
            }
            protected MemberFlowRecord _flowRec;
        });
    }

    /**
     * Register and log an action taken by a specific user for humanity assessment and conversion
     * analysis purposes. Some actions grant flow as a result of being taken, this method handles
     * that granting and updating the member's flow if they are online.
     */
    public void logUserAction (final UserActionDetails info)
    {
        _invoker.postUnit(new RepositoryUnit("takeAction") {
            public void invokePersist () throws PersistenceException {
                // record that that took the action
                _flowRec = _memberRepo.getFlowRepository().logUserAction(info);
            }
            public void handleSuccess () {
                if (_flowRec != null) {
                    MemberNodeActions.flowUpdated(_flowRec);
                }
            }
            public void handleFailure (Exception pe) {
                log.warning("Unable to note user action [action=" + info + "].");
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
            _invoker.postUnit(new RepositoryUnit("updateLevel") {
                public void invokePersist () throws PersistenceException {
                    int memberId = member.getMemberId();
                    // record the new level, and grant a new invite
                    _memberRepo.setUserLevel(memberId, newLevel);
                    _memberRepo.grantInvites(memberId, 1);
                    // mark the level gain in their feed
                    _feedRepo.publishMemberMessage(
                        memberId, FeedMessageType.FRIEND_GAINED_LEVEL, String.valueOf(newLevel));
                }
                public void handleSuccess () {
                    _notifyMan.notify(member, new LevelUpNotification(newLevel));
                }
                public void handleFailure (Exception pe) {
                    log.warning("Unable to set user level [memberId=" +
                        member.getMemberId() + ", level=" + newLevel + "]");
                }
            });
        }
    }

    /**
     * Boots a player from the server.  Must be called on the dobjmgr thread.
     *
     * @return true if the player was found and booted successfully
     */
    public boolean bootMember (int memberId)
    {
        MemberObject mobj = _locator.lookupMember(memberId);
        if (mobj != null) {
            PresentsClient pclient = _clmgr.getClient(mobj.username);
            if (pclient != null) {
                pclient.endSession();
                return true;
            }
        }
        return false;
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
        _invoker.postUnit(new RepositoryUnit("setAvatarPt2") {
            public void invokePersist () throws PersistenceException {
                _memberRepo.configureAvatarId(user.getMemberId(),
                    (avatar == null) ? 0 : avatar.itemId);
                if (newScale != 0 && avatar != null && avatar.scale != newScale) {
                    _itemMan.getAvatarRepository().updateScale(avatar.itemId, newScale);
                }
            }

            public void handleSuccess () {
                Avatar prev = user.avatar;
                if (newScale != 0 && avatar != null) {
                    avatar.scale = newScale;
                }
                _itemMan.updateItemUsage(
                    user.getMemberId(), prev, avatar, new ResultListener.NOOP<Object>() {
                    public void requestFailed (Exception cause) {
                        log.warning("Unable to update usage from an avatar change.");
                    }
                });

                // now we need to make sure that the two avatars have a reasonable touched time
                user.startTransaction();
                try {
                    // unset the current avatar to avoid busy-work in avatarUpdatedOnPeer, but
                    // we'll set the new avatar at the bottom...
                    user.avatar = null;

                    // NOTE: we are not updating the used/location fields of the cached avatars,
                    // I don't think it's necessary, but it'd be a simple matter here...
                    long now = System.currentTimeMillis();
                    if (prev != null) {
                        prev.lastTouched = now;
                        _itemMan.avatarUpdatedOnPeer(user, prev);
                    }
                    if (avatar != null) {
                        avatar.lastTouched = now + 1; // the new one should be more recently touched
                        _itemMan.avatarUpdatedOnPeer(user, avatar);
                    }

                    // now set the new avatar
                    user.setAvatar(avatar);
                    user.actorState = null; // clear out their state
                    updateOccupantInfo(user);

                } finally {
                    user.commitTransaction();
                }
                listener.requestProcessed();
            }

            public void handleFailure (Exception pe) {
                log.warning("Unable to set avatar [user=" + user.which() +
                            ", avatar='" + avatar + "', " + "error=" + pe + "].");
                log.warning(pe);
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

    /** The array of memoized flow values for each level.  The first few levels are hard coded, the
     * rest are calculated according to the equation in calculateLevelsForFlow() */
    protected int[] _levelForFlow;

    // dependencies
    @Inject protected ClientManager _clmgr;
    @Inject protected PresentsDObjectMgr _omgr;
    @Inject protected PlaceRegistry _placeReg;
    @Inject protected MailLogic _mailLogic;
    @Inject protected SupportLogic _supportLogic;
    @Inject protected MemberLogic _memberLogic;
    @Inject protected BodyManager _bodyMan;
    @Inject protected BadgeManager _badgeMan;
    @Inject protected NotificationManager _notifyMan;
    @Inject protected ItemManager _itemMan;
    @Inject protected MsoyPeerManager _peerMan;
    @Inject protected MemberLocator _locator;
    @Inject protected MemberRepository _memberRepo;
    @Inject protected GroupRepository _groupRepo;
    @Inject protected ProfileRepository _profileRepo;
    @Inject protected FeedRepository _feedRepo;
    @Inject protected MsoySceneRepository _sceneRepo;
    @Inject protected MsoyEventLogger _eventLog;
    @Inject protected @MainInvoker Invoker _invoker;

    /** The required flow for the first few levels is hard-coded */
    protected static final int[] BEGINNING_FLOW_LEVELS = { 0, 300, 900, 1800, 3000, 5100, 8100 };

    /** The frequency with which we recalculate our popular places snapshot. */
    protected static final long POP_PLACES_REFRESH_PERIOD = 5*1000;
}
