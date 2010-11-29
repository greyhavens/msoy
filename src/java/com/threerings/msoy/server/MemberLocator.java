//
// $Id$

package com.threerings.msoy.server;

import java.util.Collections;
import java.util.Map;

import com.google.common.collect.Maps;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import com.samskivert.util.ObserverList;

import com.threerings.presents.data.ClientObject;
import com.threerings.util.Name;

import com.threerings.presents.annotation.EventThread;
import com.threerings.presents.server.PresentsDObjectMgr;

import com.threerings.crowd.data.BodyObject;
import com.threerings.crowd.server.BodyLocator;

import com.threerings.msoy.data.MemberClientObject;
import com.threerings.msoy.data.MemberObject;
import com.threerings.msoy.data.all.MemberName;

import java.util.Collection;

import static com.threerings.msoy.Log.log;

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
     * On the assumption that the given argument is a @{link }MemberClientLoader}, look up its
     * associated {@link MemberObject} and return it. If the loader is not in fact of that type,
     * or the member has not finished loading, return null.
     */
    public MemberObject lookupMember (ClientObject loader)
    {
        if (loader instanceof MemberClientObject) {
            return ((MemberClientObject) loader).memobj;
        }
        return null;
    }

    /**
     * On the assumption that the given argument is a @{link }MemberClientLoader}, look up its
     * associated {@link MemberObject} and return it. If the loader is not in fact of that type,
     * throw a runtime exception.
     */
    public MemberObject requireMember (ClientObject loader)
    {
        if (loader instanceof MemberClientObject) {
            MemberObject memobj = ((MemberClientObject) loader).memobj;
            if (memobj != null) {
                return memobj;
            }
            log.warning("Invocation from client that's not connected", "client", loader);
            throw new IllegalStateException("Client not connected");
        }
        log.warning("Expected source to be MemberClientObject", "client", loader);
        throw new IllegalStateException("Unexpected client type");
    }

    /**
     * Returns the member object for the user identified by the given name if they are online
     * currently, null otherwise.
     */
    public MemberObject lookupMember (MemberName name)
    {
        return lookupMember(name.getId());
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
        _online.put(memobj.memberName.getId(), memobj);

        // notify our observers
        _observers.apply(new ObserverList.ObserverOp<Observer>() {
            public boolean apply (Observer observer) {
                observer.memberLoggedOn(memobj);
                return true;
            }
        });
    }

    /**
     * Called when a member ends their session to clear their name to member object mapping.
     */
    public void memberLoggedOff (final MemberObject memobj)
    {
        _online.remove(memobj.memberName.getId());

        // notify our observers
        _observers.apply(new ObserverList.ObserverOp<Observer>() {
            public boolean apply (Observer observer) {
                observer.memberLoggedOff(memobj);
                return true;
            }
        });
    }

    @Override // from BodyLocator
    public BodyObject lookupBody (Name visibleName)
    {
        _omgr.requireEventThread();
        return _online.get(((MemberName) visibleName).getId());
    }

    @Override // from BodyLocator
    public BodyObject forClient (ClientObject client)
    {
        if (client instanceof MemberClientObject) {
            return requireMember(client);
        } else {
            return super.forClient(client);
        }
    }

    /** A mapping from member name to member object for all online members. */
    protected Map<Integer, MemberObject> _online = Maps.newHashMap();

    /** A list of member session observers. */
    protected ObserverList<Observer> _observers = ObserverList.newFastUnsafe();

    @Inject protected PresentsDObjectMgr _omgr;
}
