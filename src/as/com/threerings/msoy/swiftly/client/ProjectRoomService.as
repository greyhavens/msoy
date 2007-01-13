//
// $Id$

package com.threerings.msoy.swiftly.client {

import flash.utils.ByteArray;
import com.threerings.msoy.swiftly.client.ProjectRoomService;
import com.threerings.msoy.swiftly.data.PathElement;
import com.threerings.presents.client.Client;
import com.threerings.presents.client.InvocationService;
import com.threerings.presents.client.InvocationService_InvocationListener;

/**
 * An ActionScript version of the Java ProjectRoomService interface.
 */
public interface ProjectRoomService extends InvocationService
{
    // from Java interface ProjectRoomService
    function addPathElement (arg1 :Client, arg2 :PathElement) :void;

    // from Java interface ProjectRoomService
    function deletePathElement (arg1 :Client, arg2 :int) :void;

    // from Java interface ProjectRoomService
    function updatePathElement (arg1 :Client, arg2 :PathElement) :void;
}
}
