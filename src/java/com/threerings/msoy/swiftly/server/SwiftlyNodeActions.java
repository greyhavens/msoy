//
// $Id$

package com.threerings.msoy.swiftly.server;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import com.threerings.msoy.data.all.MemberName;

import com.threerings.msoy.peer.server.MsoyPeerManager;

import com.threerings.msoy.swiftly.data.all.SwiftlyProject;

/**
 * Contains various Swiftly node actions.
 */
@Singleton
public class SwiftlyNodeActions
{
    /**
     * Reports to the room hosting this project that the supplied project object has been updated.
     */
    public void projectUpdated (SwiftlyProject project)
    {
        _peerMan.invokeNodeAction(new ProjectUpdatedAction(project));
    }

    /**
     * Reports to the room hosting this project that the supplied member is now a collaborator.
     */
    public void collaboratorAdded (int projectId, MemberName name)
    {
        _peerMan.invokeNodeAction(new CollaboratorAddedAction(projectId, name));
    }

    /**
     * Reports to the room hosting this project that the supplied member is not a collaborator.
     */
    public void collaboratorRemoved (int projectId, MemberName name)
    {
        _peerMan.invokeNodeAction(new CollaboratorRemovedAction(projectId, name));
    }

    protected static class ProjectUpdatedAction extends SwiftlyNodeAction
    {
        public ProjectUpdatedAction (SwiftlyProject project) {
            super(project.projectId);
            _project = project;
        }

        public ProjectUpdatedAction () {
        }

        @Override protected void execute (SwiftlyManager swiftlyMan) {
            swiftlyMan.projectUpdated(_project);
        }

        protected SwiftlyProject _project;
    }

    protected static class CollaboratorAddedAction extends SwiftlyNodeAction
    {
        public CollaboratorAddedAction (int projectId, MemberName name) {
            super(projectId);
            _name = name;
        }

        public CollaboratorAddedAction () {
        }

        @Override protected void execute (SwiftlyManager swiftlyMan) {
            swiftlyMan.collaboratorAdded(_projectId, _name);
        }

        protected MemberName _name;
    }

    protected static class CollaboratorRemovedAction extends SwiftlyNodeAction
    {
        public CollaboratorRemovedAction (int projectId, MemberName name) {
            super(projectId);
            _name = name;
        }

        public CollaboratorRemovedAction () {
        }

        @Override protected void execute (SwiftlyManager swiftlyMan) {
            swiftlyMan.collaboratorRemoved(_projectId, _name);
        }

        protected MemberName _name;
    }

    @Inject protected MsoyPeerManager _peerMan;
}
