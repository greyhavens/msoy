//
// $Id$

package com.threerings.msoy.server;

import static com.threerings.msoy.Log.log;

import java.util.List;
import java.util.Map;

import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import com.google.inject.Inject;

import com.samskivert.util.Tuple;

import com.threerings.io.Streamable;

import com.threerings.msoy.mail.server.MailLogic;

import com.threerings.presents.server.ClientManager;
import com.threerings.presents.server.ClientManager.ClientObserver;
import com.threerings.util.StreamableArrayIntSet;

import com.threerings.presents.data.ClientObject;
import com.threerings.presents.dobj.DSet;
import com.threerings.presents.server.ClientLocal;
import com.threerings.presents.server.PresentsSession;

import com.threerings.crowd.server.CrowdClientResolver;

import com.threerings.stats.data.Stat;
import com.threerings.stats.data.StatSet;
import com.threerings.stats.server.persist.StatRepository;

import com.threerings.msoy.data.LurkerName;
import com.threerings.msoy.data.MemberClientObject;
import com.threerings.msoy.data.MemberObject;
import com.threerings.msoy.data.all.MemberName;
import com.threerings.msoy.data.all.VisitorInfo;
import com.threerings.msoy.data.all.VizMemberName;
import com.threerings.msoy.server.ResolutionQueue.Listener;
import com.threerings.msoy.server.ResolutionQueue.Task;
import com.threerings.msoy.server.persist.MemberRecord;
import com.threerings.msoy.server.persist.MemberRepository;

import com.threerings.msoy.peer.server.MsoyPeerManager;
import com.threerings.msoy.web.gwt.MemberCard;

import com.threerings.msoy.badge.data.EarnedBadgeSet;
import com.threerings.msoy.badge.data.InProgressBadgeSet;
import com.threerings.msoy.badge.server.BadgeManager;
import com.threerings.msoy.badge.server.ServerStatSet;
import com.threerings.msoy.badge.server.persist.BadgeRepository;
import com.threerings.msoy.badge.server.persist.EarnedBadgeRecord;
import com.threerings.msoy.badge.server.persist.InProgressBadgeRecord;
import com.threerings.msoy.group.data.all.GroupMembership;
import com.threerings.msoy.group.server.persist.GroupRepository;
import com.threerings.msoy.item.data.all.Avatar;
import com.threerings.msoy.item.server.ItemLogic;
import com.threerings.msoy.item.server.ItemManager;
import com.threerings.msoy.item.server.persist.AvatarRecord;
import com.threerings.msoy.mail.server.persist.MailRepository;
import com.threerings.msoy.money.data.all.MemberMoney;
import com.threerings.msoy.money.server.MoneyLogic;
import com.threerings.msoy.person.server.persist.ProfileRecord;
import com.threerings.msoy.person.server.persist.ProfileRepository;
import com.threerings.msoy.room.server.persist.MemoriesRecord;
import com.threerings.msoy.room.server.persist.MemoryRepository;

/**
 * Used to configure msoy-specific client object data.
 */
public class MsoyClientResolver extends CrowdClientResolver
{
    @Override
    public ClientObject createClientObject ()
    {
        _mcobj = new MemberClientObject();
        _mcobj.position = _queue.getQueueSize();
        return _mcobj;
    }

    @Override
    public ClientLocal createLocalAttribute ()
    {
        return new MemberClientLocal();
    }

