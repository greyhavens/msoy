//
// $Id$

package com.threerings.msoy.web.data;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.google.gwt.user.client.rpc.IsSerializable;

import com.threerings.msoy.item.data.all.MediaDesc;

/** 
 * Contains the data that we need for the My Whirled or Whirledwide views.
 */
public class Whirled
    implements IsSerializable
{
    /** 
     * The list of places for this view.
     *
     * @gwt.typeArgs <com.threerings.msoy.web.data.SceneCard>
     */
    public List places = new ArrayList();

    /** 
     * The list of games for this view.
     * 
     * @gwt.typeArgs <com.threerings.msoy.web.data.SceneCard>
     */
    public List games = new ArrayList();

    /** 
     * The list of people for this view.
     *
     * @gwt.typeArgs <com.threerings.msoy.web.data.MemberCard>
     */
    public List people = new ArrayList();

    /** 
     * This person's profile pic.
     */
    public MediaDesc photo;

    /**
     * The list of rooms owned by this person.
     *
     * @gwt.typeArgs <java.lang.Integer,java.lang.String>
     */
    public Map ownedRooms;
}
