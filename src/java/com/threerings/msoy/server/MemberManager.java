//
// $Id$

package com.threerings.msoy.server;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import com.samskivert.jdbc.WriteOnlyUnit;
import com.samskivert.util.Interval;
import com.samskivert.util.Invoker;
import com.samskivert.util.ResultListener;
import com.samskivert.util.StringUtil;

import com.google.common.base.Function;
import com.google.common.collect.Lists;

import com.threerings.crowd.chat.server.ChatChannelManager;
import com.threerings.underwire.server.persist.EventRecord;
import com.threerings.underwire.web.data.Event;
import com.threerings.util.Name;
import com.threerings.cron.server.CronLogic;

import com.threerings.presents.annotation.EventThread;
import com.threerings.presents.annotation.MainInvoker;
import com.threerings.presents.client.InvocationService;
import com.threerings.presents.data.ClientObject;
import com.threerings.presents.data.InvocationCodes;
import com.threerings.presents.dobj.AttributeChangeListener;
import com.threerings.presents.dobj.AttributeChangedEvent;
import com.threerings.presents.server.ClientManager;
import com.threerings.presents.server.InvocationException;
import com.threerings.presents.server.InvocationManager;
import com.threerings.presents.server.PresentsDObjectMgr;
import com.threerings.presents.server.PresentsSession;
import com.threerings.presents.server.ReportManager;
import com.threerings.presents.util.PersistingUnit;

import com.threerings.crowd.chat.data.UserMessage;
import com.threerings.crowd.chat.server.ChatHistory;
import com.threerings.crowd.chat.server.SpeakUtil;
import com.threerings.crowd.data.BodyObject;
import com.threerings.crowd.chat.server.ChatChannelManager.ChatHistoryResult;
import com.threerings.crowd.server.BodyManager;
import com.threerings.crowd.server.PlaceManager;
import com.threerings.crowd.server.PlaceRegistry;

import com.threerings.orth.notify.data.GenericNotification;
import com.threerings.orth.notify.data.Notification;

import com.threerings.msoy.chat.data.MsoyChatChannel;
import com.threerings.msoy.chat.data.MsoyChatCodes;
import com.threerings.msoy.data.MemberExperience;
import com.threerings.msoy.data.MemberLocation;
import com.threerings.msoy.data.MemberMarshaller;
import com.threerings.msoy.data.MemberObject;
import com.threerings.msoy.data.MsoyCodes;
import com.threerings.msoy.data.all.GroupName;
import com.threerings.msoy.data.all.MemberName;
import com.threerings.msoy.group.server.ThemeRegistry;
import com.threerings.msoy.group.server.ThemeRegistry.ThemeEntry;
import com.threerings.msoy.group.server.persist.GroupRepository;
import com.threerings.msoy.server.persist.BatchInvoker;
import com.threerings.msoy.server.persist.MemberRepository;
import com.threerings.msoy.server.util.ServiceUnit;

import com.threerings.msoy.peer.server.MsoyPeerManager;

import com.threerings.msoy.admin.server.MsoyAdminManager;
import com.threerings.msoy.badge.server.BadgeManager;
import com.threerings.msoy.notify.data.LevelUpNotification;
import com.threerings.msoy.notify.server.MsoyNotificationManager;
import com.threerings.msoy.person.server.persist.ProfileRepository;
import com.threerings.msoy.profile.gwt.Profile;
import com.threerings.msoy.room.data.MemberInfo;
import com.threerings.msoy.underwire.server.SupportLogic;

import static com.threerings.msoy.Log.log;

/**
 * Manage msoy members.
 */
