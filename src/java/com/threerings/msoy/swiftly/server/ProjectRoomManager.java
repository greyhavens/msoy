//
// $Id$

package com.threerings.msoy.swiftly.server;

import com.threerings.presents.data.ClientObject;

import com.threerings.crowd.data.PlaceObject;
import com.threerings.crowd.server.PlaceManager;

import com.threerings.msoy.server.MsoyServer;

import com.threerings.msoy.swiftly.data.DocumentElement;
import com.threerings.msoy.swiftly.data.PathElement;
import com.threerings.msoy.swiftly.data.ProjectRoomMarshaller;
import com.threerings.msoy.swiftly.data.ProjectRoomObject;

/**
 * Manages a Swiftly project room.
 */
public class ProjectRoomManager extends PlaceManager
    implements ProjectRoomProvider
{
    @Override // from PlaceManager
    protected PlaceObject createPlaceObject ()
    {
        return new ProjectRoomObject();
    }

    @Override // from PlaceManager
    protected void didStartup ()
    {
        super.didStartup();

        // get a casted reference to our room object
        _roomObj = (ProjectRoomObject)_plobj;

        // TODO: load up the project information and populate the room object
        PathElement node;
        _roomObj.addPathElement(node = PathElement.createRoot("Fake Project"));
        _roomObj.addPathElement(node = PathElement.createDirectory("Directory 1", node.elementId));
        _roomObj.addPathElement(new PathElement(PathElement.Type.FILE, "File 1", node.elementId));
        _roomObj.addPathElement(new PathElement(PathElement.Type.FILE, "File 2", node.elementId));
        _roomObj.addPathElement(new PathElement(PathElement.Type.FILE, "File 3", node.elementId));
        _roomObj.addPathElement(node = PathElement.createDirectory("Directory 2", node.elementId));
        _roomObj.addPathElement(new PathElement(PathElement.Type.FILE, "File 4", node.elementId));
        _roomObj.addPathElement(new PathElement(PathElement.Type.FILE, "File 5", node.elementId));
        _roomObj.addPathElement(new DocumentElement("File 6", node.elementId, "Bob!"));

        // wire up our invocation service
        _roomObj.setService(
            (ProjectRoomMarshaller)MsoyServer.invmgr.registerDispatcher(
                new ProjectRoomDispatcher(this), false));
    }

    @Override // from PlaceManager
    protected void didShutdown ()
    {
        super.didShutdown();
        MsoyServer.swiftlyMan.projectDidShutdown(this);
    }

    // from interface ProjectRoomProvider
    public void addPathElement (ClientObject caller, PathElement element)
    {
        // TODO: check access!

        // for now just update the room object
        _roomObj.addPathElement(element);
    }

    // from interface ProjectRoomProvider
    public void updatePathElement (ClientObject caller, PathElement element)
    {
        // TODO: check access!
        _roomObj.updateElements(element);
    }

    // from interface ProjectRoomProvider
    public void deletePathElement (ClientObject caller, int elementId)
    {
        // TODO: check access!
        _roomObj.removeFromElements(elementId);
    }

    protected ProjectRoomObject _roomObj;
}
