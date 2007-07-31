//
// $Id$

package com.threerings.msoy.web.data;

import com.google.gwt.user.client.rpc.IsSerializable;

import com.threerings.msoy.item.data.all.MediaDesc;

/**
 * Contains a limited amount of scene information that is needed for displaying scene information
 * in GWT land.
 */
public class SceneCard
    implements IsSerializable
{
    /** The scene's id. */
    public int sceneId;

    /** The scene's name. */
    public String name;

    /**
     * If this scene is owned by a group, this is the group's logo, if by a person, this is
     * their profile pic.
     */
    public MediaDesc logo;
}
