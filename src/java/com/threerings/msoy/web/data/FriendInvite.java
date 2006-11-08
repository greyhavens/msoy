//
// $Id$

package com.threerings.msoy.web.data;

import java.util.HashMap;
import java.util.Map;

import com.google.gwt.user.client.ui.Widget;
import com.threerings.msoy.web.client.WebContext;

public class FriendInvite
    implements MailBodyObjectComposer
{
    // @Override
    public Widget widgetForComposition (WebContext ctx)
    {
        // TODO Auto-generated method stub
        return null;
    }
    // @Override
    public MailBodyObject getComposedWidget ()
    {
        return new FriendBodyObject(new HashMap());
    }

    public static final class FriendBodyObject extends MailBodyObject
    {
        public FriendBodyObject ()
        {
            // TODO
        }
        
        protected FriendBodyObject (Map state)
        {
            super();
        }

        // @Override
        public int getType ()
        {
            return TYPE_FRIEND_INVITE;
        }

        // @Override
        public Map exportState ()
        {
            // TODO Auto-generated method stub
            return null;
        }

        // @Override
        public Widget widgetForRecipient (WebContext ctx)
        {
            // TODO Auto-generated method stub
            return null;
        }

        // @Override
        public Widget widgetForOthers (WebContext ctx)
        {
            // TODO Auto-generated method stub
            return null;
        }

        // @Override
        public boolean equals (Object obj)
        {
            if (this == obj) {
                return true;
            }
            if (obj == null || !(obj instanceof FriendInvite)) {
                return false;
            }
            return true;
        }

        // @Override
        public int hashCode ()
        {
            return 0;
        }
    }
}
