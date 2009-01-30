//
// $Id$

package com.threerings.msoy.server;

import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import com.samskivert.jdbc.RepositoryUnit;
import com.samskivert.jdbc.WriteOnlyUnit;
import com.samskivert.util.Interval;
import com.samskivert.util.Invoker;
import com.samskivert.util.ObjectUtil;
import com.samskivert.util.ResultListener;
import com.samskivert.util.StringUtil;

import com.google.common.collect.Lists;
import com.threerings.underwire.server.persist.EventRecord;
import com.threerings.underwire.web.data.Event;
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
import com.threerings.presents.server.PresentsSession;
import com.threerings.presents.server.PresentsDObjectMgr;
import com.threerings.presents.util.PersistingUnit;

import com.threerings.crowd.chat.data.ChatCodes;
import com.threerings.crowd.chat.data.ChatMessage;
import com.threerings.crowd.chat.data.UserMessage;
import com.threerings.crowd.chat.server.SpeakUtil;
import com.threerings.crowd.data.PlaceObject;
import com.threerings.crowd.server.BodyManager;
import com.threerings.crowd.server.PlaceManager;
import com.threerings.crowd.server.PlaceRegistry;

import com.threerings.msoy.data.CoinAwards;
import com.threerings.msoy.data.MemberExperience;
import com.threerings.msoy.data.MemberLocation;
import com.threerings.msoy.data.MemberObject;
import com.threerings.msoy.data.MsoyBodyObject;
import com.threerings.msoy.data.MsoyCodes;
import com.threerings.msoy.data.all.FriendEntry;
import com.threerings.msoy.data.all.MemberName;
import com.threerings.msoy.server.persist.BatchInvoker;
import com.threerings.msoy.server.persist.MemberRecord;
import com.threerings.msoy.server.persist.MemberRepository;
import com.threerings.msoy.server.util.MailSender;

import com.threerings.msoy.peer.server.MsoyPeerManager;

import com.threerings.msoy.badge.data.BadgeType;
import com.threerings.msoy.badge.data.all.EarnedBadge;
import com.threerings.msoy.badge.server.BadgeManager;
import com.threerings.msoy.group.server.persist.GroupRecord;
import com.threerings.msoy.group.server.persist.GroupRepository;
import com.threerings.msoy.item.data.ItemCodes;
import com.threerings.msoy.item.data.all.Avatar;
import com.threerings.msoy.item.data.all.Item;
import com.threerings.msoy.item.data.all.ItemIdent;
import com.threerings.msoy.item.server.ItemLogic;
import com.threerings.msoy.item.server.ItemManager;
import com.threerings.msoy.mail.server.MailLogic;
import com.threerings.msoy.notify.data.LevelUpNotification;
import com.threerings.msoy.notify.server.NotificationManager;
import com.threerings.msoy.person.server.persist.FeedRepository;
import com.threerings.msoy.person.server.persist.ProfileRepository;
import com.threerings.msoy.person.util.FeedMessageType;
import com.threerings.msoy.profile.gwt.Profile;
import com.threerings.msoy.room.data.EntityMemoryEntry;
import com.threerings.msoy.room.data.MemberInfo;
import com.threerings.msoy.room.data.MsoySceneModel;
import com.threerings.msoy.room.data.RoomObject;
import com.threerings.msoy.room.server.persist.MemoriesRecord;
import com.threerings.msoy.room.server.persist.MemoryRepository;
import com.threerings.msoy.room.server.persist.MsoySceneRepository;
import com.threerings.msoy.room.server.persist.SceneRecord;
import com.threerings.msoy.underwire.server.SupportLogic;

import static com.threerings.msoy.Log.log;

/**
 * Manage msoy members.
 */
