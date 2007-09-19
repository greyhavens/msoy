
//
// $Id$

package com.threerings.msoy.swiftly.client.model;

import java.util.Comparator;
import java.util.HashSet;
import java.util.Set;
import java.util.TreeSet;

import com.threerings.crowd.client.OccupantObserver;
import com.threerings.crowd.data.OccupantInfo;
import com.threerings.msoy.data.MemberObject;
import com.threerings.msoy.data.all.MemberName;
import com.threerings.msoy.swiftly.client.SwiftlyContext;
import com.threerings.msoy.swiftly.client.event.AccessControlListener;
import com.threerings.msoy.swiftly.client.event.OccupantListener;
import com.threerings.msoy.swiftly.client.model.ProjectModelDelegate.FailureCode;
import com.threerings.msoy.swiftly.data.BuildResult;
import com.threerings.msoy.swiftly.data.ProjectRoomObject;
import com.threerings.msoy.web.data.SwiftlyProject;
import com.threerings.presents.client.Client;
import com.threerings.presents.client.InvocationService.ResultListener;
import com.threerings.presents.dobj.AttributeChangeListener;
import com.threerings.presents.dobj.AttributeChangedEvent;
import com.threerings.presents.dobj.EntryAddedEvent;
import com.threerings.presents.dobj.EntryRemovedEvent;
import com.threerings.presents.dobj.EntryUpdatedEvent;
import com.threerings.presents.dobj.MessageEvent;
import com.threerings.presents.dobj.MessageListener;
import com.threerings.presents.dobj.SetListener;

/**
 * Manages various SwiftlyProject related data and services.
 */
