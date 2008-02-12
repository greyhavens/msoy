//
// $Id$

package com.threerings.msoy.web.data;

import java.util.List;
import java.util.Map;

import com.google.gwt.user.client.rpc.IsSerializable;

import com.threerings.msoy.item.data.all.MediaDesc;

/** 
 * Contains the data that we need for the My Whirled views.
 */
public class MyWhirledData
    implements IsSerializable
{
    /** This member's profile pic. */
    public MediaDesc photo;

    /** The scene id of our home scene. */
    public int homeSceneId;

    /** 
     * This member's online friends.
     *
     * @gwt.typeArgs <com.threerings.msoy.web.data.OnlineMemberCard>
     */
    public List friends;

    /** 
     * This member's recent feed messages.
     *
     * @gwt.typeArgs <com.threerings.msoy.person.data.FeedMessage>
     */
    public List feed;

    /**
     * This member's rooms.
     *
     * @gwt.typeArgs <java.lang.Integer,java.lang.String>
     */
    public Map rooms;
}
