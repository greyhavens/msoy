//
// $Id$

package client.people;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.ui.ClickListener;
import com.google.gwt.user.client.ui.Widget;

import com.threerings.msoy.comment.gwt.Comment;
import com.threerings.msoy.data.all.MediaDesc;
import com.threerings.msoy.profile.gwt.ProfileService;

import client.comment.CommentsPanel;
import client.shell.ShellMessages;

/**
 * Displays a comment wall on a member's profile.
 */
public class CommentsBlurb extends Blurb
{
    @Override // from Blurb
    public boolean shouldDisplay (ProfileService.ProfileResult pdata)
    {
        return true;
    }

    @Override // from Blurb
    public void init (ProfileService.ProfileResult pdata)
    {
        super.init(pdata);

        setHeader(CPeople.msgs.commentsTitle());
        setContent(_wall = new WallPanel(pdata.name.getMemberId()));
        restorePostFooter();
    }

    protected void restorePostFooter ()
    {
        setFooterLabel(_cmsgs.postComment(), new ClickListener() {
            public void onClick (Widget sender) {
                _wall.startPost();
                setFooter(null);
            }
        });
    }

    protected class WallPanel extends CommentsPanel
    {
        public WallPanel (int memberId) {
            super(Comment.TYPE_PROFILE_WALL, memberId, Integer.MAX_VALUE);
            addStyleName("Wall");
            removeStyleName("dottedGrid");
            setVisible(true); // trigger immediate loading of our model
        }

        public void startPost () {
            showPostPanel();
        }

        @Override // from PagedGrid
        protected boolean displayNavi (int items) {
            return false; // don't need it
        }

        @Override // from CommentsPanel
        protected void clearPostPanel (PostPanel panel) {
            super.clearPostPanel(panel);
            restorePostFooter();
        }

        @Override // from CommentsPanel
        protected int getThumbnailSize() {
            return MediaDesc.HALF_THUMBNAIL_SIZE;
        }
    }

    protected WallPanel _wall;

    protected static final ShellMessages _cmsgs = GWT.create(ShellMessages.class);
}