    @Override
    public void objectAvailable (ClientObject object)
    {
        _clobj = object;

        _mcobj.username = _username;

        // see if we have a member object forwarded from our peer
        _fwddata = _peerMan.getForwardedMemberObject(_username);
        if (_fwddata != null) {
            _memobj = _fwddata.left;
            _mcobj.memobj = _memobj;
            _mcobj.bodyOid = _memobj.getOid();
            for (Streamable local : _fwddata.right) {
                @SuppressWarnings("unchecked") Class<Streamable> lclass =
                    (Class<Streamable>)local.getClass();
                _clobj.setLocal(lclass, null); // delete any stock local
                _clobj.setLocal(lclass, local); // configure our forwarded data
            }
        }

        if (_mcobj.bodyOid != 0) {
            // we're done
            log.debug("Resolved forwarded session", "clobj", _clobj.who());

            reportSuccess();
            return;
        }

        // otherwise we're creating a new MemberObject
        _memobj = new MemberObject();

        // register it with the presents system
        _omgr.registerObject(_memobj);

        // hook the client object up with the body
        _mcobj.memobj = _memobj;

        // give the MemberObject the same (auth) username as we gave MemberClientObject
        _memobj.username = _username;

        // and put the local into place. this completes what's normally done in clientmanager.
        MemberLocal local = new MemberLocal();
        _memobj.setLocal(ClientLocal.class, local);

        // create a deferred notifications array so that we can track any notifications dispatched
        // to this client until they're ready to read them; we'd have MsoyNotificationManager do
        // this in a MemberLocator.Observer but we need to be sure this is filled in before any
        // other MemberLocator.Observers are notified because that's precisely when early
        // notifications are likely to be generated
        local.deferredNotifications = Lists.newArrayList();

        // do some stats-related hackery
        if (local.stats instanceof ServerStatSet) {
            ((ServerStatSet)local.stats).init(_badgeMan, _memobj);
        }

        // guests have MemberName as an auth username, members have Name
        if (_username instanceof MemberName) {
            // our auth username has our assigned name and member id, so use those
            final MemberName aname = (MemberName)_username;
            _memobj.memberName = new VizMemberName(
                aname.toString(), aname.getId(), MemberCard.DEFAULT_PHOTO);
            local.stats = new StatSet();
            local.badges = new EarnedBadgeSet();
            local.friendIds = new StreamableArrayIntSet(0);
            local.inProgressBadges = new InProgressBadgeSet();

            log.debug("Resolved guest session", "guest", _memobj);
            reportSuccess();
            announce();
            return;
        }

        if (_username instanceof LurkerName) {
            // we are lurker, we have no visible name to speak of
            _memobj.memberName = new VizMemberName("", 0, MemberCard.DEFAULT_PHOTO);
            local.stats = new StatSet();
            local.badges = new EarnedBadgeSet();
            local.friendIds = new StreamableArrayIntSet(0);
            local.inProgressBadges = new InProgressBadgeSet();

            log.debug("Resolved lurker session", "guest", _memobj);
            reportSuccess();
            announce();
            return;
        }

        // to finish creating MemberClientObject, we have to read MemberRecord on the invoker
        // Note: this will automatically call reportSuccess(), but not announce()
        _invoker.postUnit(this);
    }

    @Override // from ClientResolver
    protected void resolveClientData (ClientObject clobj)
        throws Exception
    {
        // load their record
        _mrec = _memberRepo.loadMember(_username.toString());
        if (_mrec == null) {
            throw new Exception("Missing member record for authenticated member? " +
                                "[username=" + _username + "]");
        }

        // and their mutelist
        int[] muted = _memberRepo.loadMutelist(_mrec.memberId);
        if (muted.length > 0) {
            MemberClientLocal local = _mcobj.getLocal(MemberClientLocal.class);
            local.mutedMemberIds = muted;
        }

        // support get resolved immediately, others will need to queue
        if (_mrec.isSupport()) {
            resolveMember();
        }
    }

    @Override // from ClientResolver
    protected void finishResolution (ClientObject clobj)
    {
        // if we're support, we already resolved, just finish up here
        if (_mrec.isSupport()) {
            _memobj.setParty(_peerMan.getPartySummary(_memobj.getMemberId()));

            // reportSuccess() will be called automatically by our superclass, but we
            // still want the availability of the MemberObject announced aftwards
            _needAnnounce = true;
            return;
        }

        // otherwise we queue up

        Task task = new Task() {
            @Override public void resolve () throws Exception {
                resolveMember();
            }
            @Override public void handle () throws Exception {
                _memobj.setParty(_peerMan.getPartySummary(_memobj.getMemberId()));
            }
        };
        Listener listener = new Listener() {
            @Override public void progress (int position) {
                _mcobj.setPosition(position);
            }
            @Override public void done (Task task) {
                // note: reportSuccess() has already been called by our superclass
                didLeaveQueue();
            }
            @Override public void failed (Task task, Exception e) {
                // destroy the dangling user object
                _omgr.destroyObject(_clobj.getOid());
                // let our listeners know that we're hosed
                reportFailure(e);
            }
        };

        _taskIx = _queue.queueTask(task, listener);
        log.info("Queueing resolution task", "who", _clobj.who(),
            "queue size", _queue.getQueueSize(), "taskIx", _taskIx);

        // update the queue size, things might have happened since the object was created
        _mcobj.position = _queue.getQueueSize();

        // register ourselves as interested in client sessions, so we can dequeue if needed
        _clmgr.addClientObserver(_sessionObserver);

        log.debug("Resolved unforwarded session", "clobj", _clobj.who());
    }

