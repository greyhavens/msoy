//
// $Id$

package com.threerings.msoy.web.data;

import java.util.List;

import com.google.gwt.user.client.rpc.IsSerializable;

/** 
 * Contains the data that we need for the My Whirled views.
 */
public class MyWhirledData
    implements IsSerializable
{
    /** The total number of people online. */
    public int whirledPopulation;

    /** 
     * This member's online friends.
     *
     * @gwt.typeArgs <com.threerings.msoy.web.data.MemberCard>
     */
    public List friends;

    /** 
     * This member's recent feed messages.
     *
     * @gwt.typeArgs <com.threerings.msoy.person.data.FeedMessage>
     */
    public List feed;
}
