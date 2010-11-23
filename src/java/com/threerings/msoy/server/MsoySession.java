//
// $Id$

package com.threerings.msoy.server;

import com.google.inject.Inject;

import com.samskivert.jdbc.WriteOnlyUnit;
import com.samskivert.util.Invoker;
import com.samskivert.util.StringUtil;
import com.threerings.presents.annotation.MainInvoker;
import com.threerings.presents.dobj.AttributeChangeListener;
import com.threerings.presents.dobj.AttributeChangedEvent;
import com.threerings.presents.net.AuthRequest;
import com.threerings.presents.net.BootstrapData;
import com.threerings.presents.server.net.PresentsConnection;

import com.threerings.crowd.data.OccupantInfo;

import com.threerings.stats.data.Stat;
import com.threerings.stats.data.StatSet;
import com.threerings.stats.server.persist.StatRepository;

import com.threerings.whirled.server.WhirledSession;

import com.threerings.msoy.admin.server.RuntimeConfig;
import com.threerings.msoy.room.server.persist.MemoriesRecord;
import com.threerings.msoy.room.server.persist.MemoryRepository;

import com.threerings.msoy.data.LurkerName;
import com.threerings.msoy.data.MemberObject;
import com.threerings.msoy.data.MsoyBootstrapData;
import com.threerings.msoy.data.MsoyTokenRing;
import com.threerings.msoy.data.StatType;
import com.threerings.msoy.data.WorldCredentials;
import com.threerings.msoy.data.all.VisitorInfo;
import com.threerings.msoy.server.persist.MemberRepository;

import static com.threerings.msoy.Log.log;

/**
 * Represents an attached Msoy client on the server-side.
 */
public class MsoySession extends WhirledSession
{
    /**
     * Called by the peer manager to let us know that our session was forwarded to another server.
     * When this session ends we'll know that it's not *really* the end of the user's session,
     * they're just moving to another server.
     */
    public void setSessionForwarded (boolean sessionForwarded)
    {
        _sessionForwarded = sessionForwarded;
    }

    /**
     * Returns the number of non-idle seconds this client has passed during this session.
     * <em>Note:</em> this value is only valid <em>before</em> the session has actually ended.
     */
    public int getSessionSeconds ()
    {
        int connectTime = (int)((System.currentTimeMillis() - _networkStamp) / 1000L);
        return connectTime - _idleTracker.getIdleTime();
    }

    @Override // from PresentsSession
    protected BootstrapData createBootstrapData ()
    {
        return new MsoyBootstrapData();
    }

    @Override // from PresentsSession
    protected void populateBootstrapData (BootstrapData data)
    {
        super.populateBootstrapData(data);

        MsoyBootstrapData mData = (MsoyBootstrapData) data;

        MemberLocal local = _memobj.getLocal(MemberLocal.class);
        if (local.mutedMemberIds != null) {
            mData.mutedMemberIds = local.mutedMemberIds;
            local.mutedMemberIds = null;
        }
        int expiration = (int) ((System.currentTimeMillis() / 1000) + 7 * 24 * 3600);
    }

    @Override // from PresentsSession
    protected void sessionWillStart ()
    {
        super.sessionWillStart();

        _memobj = (MemberObject) _clobj;
        _memobj.setAccessController(MsoyObjectAccess.USER);
        _memobj.addListener(_idleTracker);

        AuthenticationDomain.Account acct = (AuthenticationDomain.Account)_authdata;

        // If this is an embedded world session for a freshly created permaguest, we have not
        // yet associated their visitorId with a tracker and must do so here. But only once, so
        // for now we look in the database whether or not the tracker already exists. We might
        // want to create a field in {@link MsoyCredentials} that suggests a visitorId was
        // freshly created on the client.
        if (_memobj.isPermaguest() && !_memobj.isViewer()) {
            WorldCredentials creds = (WorldCredentials)getCredentials();
            final String vector = StringUtil.getOr(creds.vector, "world_session");
            final int memberId = _memobj.memberName.getMemberId();

            final VisitorInfo info;
            if (creds.visitorId != null) {
                info = new VisitorInfo(creds.visitorId, false);

            } else if (_memobj.visitorInfo != null) {
                info = _memobj.visitorInfo;

            } else {
                log.warning("No visitorId in WorldCredentials", "memberId", memberId);
                info = new VisitorInfo();
            }

            log.info("MsoySession", "info", info, "vector", vector, "memberId", memberId);

            _invoker.postUnit(new WriteOnlyUnit("maybeNoteNewVisitor") {
                public void invokePersist () throws Exception {
                    if (_memberRepo.entryVectorExists(info.id) != null) {
                        return;
                    }
                    _memberLogic.noteNewVisitor(info, false, vector, null, memberId);

                    // DEBUG
                    log.info("VisitorInfo created", "info", info, "reason", "MsoySession",
                        "vector", vector, "memberId", memberId);
                }
            });
        }

        if (acct != null) {
            _memobj.setTokens(acct.tokens);
        } else {
            _memobj.setTokens(new MsoyTokenRing());
        }

        // start active/idle metrics on this server - the player starts out active
        _memobj.getLocal(MemberLocal.class).metrics.idle.init(true);

        // let our various server entities know that this member logged on
        _locator.memberLoggedOn(_memobj);
    }

    @Override // from PresentsSession
    protected void resumeSession (AuthRequest req, PresentsConnection conn)
    {
        // note that we're in the middle of resuming a session so that we don't end our session
        // when the old connection is closed
        _resumingSession = true;

        super.resumeSession(req, conn);
    }