@Singleton @EventThread
public class MemberManager
    implements MemberLocator.Observer, MemberProvider
{
    @Inject public MemberManager (
        InvocationManager invmgr, MsoyPeerManager peerMan, MemberLocator locator)
    {
        // register our bootstrap invocation service
        invmgr.registerDispatcher(new MemberDispatcher(this), MsoyCodes.MEMBER_GROUP);

        // register to hear when members logon and off
        locator.addObserver(this);

        // register to hear when members are forwarded between nodes
        peerMan.memberFwdObs.add(new MsoyPeerManager.MemberForwardObserver() {
            public void memberWillBeSent (String node, MemberObject memobj) {
                // flush the transient bits in our metrics as we will snapshot and send this data
                // before we depart our current room (which is when the are normally saved)
                MemberLocal mlocal = memobj.getLocal(MemberLocal.class);
                mlocal.metrics.save(memobj);

                // update the number of active seconds they've spent online
                MsoySession mclient = (MsoySession)_clmgr.getClient(memobj.username);
                if (mclient != null) {
                    mlocal.sessionSeconds += mclient.getSessionSeconds();
                    // let our client handler know that the session is not over but rather is being
                    // forwarded to another server
                    mclient.setSessionForwarded(true);
                }
            }
        });

        // intialize our internal array of memoized flow values per level.  Start with 256
        // calculated levels
        _levelForFlow = new int[256];
        for (int ii = 0; ii < BEGINNING_FLOW_LEVELS.length; ii++) {
            // augment the value so the account creation does not cause level 3 to happen
            _levelForFlow[ii] = BEGINNING_FLOW_LEVELS[ii] + CoinAwards.CREATED_ACCOUNT;
        }
        calculateLevelsForFlow(BEGINNING_FLOW_LEVELS.length);
    }

    /**
     * Prepares our member manager for operation.
     */
    public void init ()
    {
        // loading all the greeter ids is a pretty expensive query, so run it infrequently.
        _greeterIdsSnapshot = _memberRepo.loadGreeterIds();
        _greeterIdsInvalidator = new Interval(_batchInvoker) {
            @Override public void expired() {
                List<Integer> greeterIds = _memberRepo.loadGreeterIds();
                synchronized (_snapshotLock) {
                    _greeterIdsSnapshot = greeterIds;
                }
            }
        };
        _greeterIdsInvalidator.schedule(GREETERS_REFRESH_PERIOD, true);

        _ppSnapshot = PopularPlacesSnapshot.takeSnapshot(_omgr, _peerMan, getGreeterIdsSnapshot());
        _ppInvalidator = new Interval(_omgr) {
            @Override public void expired() {
                final PopularPlacesSnapshot newSnapshot =
                    PopularPlacesSnapshot.takeSnapshot(_omgr, _peerMan, getGreeterIdsSnapshot());
                synchronized (_snapshotLock) {
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
        synchronized (_snapshotLock) {
            return _ppSnapshot;
        }
    }

    public void addExperience (final MemberObject memObj, final MemberExperience newExp)
    {
        memObj.startTransaction();
        try {
            // If we're at our limit of experiences, remove the oldest.
            if (memObj.experiences.size() >= MAX_EXPERIENCES) {
                MemberExperience oldest = null;
                for (MemberExperience experience : memObj.experiences) {
                    if (oldest == null ||
                            experience.getDateOccurred().compareTo(oldest.getDateOccurred()) < 0) {
                        oldest = experience;
                    }
                }
                memObj.removeFromExperiences(oldest.getKey());
            }

            // Add the new experience
            memObj.addToExperiences(newExp);
        } finally {
            memObj.commitTransaction();
        }
    }

    // from interface MemberLocator.Observer
    public void memberLoggedOn (final MemberObject member)
    {
        if (member.isGuest()) {
            return;
        }

        //  add a listener for changes to accumulated flow so that the member's level can be
        // updated as necessary
        member.addListener(new AttributeChangeListener() {
            public void attributeChanged (final AttributeChangedEvent event) {
                if (MemberObject.ACC_COINS.equals(event.getName())) {
                    checkCurrentLevel(member);
                }
            }
        });

        // check their current level now in case they got flow while they were offline
        checkCurrentLevel(member);

        // update badges
        _badgeMan.updateBadges(member);
    }

    // from interface MemberLocator.Observer
    public void memberLoggedOff (final MemberObject member)
    {
        // nada
    }

    // from interface MemberProvider
    public void inviteToBeFriend (final ClientObject caller, final int friendId,
                                  final InvocationService.ResultListener listener)
        throws InvocationException
    {
        final MemberObject user = (MemberObject) caller;
        ensureNotGuest(user);
        _invoker.postUnit(new PersistingUnit("inviteToBeFriend", listener) {
            boolean autoFriended;
            @Override public void invokePersistent () throws Exception {
                MemberRecord frec = _memberRepo.loadMember(friendId);
                if (frec == null) {
                    log.warning("Requested to friend non-existent member", "who", user.who(),
                                "friendId", friendId);
                } else if (frec.isGreeter()) {
                    _memberLogic.establishFriendship(user.getMemberId(), friendId);
                    autoFriended = true;
                } else {
                    _mailLogic.sendFriendInvite(user.getMemberId(), friendId);
                }
            }
            @Override public void handleSuccess () {
                reportRequestProcessed(autoFriended);
                if (autoFriended) {
                    trackClientAction(caller, "autoFriendedFlashClient", null);
                }
            }
        });
    }

    // from interface MemberProvider
    public void bootFromPlace (final ClientObject caller, final int booteeId,
                               final InvocationService.ConfirmListener listener)
        throws InvocationException
    {
        final MemberObject user = (MemberObject) caller;
        if (user.location == null) {
            throw new InvocationException(InvocationCodes.INTERNAL_ERROR);
//            // TEST: let's pretend that we KNOW that they're in a game... just move them home
//            MemberObject bootee = _locator.lookupMember(booteeId);
//            _screg.moveBody(bootee, bootee.getHomeSceneId());
//            listener.requestProcessed();
//            return;
        }

        final PlaceManager pmgr = _placeReg.getPlaceManager(user.location.placeOid);
        if (!(pmgr instanceof BootablePlaceManager)) {
            throw new InvocationException(InvocationCodes.INTERNAL_ERROR);
        }

        final String response = ((BootablePlaceManager) pmgr).bootFromPlace(user, booteeId);
        if (response == null) {
            listener.requestProcessed();
        } else {
            listener.requestFailed(response);
        }
    }

    // from interface MemberProvider
    public void getHomeId (final ClientObject caller, final byte ownerType, final int ownerId,
                           final InvocationService.ResultListener listener)
        throws InvocationException
    {
        _invoker.postUnit(new PersistingUnit("getHomeId", listener) {
            @Override public void invokePersistent () throws Exception {
                _homeId = _memberLogic.getHomeId(ownerType, ownerId);
            }
            @Override public void handleSuccess () {
                if (_homeId == null) {
                    handleFailure(new InvocationException("m.no_such_user"));
                } else {
                    reportRequestProcessed(_homeId);
                }
            }
            protected Integer _homeId;
        });
    }

    // from interface MemberProvider
    public void getCurrentMemberLocation (final ClientObject caller, final int memberId,
                                          final InvocationService.ResultListener listener)
        throws InvocationException
    {
        final MemberObject user = (MemberObject) caller;

        // ensure that the other member is a full friend
        final FriendEntry entry = user.friends.get(memberId);
        if (null == entry) {
            throw new InvocationException("e.not_a_friend");
        }

        final MemberLocation memloc = _peerMan.getMemberLocation(memberId);
        if (memloc == null) {
            throw new InvocationException(MessageBundle.tcompose("e.not_online", entry.name));
        }
        listener.requestProcessed(memloc);
    }

    // from interface MemberProvider
    public void updateAvailability (final ClientObject caller, final int availability)
    {
        final MemberObject user = (MemberObject) caller;
        user.setAvailability(availability);
    }

    // from interface MemberProvider
    public void inviteToFollow (final ClientObject caller, final int memberId,
                                final InvocationService.InvocationListener listener)
        throws InvocationException
    {
        final MemberObject user = (MemberObject) caller;

        // make sure the target member is online and in the same room as the requester
        final MemberObject target = _locator.lookupMember(memberId);
        if (target == null || !ObjectUtil.equals(user.location, target.location)) {
            throw new InvocationException("e.follow_not_in_room");
        }

        // issue the follow invitation to the target
        if (!_notifyMan.notifyFollowInvite(target, user.memberName)) {
            // the target is not accepting invitations from the requester
            throw new InvocationException("e.follow_not_available");
        }
    }

    // from interface MemberProvider
    public void followMember (final ClientObject caller, final int memberId,
                              final InvocationService.InvocationListener listener)
        throws InvocationException
    {
        final MemberObject user = (MemberObject) caller;

        // if the caller is requesting to clear their follow, do so
        if (memberId == 0) {
            if (user.following != null) {
                MemberNodeActions.removeFollower(user.following.getMemberId(), user.getMemberId());
                user.setFollowing(null);
            }
            return;
        }

        // Make sure the target isn't bogus
        final MemberObject target = _locator.lookupMember(memberId);
        if (target == null) {
            throw new InvocationException("e.follow_invite_expired");
        }

        // Wire up both the leader and follower
        if (!target.followers.containsKey(user.getMemberId())) {
            log.info("Adding follower " + user.who() + " to " + target.who() + ".");
            target.addToFollowers(user.memberName);
        }
        user.setFollowing(target.memberName);
    }

    // from interface MemberProvider
    public void ditchFollower (ClientObject caller, int followerId,
                               InvocationService.InvocationListener listener)
        throws InvocationException
    {
        MemberObject leader = (MemberObject) caller;

        if (followerId == 0) { // Clear all followers
            for (MemberName follower : leader.followers) {
                MemberObject fmo = _locator.lookupMember(follower.getMemberId());
                if (fmo != null) {
                    fmo.setFollowing(null);
                }
            }
            leader.setFollowers(new DSet<MemberName>());

        } else { // Ditch a single follower
            if (leader.followers.containsKey(followerId)) {
                leader.removeFromFollowers(followerId);
            }
            MemberObject follower = _locator.lookupMember(followerId);
            if (follower != null && follower.following != null &&
                follower.following.getMemberId() == leader.getMemberId()) {
                follower.setFollowing(null);
            }
        }
    }

    // from interface MemberProvider
    public void setAway (final ClientObject caller, final boolean away, final String message)
        //throws InvocationException
    {
        final MemberObject user = (MemberObject) caller;
        user.setAwayMessage(away ? message : null);
        _bodyMan.updateOccupantStatus(user, away ? MsoyBodyObject.AWAY : MemberInfo.ACTIVE);
    }

    // from interface MemberProvider
    public void setAvatar (final ClientObject caller, final int avatarItemId, final float newScale,
                           final InvocationService.ConfirmListener listener)
        throws InvocationException
    {
        final MemberObject user = (MemberObject) caller;
        ensureNotGuest(user);

        if (avatarItemId == 0) {
            // a request to return to the default avatar
            finishSetAvatar(user, null, newScale, null, listener);
            return;
        }

        // otherwise, make sure it exists and we own it
        final ItemIdent ident = new ItemIdent(Item.AVATAR, avatarItemId);

        _invoker.postUnit(new RepositoryUnit("setAvatar") {
            @Override public void invokePersist () throws Exception {
                _avatar = (Avatar)_itemMan.loadItem(ident);
                if (_avatar == null) {
                    _failure = ItemCodes.E_NO_SUCH_ITEM;
                    return;
                }

                if (!user.isActive()) {
                    // TODO: remove this logging, it's not very interesting
                    log.info("User logged out during getItem", "avatar", _avatar,
                        "user", user.which());
                    _failure = InvocationCodes.INTERNAL_ERROR;
                    return;
                }

                if (user.getMemberId() != _avatar.ownerId) { // ensure that they own it
                    log.warning("Not user's avatar", "user", user.which(),
                        "ownerId", _avatar.ownerId,  _avatar.ownerId);
                    _failure = InvocationCodes.INTERNAL_ERROR;
                    return;
                }


                MemoriesRecord memrec = _memoryRepo.loadMemory(_avatar.getType(), _avatar.itemId);
                _memories = (memrec == null) ? null : memrec.toEntries();
            }

            @Override public void handleSuccess () {
                if (_failure == null && !user.isActive()) {
                    // TODO: remove this logging, it's not very interesting
                    log.info("User logged out during loadMemory", "avatar", _avatar,
                        "user", user.which());
                    _failure = InvocationCodes.INTERNAL_ERROR;
                }

                if (_failure != null) {
                    listener.requestFailed(_failure);

                } else {
                    finishSetAvatar(user, _avatar, newScale, _memories, listener);
                }
            }

            @Override public void handleFailure (final Exception pe) {
                log.warning(
                    "Unable to resolve avatar", "user", user.which(), "avatar", _avatar, pe);
                listener.requestFailed(InvocationCodes.INTERNAL_ERROR);
            }

            protected List<EntityMemoryEntry> _memories;
            protected Avatar _avatar;
            protected String _failure;
        });
    }

    // from interface MemberProvider
    public void setDisplayName (final ClientObject caller, final String name,
                                final InvocationService.ConfirmListener listener)
        throws InvocationException
    {
        final MemberObject user = (MemberObject) caller;
        ensureNotGuest(user);

        // TODO: verify entered string

        _invoker.postUnit(new PersistingUnit("setDisplayName", listener,
                                             "user", user.who(), "name", name) {
            @Override public void invokePersistent () throws Exception {
                _memberRepo.configureDisplayName(user.getMemberId(), name);
            }
            @Override public void handleSuccess () {
                user.updateDisplayName(name);
                _bodyMan.updateOccupantInfo(user,
                    new MemberInfo.NameUpdater(user.getVisibleName()));
                super.handleSuccess();
            }
        });
    }

    // from interface MemberProvider
    public void getDisplayName (final ClientObject caller, final int memberId,
                                final InvocationService.ResultListener listener)
        throws InvocationException
    {
        _invoker.postUnit(new PersistingUnit("getDisplayName", listener, "mid", memberId) {
            @Override public void invokePersistent ()
                throws Exception
            {
                _displayName = String.valueOf(_memberRepo.loadMemberName(memberId));
            }
            @Override public void handleSuccess ()
            {
                reportRequestProcessed(_displayName);
            }
            protected String _displayName;
        });
    }

    // from interface MemberProvider
    public void getGroupName (final ClientObject caller, final int groupId,
                              final InvocationService.ResultListener listener)
    {
        _invoker.postUnit(new PersistingUnit("getGroupName", listener, "gid", groupId) {
            @Override public void invokePersistent ()
                throws Exception
            {
                final GroupRecord rec = _groupRepo.loadGroup(groupId);
                _groupName = (rec == null) ? "" : rec.name;
            }
            @Override public void handleSuccess ()
            {
                reportRequestProcessed(_groupName);
            }
            protected String _groupName;
        });
    }

    // from interface MemberProvider
    public void acknowledgeWarning (final ClientObject caller)
    {
        final MemberObject user = (MemberObject) caller;
        _invoker.postUnit(new WriteOnlyUnit("acknowledgeWarning(" + user.getMemberId() + ")") {
            @Override public void invokePersist ()
                throws Exception
            {
                _memberRepo.clearMemberWarning(user.getMemberId());
            }
        });
    }

    // from interface MemberProvider
    public void setHomeSceneId (final ClientObject caller, final int ownerType, final int ownerId,
                                final int sceneId, final InvocationService.ConfirmListener listener)
        throws InvocationException
    {
        final MemberObject member = (MemberObject) caller;
        ensureNotGuest(member);

        _invoker.postUnit(new PersistingUnit("setHomeSceneId", listener, "who", member.who()) {
            @Override
            public void invokePersistent () throws Exception {
                final int memberId = member.getMemberId();
                final SceneRecord scene = _sceneRepo.loadScene(sceneId);
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
            @Override
            public void handleSuccess () {
                if (ownerType == MsoySceneModel.OWNER_TYPE_MEMBER) {
                    member.setHomeSceneId(sceneId);
                }
                super.handleSuccess();
            }
        });
    }

    // from interface MemberProvider
    public void getGroupHomeSceneId (final ClientObject caller, final int groupId,
                                     final InvocationService.ResultListener listener)
        throws InvocationException
    {
        _invoker.postUnit(new PersistingUnit("getHomeSceneId", listener, "gid", groupId) {
            @Override public void invokePersistent ()
                throws Exception
            {
                _homeSceneId = _groupRepo.getHomeSceneId(groupId);
            }
            @Override public void handleSuccess ()
            {
                reportRequestProcessed(_homeSceneId);
            }
            protected int _homeSceneId;
        });
    }

    // from interface MemberProvider
    public void complainMember (ClientObject caller, final int memberId, String complaint)
    {
        final MemberObject source = (MemberObject)caller;

        final EventRecord event = new EventRecord();
        event.source = Integer.toString(source.memberName.getMemberId());
        event.sourceHandle = source.memberName.toString();
        event.status = Event.OPEN;
        event.subject = complaint;

        // format and provide the complainer's chat history
        SimpleDateFormat df = new SimpleDateFormat("HH:mm:ss");
        StringBuilder chatHistory = new StringBuilder();
        for (ChatMessage msg : SpeakUtil.getChatHistory(source.memberName)) {
            UserMessage umsg = (UserMessage)msg;
            chatHistory.append(df.format(new Date(umsg.timestamp))).append(' ');
//             if (umsg instanceof ChannelMessage) {
//                 ChannelMessage cmsg = (ChannelMessage)umsg;
//                 chatHistory.append('[').append(ChatChannel.XLATE_TYPE[cmsg.channel.type]);
//                 chatHistory.append(':').append(cmsg.channel.ident).append("] ");
//             } else {
                chatHistory.append(StringUtil.pad(ChatCodes.XLATE_MODES[umsg.mode], 10)).append(' ');
//             }
            chatHistory.append(umsg.speaker);
            if (umsg.speaker instanceof MemberName) {
                chatHistory.append('(').append(((MemberName)umsg.speaker).getMemberId()).append(')');
            }
            chatHistory.append(": ").append(umsg.message).append('\n');
        }
        event.chatHistory = chatHistory.toString();

        // if the target is online, get thir name from their member object
        MemberObject target = _locator.lookupMember(memberId);
        if (target != null) {
            event.targetHandle = target.memberName.toString();
            event.target = Integer.toString(target.memberName.getMemberId());
        }

        _invoker.postUnit(new Invoker.Unit("addComplaint") {
            @Override public boolean invoke () {
                try {
                    _supportLogic.addComplaint(event, memberId);
                } catch (Exception e) {
                    log.warning("Failed to add complaint event [event=" + event + "].", e);
                    _failed = true;
                }
                return true;
            }
            @Override public void handleResult () {
                SpeakUtil.sendFeedback(source, MsoyCodes.GENERAL_MSGS,
                        _failed ? "m.complain_fail" : "m.complain_success");
            }
            protected boolean _failed = false;
        });
    }

    // from interface MemberProvider
    public void updateStatus (final ClientObject caller, final String status,
                              final InvocationService.InvocationListener listener)
        throws InvocationException
    {
        final MemberObject member = (MemberObject) caller;
        ensureNotGuest(member);

        final String commitStatus = StringUtil.truncate(status, Profile.MAX_STATUS_LENGTH);
        _invoker.postUnit(new PersistingUnit("updateStatus", listener, "who", member.who()) {
            @Override public void invokePersistent () throws Exception {
                _profileRepo.updateHeadline(member.getMemberId(), commitStatus);
            }
            @Override public void handleSuccess () {
                member.setHeadline(commitStatus);
                MemberNodeActions.updateFriendEntries(member);
            }
        });
    }

    // from interface MemberProvider
    public void trackVectorAssociation (final ClientObject caller, final String vector)
    {
        final MemberObject memObj = (MemberObject) caller;
        _eventLog.vectorAssociated(memObj.visitorInfo, vector);
    }

    // from interface MemberProvider
    public void emailShare (ClientObject caller, boolean isGame, String placeName, int placeId,
                            String[] emails, String message, InvocationService.ConfirmListener cl)
    {
        final MemberObject memObj = (MemberObject) caller;
        String page;
        if (isGame) {
            page = "world-game_g_" + placeId;
        } else {
            page = "world-s" + placeId;
        }
        String url = ServerConfig.getServerURL();
        if (memObj.isGuest()) {
            url += "#" + page;
        } else {
            // set them up with the affiliate info
            url += "welcome/" + memObj.getMemberId() + "/" + StringUtil.encode(page);
        }

        final String template = isGame ? "shareGameInvite" : "shareRoomInvite";
        // username is their authentication username which is their email address
        final String from = memObj.username.toString();
        for (final String recip : emails) {
            // this just passes the buck to an executor, so we can call it from the dobj thread
            _mailer.sendTemplateEmail(recip, from, template, "inviter", memObj.memberName,
                                      "name", placeName, "message", message, "link", url);
        }

        cl.requestProcessed();
    }

    // from interface MemberProvider
    public void getABTestGroup (final ClientObject caller, final String testName,
        final boolean logEvent, final InvocationService.ResultListener listener)
    {
        final MemberObject memObj = (MemberObject) caller;
        _invoker.postUnit(new PersistingUnit("getABTestGroup", listener) {
            @Override public void invokePersistent () throws Exception {
                _testGroup = _memberLogic.getABTestGroup(testName, memObj.visitorInfo, logEvent);
            }
            @Override public void handleSuccess () {
                reportRequestProcessed(_testGroup);
            }
            protected Integer _testGroup;
        });
    }

    // from interface MemberProvider
    public void trackClientAction (final ClientObject caller, final String actionName,
        final String details)
    {
        final MemberObject memObj = (MemberObject) caller;
        if (memObj.visitorInfo == null) {
            log.warning("Failed to log client action with null visitorInfo", "caller", caller.who(),
                        "actionName", actionName);
            return;
        }
        _eventLog.clientAction(memObj.getVisitorId(), actionName, details);
    }

    // from interface MemberProvider
    public void trackTestAction (final ClientObject caller, final String actionName,
        final String testName)
    {
        final MemberObject memObj = (MemberObject) caller;
        if (memObj.visitorInfo == null) {
            log.warning("Failed to log test action with null visitorInfo", "caller", caller.who(),
                        "actionName", actionName);
            return;
        }

        _invoker.postUnit(new Invoker.Unit("getABTestGroup") {
            @Override public boolean invoke () {
                int abTestGroup = -1;
                String actualTestName;
                if (testName != null) {
                    // grab the group without logging a tracking event about it
                    abTestGroup = _memberLogic.getABTestGroup(testName, memObj.visitorInfo, false);
                    actualTestName = testName;
                } else {
                    actualTestName = "";
                }
                _eventLog.testAction(memObj.getVisitorId(), actionName, actualTestName,
                    abTestGroup);
                return false;
            }
        });
    }

    // from interface MemberProvider
    public void loadAllBadges(ClientObject caller, InvocationService.ResultListener listener)
        throws InvocationException
    {
        long now = System.currentTimeMillis();
        List<EarnedBadge> badges = Lists.newArrayList();
        for (BadgeType type : BadgeType.values()) {
            int code = type.getCode();
            for (int ii = 0; ii < type.getNumLevels(); ii++) {
                String levelUnits = type.getRequiredUnitsString(ii);
                int coinValue = type.getCoinValue(ii);
                badges.add(new EarnedBadge(code, ii, levelUnits, coinValue, now));
            }
        }

        listener.requestProcessed(badges.toArray(new EarnedBadge[badges.size()]));
    }

    // from interface MemberProvider
    public void dispatchDeferredNotifications (ClientObject caller)
    {
        _notifyMan.dispatchDeferredNotifications((MemberObject)caller);
    }

    // from interface MemberProvider
    public void getHomePageGridItems (
        ClientObject caller, InvocationService.ResultListener listener)
        throws InvocationException
    {
        MemberObject memObj = (MemberObject) caller;
        final MemberExperience[] experiences = new MemberExperience[memObj.experiences.size()];
        memObj.experiences.toArray(experiences);
        final boolean onTour = memObj.onTour;
        final int memberId = memObj.getMemberId();
        final short badgesVersion = memObj.getLocal(MemberLocal.class).badgesVersion;

        _invoker.postUnit(new PersistingUnit("getHPGridItems", listener, "who", memObj.who()) {
            @Override public void invokePersistent () throws Exception {
                _result = _memberLogic.getHomePageGridItems(
                    memberId, experiences, onTour, badgesVersion);
            }

            @Override public void handleSuccess () {
                reportRequestProcessed(_result);
            }

            protected Object _result;
        });
    }

    /**
     * Check if the member's accumulated flow level matches up with their current level, and update
     * their current level if necessary
     */
    public void checkCurrentLevel (final MemberObject member)
    {
        int level = Arrays.binarySearch(_levelForFlow, member.accCoins);
        if (level < 0) {
            level = -1 * level - 1;
            final int length = _levelForFlow.length;
            // if the _levelForFlow array isn't big enough, double its size and flesh out the new
            // half
            if (level == length) {
                final int[] temp = new int[length*2];
                System.arraycopy(_levelForFlow, 0, temp, 0, length);
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
                @Override public void invokePersist () throws Exception {
                    final int memberId = member.getMemberId();
                    // record the new level, and grant a new invite
                    _memberRepo.setUserLevel(memberId, newLevel);
                    _memberRepo.grantInvites(memberId, 1);
                    // mark the level gain in their feed
                    _feedRepo.publishMemberMessage(
                        memberId, FeedMessageType.FRIEND_GAINED_LEVEL, String.valueOf(newLevel));
                }
                @Override public void handleSuccess () {
                    _notifyMan.notify(member, new LevelUpNotification(newLevel));
                }
                @Override public void handleFailure (final Exception pe) {
                    log.warning("Unable to set user level.",
                        "memberId", member.getMemberId(), "level", newLevel);
                }
            });
        }
    }

    /**
     * Boots a player from the server.  Must be called on the dobjmgr thread.
     *
     * @return true if the player was found and booted successfully
     */
    public boolean bootMember (final int memberId)
    {
        final MemberObject mobj = _locator.lookupMember(memberId);
        if (mobj != null) {
            final PresentsSession pclient = _clmgr.getClient(mobj.username);
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
    protected void ensureNotGuest (final MemberObject caller)
        throws InvocationException
    {
        if (caller.isGuest()) {
            throw new InvocationException(InvocationCodes.ACCESS_DENIED);
        }
    }

    /**
     * Updates the runtime information for an avatar change then finally commits the change to the
     * database.
     */
    protected void finishSetAvatar (
        final MemberObject user, final Avatar avatar, final float newScale,
        List<EntityMemoryEntry> memories, final InvocationService.ConfirmListener listener)
    {
        final Avatar prev = user.avatar;
        if (newScale != 0 && avatar != null) {
            avatar.scale = newScale;
        }

        // now we need to make sure that the two avatars have a reasonable touched time
        user.startTransaction();
        try {
            // unset the current avatar to avoid busy-work in avatarUpdatedOnPeer, but
            // we'll set the new avatar at the bottom...
            user.avatar = null;

            // NOTE: we are not updating the used/location fields of the cached avatars,
            // I don't think it's necessary, but it'd be a simple matter here...
            final long now = System.currentTimeMillis();
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
            user.getLocal(MemberLocal.class).memories = memories;

            // check if this player is already in a room (should be the case)
            PlaceManager pmgr = _placeReg.getPlaceManager(user.getPlaceOid());
            if (pmgr != null) {
                PlaceObject plobj = pmgr.getPlaceObject();
                if (plobj instanceof RoomObject) {
                    // if so, make absolutely sure the avatar memories are in place in the
                    // room before we update the occupant info (which triggers the avatar
                    // media change on the client).
                    user.getLocal(MemberLocal.class).putAvatarMemoriesIntoRoom(
                        (RoomObject)plobj, false);
                }
                // if the player wasn't in a room, the avatar memories will just sit in
                // MemberLocal storage until they do enter a room, which is proper
            }
            _bodyMan.updateOccupantInfo(user, new MemberInfo.AvatarUpdater(user));

        } finally {
            user.commitTransaction();
        }
        listener.requestProcessed();

        // this just fires off an invoker unit, we don't need the result, log it
        _itemMan.updateItemUsage(
            user.getMemberId(), prev, avatar, new ResultListener.NOOP<Object>() {
            @Override
            public void requestFailed (final Exception cause) {
                log.warning("Unable to update usage from an avatar change.");
            }
        });

        // now fire off a unit to update the avatar information in the database
        _invoker.postUnit(new RepositoryUnit("updateAvatar") {
            @Override public void invokePersist () throws Exception {
                if (avatar != null) {
                    if (newScale != 0 && avatar.scale != newScale) {
                        _itemLogic.getAvatarRepository().updateScale(avatar.itemId, newScale);
                    }
                }
                _memberRepo.configureAvatarId(user.getMemberId(),
                    (avatar == null) ? 0 : avatar.itemId);
            }

            @Override public void handleSuccess () {
                // yay!
            }

            @Override public void handleFailure (final Exception pe) {
                log.warning("Unable to set avatar", "user", user.which(), "avatar", avatar,
                    "error", pe);
            }
        });
    }

    protected void calculateLevelsForFlow (final int fromIndex)
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

    /**
     * Returns the most recently loaded set of greeter ids, sorted by last session time. This is
     * only used to construct the popular places snapshot, which figures out which greeters are
     * currently online and sorts and caches a separate list.
     */
    protected List<Integer> getGreeterIdsSnapshot ()
    {
        // using the same monitor here should be ok as the block is only 2 atoms on a 64 bit OS
        synchronized (_snapshotLock) {
            return _greeterIdsSnapshot;
        }
    }

    /** An internal object on which we synchronize to update/get snapshots. */
    protected final Object _snapshotLock = new Object();

    /** An interval that updates the popular places snapshot every so often. */
    protected Interval _ppInvalidator;

    /** The most recent summary of popular places in the whirled. */
    protected PopularPlacesSnapshot _ppSnapshot;

    /** Interval to update the greeter ids snapshot. */
    protected Interval _greeterIdsInvalidator;

    /** Snapshot of all currently configured greeters, sorted by last online. Refreshed
     * periodically. */
    protected List<Integer> _greeterIdsSnapshot;

    /** The array of memoized flow values for each level.  The first few levels are hard coded, the
     * rest are calculated according to the equation in calculateLevelsForFlow() */
    protected int[] _levelForFlow;

    // dependencies
    @Inject protected @MainInvoker Invoker _invoker;
    @Inject protected @BatchInvoker Invoker _batchInvoker;
    @Inject protected MsoyEventLogger _eventLog;
    @Inject protected ClientManager _clmgr;
    @Inject protected PresentsDObjectMgr _omgr;
    @Inject protected PlaceRegistry _placeReg;
    @Inject protected MailLogic _mailLogic;
    @Inject protected MailSender _mailer;
    @Inject protected MemberLogic _memberLogic;
    @Inject protected SupportLogic _supportLogic;
    @Inject protected BodyManager _bodyMan;
    @Inject protected BadgeManager _badgeMan;
    @Inject protected NotificationManager _notifyMan;
    @Inject protected ItemManager _itemMan;
    @Inject protected ItemLogic _itemLogic;
    @Inject protected MsoyPeerManager _peerMan;
    @Inject protected MemberLocator _locator;
    @Inject protected MemberRepository _memberRepo;
    @Inject protected GroupRepository _groupRepo;
    @Inject protected ProfileRepository _profileRepo;
    @Inject protected FeedRepository _feedRepo;
    @Inject protected MsoySceneRepository _sceneRepo;
    @Inject protected MemoryRepository _memoryRepo;

    /** The required flow for the first few levels is hard-coded */
    protected static final int[] BEGINNING_FLOW_LEVELS = { 0, 300, 900, 1800, 3000, 5100, 8100 };

    /** The frequency with which we recalculate our popular places snapshot. */
    protected static final long POP_PLACES_REFRESH_PERIOD = 30*1000;

    /** The frequency with which we recalculate our greeter ids snapshot. */
    protected static final long GREETERS_REFRESH_PERIOD = 30 * 60 * 1000;

    /** Maximum number of experiences we will keep track of per user. */
    protected static final int MAX_EXPERIENCES = 20;
}
