//
// $Id$

package com.threerings.msoy.server;

import java.util.Collections;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import com.samskivert.util.IntMap;
import com.samskivert.util.IntMaps;
import com.samskivert.util.ObserverList;
import com.threerings.util.Name;

import com.threerings.presents.annotation.EventThread;
import com.threerings.presents.server.PresentsDObjectMgr;

import com.threerings.crowd.data.BodyObject;
import com.threerings.crowd.server.BodyLocator;

import com.threerings.msoy.admin.server.MsoyAdminManager;
import com.threerings.msoy.data.MemberObject;
import com.threerings.msoy.data.all.MemberName;

import java.util.Collection;

/**
 * Customizes the {@link BodyLocator} and provides a means to lookup a member by id.
 */
@Singleton @EventThread
public class MemberLocator extends BodyLocator
{
    /** Used to notify server entities of member logon and logoff. */
    public static interface Observer {
        /** Called when a member logs onto this server. */
        public void memberLoggedOn (MemberObject memobj);

        /** Called when a member log off of this server. */
        public void memberLoggedOff (MemberObject memobj);
    }

    /**
     * Returns the member object for the user identified by the given ID if they are online
     * currently, null otherwise.
     */
    public MemberObject lookupMember (int memberId)
    {
        _omgr.requireEventThread();
        return _online.get(memberId);
    }

    /**
     * Returns the member object for the user identified by the given name if they are online
     * currently, null otherwise.
     */
    public MemberObject lookupMember (MemberName name)
    {
        return lookupMember(name.getMemberId());
    }

    /**
     * Returns an <i>unmodifiable</i> collection of members currently online.
     */
    public Collection<MemberObject> getMembersOnline ()
    {
        _omgr.requireEventThread();
        return Collections.unmodifiableCollection(_online.values());
    }

    /**
     * Adds a member session observer.
     */
    public void addObserver (Observer observer)
    {
        _observers.add(observer);
    }

    /**
     * Removes a member session observer.
     */
    public void removeObserver (Observer observer)
    {
        _observers.remove(observer);
    }

    /**
     * Called when a member starts their session to associate the name with the member's
     * distributed object.
     */
    public void memberLoggedOn (final MemberObject memobj)
    {
        _online.put(memobj.memberName.getMemberId(), memobj);

        // notify our observers
        _observers.apply(new ObserverList.ObserverOp<Observer>() {
            public boolean apply (Observer observer) {
                observer.memberLoggedOn(memobj);
                return true;
            }
        });

        // update our members online count in the status object
        _adminMan.statObj.setMembersOnline(_clmgr.getClientCount());
    }

    /**
     * Called when a member ends their session to clear their name to member object mapping.
     */
    public void memberLoggedOff (final MemberObject memobj)
    {
        _online.remove(memobj.memberName.getMemberId());

        // notify our observers
        _observers.apply(new ObserverList.ObserverOp<Observer>() {
            public boolean apply (Observer observer) {
                observer.memberLoggedOff(memobj);
                return true;
            }
        });

        // update our members online count in the status object
        _adminMan.statObj.setMembersOnline(_clmgr.getClientCount());
    }

    @Override // from BodyLocator
    public BodyObject lookupBody (Name visibleName)
    {
        _omgr.requireEventThread();
        return _online.get(((MemberName) visibleName).getMemberId());
    }

    /** A mapping from member name to member object for all online members. */
    protected IntMap<MemberObject> _online = IntMaps.newHashIntMap();

    /** A list of member session observers. */
    protected ObserverList<Observer> _observers = ObserverList.newFastUnsafe();

    @Inject protected PresentsDObjectMgr _omgr;
    @Inject protected MemberManager _memberMan;
    @Inject protected FriendManager _friendMan;
    @Inject protected MsoyAdminManager _adminMan;
}
