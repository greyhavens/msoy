//
// $Id$

package com.threerings.msoy.web.data;

import java.util.ArrayList;
import java.util.List;

import com.google.gwt.user.client.rpc.IsSerializable;

import com.threerings.msoy.item.data.all.MediaDesc;

/**
 * Contains a limited amount of scene information that is needed for displaying scene information
 * in GWT land.
 */
public class SceneCard
    implements IsSerializable
{
    /** constant to indicate that this scene is a room */
    public static final int ROOM = 1;

    /** constant to indicate that this scene is a game */
    public static final int GAME = 2;

    /** The scene's id. */
    public int sceneId;

    /** The scene's name. */
    public String name;

    /**
     * If this scene is owned by a group, this is the group's logo, if by a person, this is
     * their profile pic.
     */
    public MediaDesc logo;

    /** The scene's type. */
    public int sceneType;

    /**
     * A list of the friend ids that are in this scene.
     *
     * @gwt.typeArgs <java.lang.Integer>
     */
    public List friends = new ArrayList();
}