    protected void didDisconnect ()
    {
        log.info("Dequeueing entry due to disconnect", "who", _clobj.who(), "taskIx", _taskIx);
        if (_taskIx != 0) {
            // should always be true
            _queue.dequeueTask(_taskIx);
        }
    }

    protected void didLeaveQueue ()
    {
        // renounce our interest in the birth and death of session
        _clmgr.removeClientObserver(_sessionObserver);

        // and let the world know of the new memberobject's oid
        announce();
    }

    protected void announce ()
    {
        // setting the oid should complete the client's two-phase loading process
        _mcobj.setBodyOid(_memobj.getOid());
    }

    @Override
    protected void reportSuccess ()
    {
        super.reportSuccess();

        /** If finishResolution() flagged the need for an announce here, do it */
        if (_needAnnounce) {
            announce();
        }
    }

    /**
     * Resolve a msoy member. This is called on the invoker thread.
     */
    protected void resolveMember ()
        throws Exception
    {
        int memberId = _mrec.memberId;

        // start keeping track of what we're doing
        ResolutionProfiler profiler = new ResolutionProfiler();

        // load up their member information using on their authentication (account) name
        profiler.complete(Step.MEMBER);

        final MemberMoney money = _moneyLogic.getMoneyFor(memberId);
        profiler.complete(Step.MONEY);

        // NOTE: we avoid using the dobject setters here because we know the object is not out in
        // the wild and there's no point in generating a crapload of events during user
        // initialization when we know that no one is listening
        final ProfileRecord precord = _profileRepo.loadProfile(memberId);
        profiler.complete(Step.PROFILE);

        _memobj.memberName = new VizMemberName(_mrec.name, memberId,
            (precord == null) ? MemberCard.DEFAULT_PHOTO : precord.getPhoto());
        if (precord != null) {
            _memobj.headline = precord.headline;
        }

        // the place they call home
        _memobj.homeSceneId = _mrec.homeSceneId;

        // runtime money
        _memobj.coins = money.coins;
        _memobj.bars = money.bars;

        // assign the level, giving them a chance to level up too; note that we do this even though
        // the code tries to always keep the level in sync with accCoins. this is necessary because
        //   1. once upon a time the level was not kept in sync and we did not do a big migration
        //   2. there could have been bugs or errors with previous accCoins changes that inhibited
        //      the level up
        _memobj.level = _memberLogic.synchMemberLevel(memberId, _mrec.level, money.accCoins);

        // load up this member's persistent stats
        MemberLocal local = _memobj.getLocal(MemberLocal.class);
        List<Stat> stats = _statRepo.loadStats(memberId);
        local.stats = new ServerStatSet(stats.iterator(), _badgeMan, _memobj);
        profiler.complete(Step.STATS);

        // and their badges
        local.badgesVersion = _mrec.badgesVersion;
        local.badges = new EarnedBadgeSet(Iterables.transform(
                _badgeRepo.loadEarnedBadges(memberId), EarnedBadgeRecord.TO_BADGE));
        local.inProgressBadges = new InProgressBadgeSet(Iterables.transform(
                _badgeRepo.loadInProgressBadges(memberId), InProgressBadgeRecord.TO_BADGE));
        profiler.complete(Step.BADGES);

//        // load up any item lists they may have
//        List<ItemListInfo> itemLists = _itemMan.getItemLists(memberId);
//        _memobj.lists = new DSet<ItemListInfo>(itemLists);

        // fill in this member's raw friends list; the friend manager will update it later
        local.friendIds = _memberRepo.loadFriendIds(memberId);
        profiler.complete(Step.FRIENDS);

        // load up this member's group memberships
        _memobj.groups = new DSet<GroupMembership>(
            // we don't pass in member name here because we don't need it on the client
            _groupRepo.resolveGroupMemberships(memberId, null).iterator());
        profiler.complete(Step.GROUPS);

        // load up this member's current new mail count
        _memobj.newMailCount = _mailLogic.getUnreadConvoCount(memberId);
        profiler.complete(Step.MAIL);

        // load up their selected avatar, we'll configure it later
        if (_mrec.avatarId != 0) {
            AvatarRecord avatar = _itemLogic.getAvatarRepository().loadItem(_mrec.avatarId);
            if (avatar != null) {
                _memobj.avatar = (Avatar)avatar.toItem();
                MemoriesRecord memrec = _memoryRepo.loadMemory(avatar.getType(), avatar.itemId);
                local.memories = (memrec == null) ? null : memrec.toEntry();
            }
            profiler.complete(Step.AVATAR);
        }

        // resolve their persisted theme
        if (_mrec.themeGroupId != 0) {
            _memobj.theme = _groupRepo.loadGroupName(_mrec.themeGroupId);
            profiler.complete(Step.THEME);
        }

        // for players, resolve this here from the database.
        // guests will get resolution later on, in MsoySession.sessionWillStart()
        _memobj.visitorInfo = new VisitorInfo(_mrec.visitorId, true);

        // Load up the member's experiences
        //_memobj.experiences = new DSet<MemberExperience>(
        //        _memberLogic.getExperiences(memberId));
        profiler.complete();
    }

