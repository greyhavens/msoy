//
// $Id$

package com.threerings.msoy.web.data;

import com.google.gwt.user.client.rpc.IsSerializable;

/**
 * Contains information on a Swiftly project type,
 */
public class SwiftlyProjectType
    implements IsSerializable
{
    /** The id of the project type. */
    public int typeId;

    /** The project type name. */
    public String typeName;
}