@Singleton @EventThread
public class MemberManager
    implements MemberLocator.Observer, MemberProvider
{
    /** Identifies a report that contains a dump of client object info. */
    public static final String CLIENTS_REPORT_TYPE = "clients";

    @Inject public MemberManager (InvocationManager invmgr, MsoyPeerManager peerMan,
                                  MemberLocator locator, ReportManager repMan)
    {
        // register our bootstrap invocation service
        invmgr.registerProvider(this, MemberMarshaller.class, MsoyCodes.MSOY_GROUP);

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

        // register a reporter for the clients report
        repMan.registerReporter(CLIENTS_REPORT_TYPE, new ReportManager.Reporter() {
            public void appendReport (StringBuilder buf, long now, long sinceLast, boolean reset) {
                for (ClientObject clobj : _clmgr.clientObjects()) {
                    BodyObject body = (clobj instanceof BodyObject) ?
                        (BodyObject) clobj : _locator.lookupMember(clobj);
                    if (body == null) {
                        buf.append("- ").append(clobj.getClass().getSimpleName()).append("\n");
                    } else {
                        appendBody(buf, body);
                    }
                }
            }
            protected void appendBody (StringBuilder buf, BodyObject body) {
                buf.append("- ").append(body.getClass().getSimpleName()).append(" [id=");
                Name vname = body.getVisibleName();
                if (vname instanceof MemberName) {
                    buf.append(((MemberName)vname).getId());
                } else {
                    buf.append(vname);
                }
                buf.append(", status=").append(body.status);
                buf.append(", loc=").append(body.location).append("]\n");
            }
        });
    }

    /**
     * Prepares our member manager for operation.
     */
    public void init ()
    {
        // unit to load greeters
        final Invoker.Unit loadGreeters = new Invoker.Unit("loadGreeterIds") {
            public boolean invoke () {
                List<Integer> greeterIds = _memberRepo.loadGreeterIds();
                synchronized (_snapshotLock) {
                    _greeterIdsSnapshot = greeterIds;
                }
                return false;
            }

            public long getLongThreshold () {
                return 10 * 1000;
            }
        };

        // do the initial load on the main thread so we are ready to go
        _greeterIdsSnapshot = _memberRepo.loadGreeterIds();

        // create the interval to post the unit
        _greeterIdsInvalidator = new Interval(_omgr) {
            @Override public void expired() {
                _batchInvoker.postUnit(loadGreeters);
            }
        };

        // loading all the greeter ids is expensive, so do it infrequently
        _greeterIdsInvalidator.schedule(GREETERS_REFRESH_PERIOD, true);

        _ppInvalidator = new Interval(_omgr) {
            @Override public void expired() {
                takeSnapshot();
            }
        };
        _ppInvalidator.schedule(0, POP_PLACES_REFRESH_PERIOD, true);

        // schedule member-related periodic jobs (note: these run on background threads)
        _cronLogic.scheduleEvery(1, "MemberManager purge entry vectors", new Runnable() {
            public void run () {
                _memberRepo.purgeEntryVectors();
                _memberRepo.purgeSessionRecords();
            }
        });
        _cronLogic.scheduleEvery(1, "MemberManager purge permaguests", new Runnable() {
            public void run () {
                long now = System.currentTimeMillis();
                List<Integer> weakIds = _memberRepo.loadExpiredWeakPermaguestIds(now);
                if (!weakIds.isEmpty()) {
                    _memberLogic.deleteMembers(weakIds);
                    int remaining = _memberRepo.countExpiredWeakPermaguestIds(now);
                    log.info("Purged weak permaguests", "count", weakIds.size(),
                        "remaining", remaining, "ids", weakIds);
                }
            }
        });
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

    @Override // from interface MemberLocator.Observer
    public void memberLoggedOn (final MemberObject member)
    {
        if (member.isViewer()) {
            return;
        }

        // add a listener for changes to the member's level
        member.addListener(new AttributeChangeListener() {
            public void attributeChanged (final AttributeChangedEvent event) {
                if (MemberObject.LEVEL.equals(event.getName())) {
                    _notifyMan.notify(member, new LevelUpNotification(event.getIntValue()));
                }
            }
        });

        // update badges
        _badgeMan.updateBadges(member);

        // TODO: give a time estimate, add custom message?
        if (_adminMan.willRebootSoon()) {
            _notifyMan.notify(
                member, new GenericNotification("m.reboot_soon", Notification.SYSTEM));
        }
    }

    @Override // from interface MemberLocator.Observer
    public void memberLoggedOff (final MemberObject member)
    {
        // nada
    }

    @Override // from interface MemberProvider
    public void inviteToBeFriend (final ClientObject caller, final int friendId,
                                  final InvocationService.ResultListener listener)
        throws InvocationException
    {
        final MemberObject user = _locator.requireMember(caller);
        _invoker.postUnit(new ServiceUnit("inviteToBeFriend", listener) {
            @Override public void invokePersistent () throws Exception {
                _autoFriended = _memberLogic.inviteToBeFriend(user.getMemberId(), friendId);
            }
            @Override public void handleSuccess () {
                reportRequestProcessed(_autoFriended);
            }
            protected boolean _autoFriended;
        });
    }

    @Override // from interface MemberProvider
    public void inviteAllToBeFriends (final ClientObject caller, final int memberIds[],
                                      final InvocationService.ConfirmListener listener)
    {
        final MemberObject user = _locator.requireMember(caller);
        if (memberIds.length == 0) {
            log.warning("Called inviteAllToBeFriends with no member ids", "caller", caller.who());
            listener.requestProcessed();
        }
        _invoker.postUnit(new ServiceUnit("inviteAllToBeFriends", listener) {
            List<Exception> failures = Lists.newArrayList();
            @Override public void invokePersistent () throws Exception {
                for (int friendId : memberIds) {
                    try {
                        _memberLogic.inviteToBeFriend(user.getMemberId(), friendId);
                    } catch (Exception ex) {
                        failures.add(ex);
                    }
                }
                // only report failure if no friend requests were sent
                if (failures.size() == memberIds.length) {
                    throw failures.get(0);
                }
            }
            @Override public void handleSuccess () {
                reportRequestProcessed();
                _eventLog.batchFriendRequestSent(
                    user.getMemberId(), memberIds.length, failures.size());
            }
        });
    }

    @Override // from interface MemberProvider
    public void bootFromPlace (final ClientObject caller, final int booteeId,
                               final InvocationService.ConfirmListener listener)
        throws InvocationException
    {
        final MemberObject user = _locator.requireMember(caller);
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

    @Override // from interface MemberProvider
    public void getCurrentMemberLocation (final ClientObject caller, final int memberId,
                                          final InvocationService.ResultListener listener)
        throws InvocationException
    {
        final MemberObject user = _locator.requireMember(caller);

        // ensure that the other member is a full friend (or we're support)
        if (!user.tokens.isSupport() &&
                !user.getLocal(MemberLocal.class).friendIds.contains(memberId)) {
            throw new InvocationException("e.not_a_friend");
        }

        final MemberLocation memloc = _peerMan.getMemberLocation(memberId);
        if (memloc == null) {
            throw new InvocationException("e.not_online");
        }
        listener.requestProcessed(memloc);
    }

    @Override // from interface MemberProvider
    public void setAway (
        ClientObject caller, String message, InvocationService.ConfirmListener listener)
        throws InvocationException
    {
        final MemberObject user = _locator.requireMember(caller);
        user.setAwayMessage(message);
        _bodyMan.updateOccupantInfo(user, new MemberInfo.Updater<MemberInfo>() {
            public boolean update (MemberInfo info) {
                if (info.isAway() == user.isAway()) {
                    return false;
                }
                info.updateIsAway(user);
                return true;
            }
        });
        listener.requestProcessed();
    }

    @Override // from interface MemberProvider
    public void setMuted (
        ClientObject caller, final int muteeId, final boolean muted,
        InvocationService.ConfirmListener listener)
        throws InvocationException
    {
        final MemberObject user = _locator.requireMember(caller);
        final int muterId = user.getMemberId();
        if (muterId == muteeId || muteeId == 0) {
            throw new InvocationException(InvocationCodes.E_INTERNAL_ERROR);
        }
        _invoker.postUnit(new ServiceUnit("setMuted()", listener) {
            @Override public void invokePersistent () throws Exception {
                _memberLogic.setMuted(muterId, muteeId, muted);
            }
        });
    }

    @Override // from interface MemberProvider
    public void setDisplayName (final ClientObject caller, final String name,
                                InvocationService.ConfirmListener listener)
        throws InvocationException
    {
        final MemberObject user = _locator.requireMember(caller);
        _invoker.postUnit(new ServiceUnit("setDisplayName", listener,
                                          "user", user.who(), "name", name) {
            @Override public void invokePersistent () throws Exception {
                _memberLogic.setDisplayName(user.getMemberId(), name, user.tokens.isSupport());
            }
        });
    }

    @Override // from interface MemberProvider
    public void getDisplayName (final ClientObject caller, final int memberId,
                                final InvocationService.ResultListener listener)
        throws InvocationException
    {
        _invoker.postUnit(new PersistingUnit("getDisplayName", listener, "mid", memberId) {
            @Override public void invokePersistent () throws Exception {
                _displayName = String.valueOf(_memberRepo.loadMemberName(memberId));
            }
            @Override public void handleSuccess () {
                reportRequestProcessed(_displayName);
            }
            protected String _displayName;
        });
    }

    @Override // from interface MemberProvider
    public void acknowledgeWarning (final ClientObject caller)
    {
        final MemberObject user = (MemberObject) _locator.forClient(caller);
        _invoker.postUnit(new WriteOnlyUnit("acknowledgeWarning(" + user.getMemberId() + ")") {
            @Override public void invokePersist () throws Exception {
                _memberRepo.clearMemberWarning(user.getMemberId());
            }
        });
    }

    @Override // from interface MemberProvider
    public void complainMember (ClientObject caller, final int memberId, String complaint)
    {
        MemberObject member = _locator.requireMember(caller);
        MemberObject target = _locator.lookupMember(memberId);
        complainMember(member, memberId,  complaint,
            (target != null) ? target.getMemberName() : null);
    }

    /**
     * Compile server-side information for a complaint against a MemberObject or a PlayerObject
     * and file it with the Underwire system. Note that this method must be called on the dobj
     * thread. The target name is optional (only available when the complainee is online).
     */
    public void complainMember (final BodyObject complainer, final int targetId,
        final String complaint, final MemberName optTargetName)
    {
        log.info("complainMember", "complainer", complainer.who(), "targetId", targetId,
            "complaint", complaint);
        ResultListener<ChatHistoryResult> listener = new ResultListener<ChatHistoryResult>() {
            @Override public void requestFailed (Exception cause) {
                log.warning("Failed to collect chat history for a complaint", "cause", cause);
                finish(null);
            }
            @Override public void requestCompleted (ChatHistoryResult result) {
                finish(result);
            }
            protected void finish (ChatHistoryResult result) {
                finishComplaint(result, complainer, targetId, complaint, optTargetName);
            }
        };
        _chatChanMgr.collectChatHistory(complainer.getVisibleName(), listener);
    }

    protected void finishComplaint (
        ChatHistoryResult result, final BodyObject complainer,
        final int targetId, String complaint, MemberName optTargetName)
    {
        MemberName complainerName = (MemberName)complainer.getVisibleName();

        final EventRecord event = new EventRecord();
        event.source = Integer.toString(complainerName.getId());
        event.sourceHandle = complainerName.toString();
        event.status = Event.OPEN;
        event.subject = complaint;

        // format and provide the complainer's chat history
        StringBuilder chatHistory = new StringBuilder();
        if (!result.failedNodes.isEmpty()) {
            chatHistory.append("Not all chat history could be collected; failed servers: ");
            StringUtil.toString(chatHistory, result.failedNodes);
            chatHistory.append(".\n\n");
        }
        // TODO: break up the formatting by channel and remove the "mode" infix
        SimpleDateFormat df = new SimpleDateFormat("HH:mm:ss");
        for (ChatHistory.Entry entry : _chatHistory.get(complainerName)) {
            UserMessage umsg = (UserMessage)entry.message;
            chatHistory.append(df.format(new Date(umsg.timestamp))).append(' ');
            MsoyChatChannel channel = (MsoyChatChannel)entry.channel;
            if (channel != null && channel.ident instanceof GroupName) {
                GroupName group = (GroupName)channel.ident;
                chatHistory.append("grp ").append(group).append('(')
                    .append(group.getGroupId()).append(')');
            } else {
                chatHistory.append(MsoyChatCodes.XLATE_MODES[umsg.mode]);
            }
            chatHistory.append('|').append(umsg.speaker);
            if (umsg.speaker instanceof MemberName) {
                int memberId = ((MemberName)umsg.speaker).getId();
                chatHistory.append('(').append(memberId).append(')');
            }
            chatHistory.append(": ").append(umsg.message).append('\n');
        }
        event.chatHistory = chatHistory.toString();

        if (optTargetName != null) {
            event.targetHandle = optTargetName.toString();
            event.target = Integer.toString(optTargetName.getId());
        }

        log.info("Running addComplaint invoker", "event", event);

        _invoker.postUnit(new Invoker.Unit("addComplaint") {
            @Override public boolean invoke () {
                try {
                    _supportLogic.addComplaint(event, targetId);
                } catch (Exception e) {
                    log.warning("Failed to add complaint event [event=" + event + "].", e);
                    _failed = true;
                }
                return true;
            }
            @Override public void handleResult () {
                SpeakUtil.sendFeedback(complainer, MsoyCodes.GENERAL_MSGS,
                        _failed ? "m.complain_fail" : "m.complain_success");
            }
            protected boolean _failed = false;
        });
    }

    @Override // from interface MemberProvider
    public void updateStatus (
        ClientObject caller, String status, InvocationService.InvocationListener listener)
        throws InvocationException
    {
        final MemberObject member = _locator.requireMember(caller);
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

    /**
     * Broadcast a notification to all members, on this server only.
     */
    public void notifyAll (Notification note)
    {
        for (ClientObject clobj : _clmgr.clientObjects()) {
            final MemberObject mem = _locator.lookupMember(clobj);
            if (mem != null && !mem.isViewer()) {
                _notifyMan.notify(mem, note);
            }
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

    protected void takeSnapshot ()
    {
        // the popular places snapshot function just wants an ordered list of group names
        List<GroupName> popularThemes = Lists.transform(
            _themeReg.getThemes(), new Function<ThemeEntry, GroupName>() {
                @Override public GroupName apply (ThemeEntry entry) {
                    return new GroupName(entry.name, entry.themeId);
                }
            });

        // calculate the PP snapshot
        PopularPlacesSnapshot newSnapshot = PopularPlacesSnapshot.takeSnapshot(
            _omgr, _peerMan, popularThemes, getGreeterIdsSnapshot());

        // then slide it into place
        synchronized (_snapshotLock) {
            _ppSnapshot = newSnapshot;
        }

        // finally decay the popularity of whatever themes this server hosts
        _themeReg.heartbeat(THEME_POPULARITY_DECAY, newSnapshot);
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

    // dependencies
    @Inject protected @BatchInvoker Invoker _batchInvoker;
    @Inject protected @MainInvoker Invoker _invoker;
    @Inject protected BadgeManager _badgeMan;
    @Inject protected BodyManager _bodyMan;
    @Inject protected ClientManager _clmgr;
    @Inject protected ChatChannelManager _chatChanMgr;
    @Inject protected ChatHistory _chatHistory;
    @Inject protected CronLogic _cronLogic;
    @Inject protected GroupRepository _groupRepo;
    @Inject protected MemberLocator _locator;
    @Inject protected MemberLogic _memberLogic;
    @Inject protected MemberRepository _memberRepo;
    @Inject protected MsoyAdminManager _adminMan;
    @Inject protected MsoyEventLogger _eventLog;
    @Inject protected MsoyPeerManager _peerMan;
    @Inject protected MsoyNotificationManager _notifyMan;
    @Inject protected PlaceRegistry _placeReg;
    @Inject protected PresentsDObjectMgr _omgr;
    @Inject protected ProfileRepository _profileRepo;
    @Inject protected SupportLogic _supportLogic;
    @Inject protected ThemeRegistry _themeReg;

    /** The frequency with which we recalculate our popular places snapshot. */
    protected static final long POP_PLACES_REFRESH_PERIOD = 30*1000;

    /** The frequency with which we recalculate our greeter ids snapshot. */
    protected static final long GREETERS_REFRESH_PERIOD = 30 * 60 * 1000;

    /** Maximum number of experiences we will keep track of per user. */
    protected static final int MAX_EXPERIENCES = 20;

    /** The exponential decay rate to apply each time popular places are recalculated. */
    protected static final double THEME_POPULARITY_DECAY = 0.999; // ca 25% per day
}
