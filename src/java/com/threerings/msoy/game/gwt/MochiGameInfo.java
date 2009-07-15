//
// #Id$

package com.threerings.msoy.game.gwt;

import com.google.gwt.user.client.rpc.IsSerializable;

public class MochiGameInfo
    implements IsSerializable
{
    /** The name of this game. */
    public String name;

    /** The unique identifier of this mochi game. */
    public String tag;

    /** The categories. */
    public String categories;

    /** Mochi game author. */
    public String author;

    /** Description. */
    public String desc;

    /** The url to the thumbnail. */
    public String thumbURL;

    /** The url to the swf. */
    public String swfURL;

    /** SWF width. */
    public int width;

    /** SWF height. */
    public int height;
}