    protected enum Step {
        MEMBER, MONEY, PROFILE, STATS, BADGES, FRIENDS, GROUPS, MAIL, AVATAR, THEME,
    }

    /**
     * Keep track of how much time we spend performing each logically discrete step in
     * the MemberObject (and MemberLocal) resolution process.
     */
    protected class ResolutionProfiler
    {
        protected void complete (Step step)
            throws ClientDisconnectedException
        {
            // sanity check
            if (_profile.containsKey(step)) {
                throw new IllegalStateException("Already completed step: " + step);
            }

            // abort resolution process if client disconnected
            try {
                enforceConnected();

            } catch (ClientDisconnectedException cde) {
                // let the world know how far we got
                log.info("Disconnected", "step", step, "profile", buildProfile());
                // then finish freaking out
                throw cde;
            }

            // else register how long we took to do this one step
            long now = System.nanoTime();
            _profile.put(step, (int) ((now - _stamp) / 1000));
            _stamp = now;
        }

        protected void complete ()
        {
            // if we're entirely done, let the world know
            log.info("Completed", "profile", buildProfile());
        }

        protected String buildProfile ()
        {
            StringBuilder builder = new StringBuilder("[ ");
            boolean first = true;
            for (Step step : Step.values()) {
                if (_profile.containsKey(step)) {
                    if (!first) {
                        builder.append(", ");
                    }
                    first = false;
                    builder.append(step.toString()).append("=").append(_profile.get(step));
                }
            }
            return builder.append(" ]").toString();
        }

        protected Map<Step, Integer> _profile = Maps.newHashMap();
        protected long _stamp = System.nanoTime();
    }

    /** An observer that dequeues our resolution task if we disconnect. */
    protected ClientObserver _sessionObserver = new ClientObserver() {
        public void clientSessionDidStart (PresentsSession session) { /* don't care */ }
        public void clientSessionDidEnd (PresentsSession session) {
            if (session.getAuthName().equals(_username)) {
                // the very session we're resolving for disconnected: terminate!
                didDisconnect();
            }
        }
    };

    /** Info on our member object forwarded from another server. */
    protected Tuple<MemberObject,Streamable[]> _fwddata;

    /** Our actual client object, constructed very early in the process. */
    protected MemberClientObject _mcobj;

    /** Our actual member object, which needs to be explicitly announced. */
    protected MemberObject _memobj;

    /** Our MemberRecord, which needs to be passed this way from invoker to dobj thread. */
    protected MemberRecord _mrec;

    /** The resolution task we send off to the {@link ResolutionQueue}. */
    protected int _taskIx;

    /** Whether or not we need to automatically announce() after reportSuccess() finishes. */
    protected boolean _needAnnounce;

    @Inject protected BadgeManager _badgeMan;
    @Inject protected BadgeRepository _badgeRepo;
    @Inject protected ClientManager _clmgr;
    @Inject protected GroupRepository _groupRepo;
    @Inject protected ItemLogic _itemLogic;
    @Inject protected ItemManager _itemMan;
    @Inject protected MailLogic _mailLogic;
    @Inject protected MailRepository _mailRepo;
    @Inject protected MemberLogic _memberLogic;
    @Inject protected MemberRepository _memberRepo;
    @Inject protected MemoryRepository _memoryRepo;
    @Inject protected MoneyLogic _moneyLogic;
    @Inject protected MsoyPeerManager _peerMan;
    @Inject protected ProfileRepository _profileRepo;
    @Inject protected ResolutionQueue _queue;
    @Inject protected StatRepository _statRepo;
}
