//
// $Id$

package com.threerings.msoy.web.data;

import java.util.ArrayList;
import java.util.List;

import com.google.gwt.user.client.rpc.IsSerializable;

import com.threerings.msoy.data.all.MemberName;

import com.threerings.msoy.item.data.all.MediaDesc;

/** 
 * Contains the data that we need for the My Whirled or Whirledwide views.
 */
public class Whirled
    implements IsSerializable
{
    /** 
     * The list of rooms for this view.
     *
     * @gwt.typeArgs <com.threerings.msoy.web.data.SceneCard>
     */
    public List rooms = new ArrayList();

    /** 
     * The list of games for this view.
     * 
     * @gwt.typeArgs <com.threerings.msoy.item.data.all.Game>
     */
    public List games = new ArrayList();

    /** 
     * The list of people for this view.
     *
     * @gwt.typeArgs <com.threerings.msoy.web.data.MemberCard>
     */
    public List people = new ArrayList();
}
