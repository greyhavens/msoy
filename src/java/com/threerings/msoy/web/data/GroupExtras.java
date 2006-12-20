//
// $Id$

package com.threerings.msoy.web.data;

import com.google.gwt.user.client.rpc.IsSerializable;
import com.threerings.io.Streamable;

import com.threerings.msoy.item.web.MediaDesc;

/**
 * Contains extra information about a group, such as the background images used on the GroupView
 * page.  This should be used to hold information that is only needed on the GroupView page itself,
 * and not in other places that Groups are fetched.
 */
public class GroupExtras
    implements Streamable, IsSerializable
{
    /** The tiled background image for the info area. */
    public MediaDesc infoBackground;

    /** The tiled background image for the detail area. */
    public MediaDesc detailBackground;
    
    /** The tiled background image for the people area. */
    public MediaDesc peopleBackground;

    /** The group's charter, or null if one has yet to be set. */
    public String charter;
  
    /** The URL of the group's homepage. */
    public String homepageUrl;
}
