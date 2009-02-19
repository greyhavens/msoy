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

        setHeader(_msgs.commentsTitle());
        setContent(_wall = new WallPanel(pdata.name.getMemberId()));
        setFooterLabel(_cmsgs.postComment(), new ClickListener() {
            public void onClick (Widget sender) {
                _wall.showPostPopup();
            }
        });
    }

    protected class WallPanel extends CommentsPanel
    {
        public WallPanel (int memberId) {
            super(Comment.TYPE_PROFILE_WALL, memberId, COMMENTS_PER_PAGE, false);
            addStyleName("Wall");
            removeStyleName("dottedGrid");
            setVisible(true); // trigger immediate loading of our model
        }

        @Override // from PagedGrid
        protected boolean displayNavi (int items) {
            return (items > COMMENTS_PER_PAGE);
        }

        @Override // from CommentsPanel
        protected int getThumbnailSize() {
            return MediaDesc.HALF_THUMBNAIL_SIZE;
        }
    }

    protected WallPanel _wall;

    protected static final PeopleMessages _msgs = GWT.create(PeopleMessages.class);
    protected static final ShellMessages _cmsgs = GWT.create(ShellMessages.class);

    protected static final int COMMENTS_PER_PAGE = 20;
}