    @Override // from PresentsSession
    protected void sessionConnectionClosed ()
    {
        super.sessionConnectionClosed();

        // end our session when the connection is closed, it's easy enough to get back to where you
        // were with a browser reload
        if (!_resumingSession && // but not if we're not in the middle of resuming our session
            _memobj != null) {   // and not if we never fully started our session
            endSession();
        }
    }

    @Override // from PresentsSession
    protected void sessionWillResume ()
    {
        super.sessionWillResume();

        // we're out of the woods now and can clear our resuming flag
        _resumingSession = false;
    }

    @Override // from PresentsSession
    protected void sessionDidEnd ()
    {
        super.sessionDidEnd();

        if (_memobj == null) {
            return;
        }

        // let our various server entities know that this member logged off
        _locator.memberLoggedOff(_memobj);

        // remove our idle tracker
        _memobj.removeListener(_idleTracker);

        // if this session was forwarded to another server, we can stop here
        if (_sessionForwarded) {
            _memobj = null;
            return;
        }
        final MemberLocal local = _memobj.getLocal(MemberLocal.class);

        // if this this was a player or guest (but not a lurker), log their stats
        if (!(_memobj.username instanceof LurkerName)) {
            String sessTok = ((WorldCredentials)getCredentials()).sessionToken;
            local.metrics.save(_memobj);
            _eventLog.logPlayerMetrics(_memobj, sessTok);
        }

        // if this was a member, record some end of session related info to the database
        if (!_memobj.isViewer()) {
            final int activeMins = Math.round(
                (local.sessionSeconds + _connectTime - _idleTracker.getIdleTime()) / 60f);
            final int memberId = _memobj.getMemberId();
            final StatSet stats = local.stats;
            //final List<MemberExperience> experiences = Lists.newArrayList(_memobj.experiences);
            final MemoriesRecord memrec = (local.memories == null || !local.memories.modified)
                ? null : new MemoriesRecord(local.memories);

            log.info("Session ended", "id", memberId, "amins", activeMins);
            stats.incrementStat(StatType.MINUTES_ACTIVE, activeMins);
            _invoker.postUnit(new WriteOnlyUnit("sessionDidEnd:" + _memobj.memberName) {
                @Override public void invokePersist () throws Exception {
//                    long startStamp = System.currentTimeMillis();
//                    List<Long> resolutionStamps = Lists.newArrayList();

                    // write out any modified stats
                    _statRepo.writeModified(memberId, stats.toArray(new Stat[stats.size()]));
//                    resolutionStamps.add(System.currentTimeMillis() - startStamp);

                    // increment their session and minutes online counters
                    _memberRepo.noteSessionEnded(memberId, activeMins);
//                    resolutionStamps.add(System.currentTimeMillis() - startStamp);

                    // save their experiences
                    //_memberLogic.saveExperiences(memberId, experiences);
                    // save any modified avatar memories
                    if (memrec != null) {
                        _memoryRepo.storeMemories(memrec);
//                        resolutionStamps.add(System.currentTimeMillis() - startStamp);
                    }
//                     log.info("Session persisted", "memberId", memberId,
//                              "timing", resolutionStamps);
                }
            });
        }

        // finally clear out our _memobj reference
        _memobj = null;
    }

    protected class IdleTracker implements AttributeChangeListener
    {
        public int getIdleTime () {
            int idleTime = _idleTime, idleCount = _idleCount;
            if (isIdle(_memobj.status)) {
                idleTime += (int)((System.currentTimeMillis() -
                                   _memobj.getLocal(MemberLocal.class).statusTime) / 1000L);
                idleCount++;
            }
            // TODO: add (idleCount * IDLE_TIMEOUT) to account for the time that a player was
            // *actually* idle before we discovered it?
            return idleTime;
        }

        public void attributeChanged (AttributeChangedEvent event) {
            if (event.getName().equals(MemberObject.STATUS)) {
                MemberLocal local = _memobj.getLocal(MemberLocal.class);
                local.metrics.idle.save(_memobj);

                boolean idle = isIdle((Byte)event.getValue());
                local.metrics.idle.init(!idle);
                if (idle) {
                    // log.info(_memobj.who() + " is idle.");
                    _idleStamp = local.statusTime;

                } else if (_idleStamp > 0) {
                    int idleSecs = (int)((local.statusTime - _idleStamp) / 1000L);
                    // log.info(_memobj.who() + " was idle for " + idleSecs + " seconds.");
                    _idleTime += idleSecs;
                    _idleCount++;
                    _idleStamp = 0L;
                }
            }
        }

        protected boolean isIdle (byte status) {
            return (status == OccupantInfo.IDLE);
        }

        protected int _idleTime;
        protected int _idleCount;
        protected long _idleStamp;
    }

    /** A casted reference to the userobject. */
    protected MemberObject _memobj;

    /** Tracks the time this client spends being idle. */
    protected IdleTracker _idleTracker = new IdleTracker();

    /** Only valid in {@link #sessionDidEnd}, lets us know if the session is truly over or if the
     * member just went to another server. */
    protected boolean _sessionForwarded;

    /** Used to avoid ending our session when we're in the middle of resuming it. */
    protected volatile boolean _resumingSession;

    // dependent services
    @Inject protected @MainInvoker Invoker _invoker;
    @Inject protected MemberLocator _locator;
    @Inject protected MemberLogic _memberLogic;
    @Inject protected MemberRepository _memberRepo;
    @Inject protected MemoryRepository _memoryRepo;
    @Inject protected MsoyEventLogger _eventLog;
    @Inject protected RuntimeConfig _runtime;
    @Inject protected StatRepository _statRepo;
}
