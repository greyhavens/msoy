//
// $Id: Whirled.java 5539 2007-08-17 22:43:31Z nathan $

package com.threerings.msoy.web.data;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.google.gwt.user.client.rpc.IsSerializable;

import com.threerings.msoy.item.data.all.MediaDesc;

/** 
 * Contains the data that we need for the My Whirled or Whirledwide views.
 */
public class WhirledwideData
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
     * The total server population.
     */
    public int whirledPopulation;
}