public class ProjectModel
    implements SetListener, AttributeChangeListener, MessageListener, OccupantObserver
{
    public ProjectModel (ProjectRoomObject roomObj, SwiftlyContext ctx)
    {
        _roomObj = roomObj;
        _client = ctx.getClient();
        _member = ((MemberObject)ctx.getClient().getClientObject()).memberName;
        _roomObj.addListener(this);
    }

    /**
     * Returns the project id for the project currently being displayed in the client.
     */
    public int getProjectId ()
    {
        return _roomObj.project.projectId;
    }

    /**
     * Returns the SwiftlyProject object for this project, cloned and disconnected from the
     * SwiftlyProject in the dobject.
     */
    public SwiftlyProject getProject ()
    {
        return _roomObj.project.klone();
    }

    /**
     * Returns the set (sorted) of collaborators for this project.
     */
    public Set<MemberName> getCollaborators ()
    {
        Set<MemberName> set = new TreeSet<MemberName>(new Comparator<MemberName>() {
            public int compare (MemberName m1, MemberName m2) {
                return MemberName.compareNames(m1, m2);
            }
        });
        for (MemberName member : _roomObj.collaborators) {
            set.add(member);
        }
        return set;
    }

    /**
     * Returns the number of collaborators currently working on the project.
     */
    public int occupantCount ()
    {
        return _roomObj.occupants.size();
    }

    /**
     * Returns the last BuildResult for this user, or null if the user has never performed a build.
     */
    public BuildResult getLastBuildResult ()
    {
        return _lastResult;
    }

    /**
     * Return the number of milliseconds the last build took, or a useful default guess.
     */
    public int getLastBuildTime ()
    {
        if (_lastResult == null) {
            return DEFAULT_BUILD_TIME;
        }

        return (int)_lastResult.getBuildTime();
    }

    /**
     * Returns true if the client has owner access to the project.
     */
    public boolean haveOwnerAccess ()
    {
        return _roomObj.project.ownerId == _member.getMemberId();
    }

    /**
     * Returns true if the client has write access to the project.
     */
    public boolean haveWriteAccess ()
    {
        return _roomObj.hasWriteAccess(_member);
    }

    /**
     * Returns true if the client has read access to the project.
     */
    public boolean haveReadAccess ()
    {
        return _roomObj.hasReadAccess(_member);
    }

    /**
     * Requests that the project is built.
     */
    public void buildProject (final RequestId requestId, final ProjectModelDelegate delegate)
    {
        _roomObj.service.buildProject(_client, new ResultListener () {
            public void requestProcessed (Object result)
            {
                _lastResult = (BuildResult)result;
                delegate.buildRequestSucceeded(requestId, _lastResult);
            }

            // TODO: what the hell to do with reason?
            public void requestFailed (String reason) {
                delegate.buildRequestFailed(requestId, FailureCode.BUILD_REQUEST_FAILED);
            }
        });
    }

    /**
     * Requests that the project is built and exported to the users inventory.
     */
    public void buildAndExportProject (final RequestId requestId,
                                       final ProjectModelDelegate delegate)
    {
        _roomObj.service.buildAndExportProject(_client, new ResultListener () {
            public void requestProcessed (Object result)
            {
                _lastResult = (BuildResult)result;
                delegate.buildAndExportRequestSucceeded(requestId, _lastResult);
            }

            // TODO: what the hell to do with reason?
            public void requestFailed (String reason) {
                delegate.buildAndExportRequestFailed(
                    requestId, FailureCode.BUILD_EXPORT_REQUEST_FAILED);
            }
        });
    }

    /** Called to add an AccessControlListener. */
    public void addAccessControlListener (AccessControlListener listener)
    {
        _accessListeners.add(listener);
    }

    /** Called to remove an AccessControlListener. */
    public void removeAccessControlListener (AccessControlListener listener)
    {
        _accessListeners.remove(listener);
    }

    /** Called to add an OccupantListener. */
    public void addOccupantListener (OccupantListener listener)
    {
        _occupantListeners.add(listener);
    }

    /** Called to remove an OccupantListener. */
    public void removeOccupantListener (OccupantListener listener)
    {
        _occupantListeners.remove(listener);
    }

    // from interface SetListener
    public void entryAdded (EntryAddedEvent event)
    {
        if (event.getName().equals(ProjectRoomObject.COLLABORATORS)) {
            // TODO:
        }
    }

    // from interface SetListener
    public void entryUpdated (EntryUpdatedEvent event)
    {
        if (event.getName().equals(ProjectRoomObject.COLLABORATORS)) {
            // TODO:
        }
    }

    // from interface SetListener
    public void entryRemoved (EntryRemovedEvent event)
    {
        if (event.getName().equals(ProjectRoomObject.COLLABORATORS)) {
            // TODO:
        }
    }

    // from AttributeChangeListener
    public void attributeChanged (AttributeChangedEvent event)
    {
        if (event.getName().equals(ProjectRoomObject.PROJECT)) {
            // TODO:
        }
    }

    // from MessageListener
    public void messageReceived (MessageEvent event)
    {
        if (event.getName().equals(ProjectRoomObject.ACCESS_CONTROL_CHANGE)) {
            notifyAccessControlListeners();
        }
    }

    // from OccupantObserver
    public void occupantEntered (OccupantInfo info)
    {
        for (OccupantListener listener : _occupantListeners) {
            listener.userEntered(info.username.getNormal());
        }
    }

    // from OccupantObserver
    public void occupantLeft (OccupantInfo info)
    {
        for (OccupantListener listener : _occupantListeners) {
            listener.userLeft(info.username.getNormal());
        }
    }

    // from OccupantObserver
    public void occupantUpdated (OccupantInfo oldinfo, OccupantInfo newinfo)
    {
        // nada
    }

    /**
     * Called whenever any data changes that would affect the client's rights in the project
     * being edited.
     */
    private void notifyAccessControlListeners ()
    {
        if (_roomObj.hasWriteAccess(_member)) {
            for (AccessControlListener listener : _accessListeners) {
                listener.writeAccessGranted();
            }

        } else if (_roomObj.hasReadAccess(_member)) {
            for (AccessControlListener listener : _accessListeners) {
                listener.readOnlyAccessGranted();
            }

        } else {
            // the user no longer has access to anything, log them off.
            _client.logoff(false);
        }
    }

    /** The first build will be guessed to be 6 seconds. */
    private static final int DEFAULT_BUILD_TIME = 6000;

    /** A set of components listening for access control change events. */
    private final Set<AccessControlListener> _accessListeners =
        new HashSet<AccessControlListener>();

    /** A set of components listening for occupant events. */
    private final Set<OccupantListener> _occupantListeners = new HashSet<OccupantListener>();

    /** The last BuildResult for the current user. */
    private BuildResult _lastResult;

    private final ProjectRoomObject _roomObj;
    private final Client _client;
    private final MemberName _member;
}
