//
// $Id$

package com.threerings.msoy.item.data.all;

/**
 * An ItemListInfo that also includes the owner's member name.
 *
 * @author mjensen
 */
public class MemberItemListInfo extends ItemListInfo
{
    public String memberName;

    public MemberItemListInfo ()
    {
    }

    public MemberItemListInfo (ItemListInfo template)
    {
        listId = template.listId;
        memberId = template.memberId;
        type = template.type;
        name = template.name;
    }
}
