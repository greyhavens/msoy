//
// $Id$

package com.threerings.msoy.person.gwt;

import java.util.List;

import com.google.gwt.user.client.rpc.IsSerializable;

import com.threerings.msoy.data.all.MemberName;

/**
 * List of all the galleries a person owns, plus the owner information.
 */
public class GalleryListData implements IsSerializable
{
    public List<Gallery> galleries;
    public MemberName owner;
}

