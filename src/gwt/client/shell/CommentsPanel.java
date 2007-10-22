//
// $Id$

package client.shell;

import java.util.List;

import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.ClickListener;
import com.google.gwt.user.client.ui.FlexTable;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.TextArea;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;

import com.threerings.gwt.ui.PagedGrid;
import com.threerings.gwt.util.DataModel;
import com.threerings.msoy.fora.data.Comment;
import com.threerings.msoy.web.client.CommentService;

import client.util.MsoyUI;
import client.util.RowPanel;

/**
 * Displays comments on a particular entity and allows posting.
 */
public class CommentsPanel extends PagedGrid
{
    public CommentsPanel (int entityType, int entityId)
    {
        super(COMMENTS_PER_PAGE, 1, NAV_ON_BOTTOM);
        addStyleName("dottedGrid");

        _entityType = entityType;
        _entityId = entityId;
    }

    // @Override // from UIObject
    public void setVisible (boolean visible)
    {
        super.setVisible(visible);
        if (visible && _model == null) {
            setModel(new CommentModel(), 0);
        }
    }

    // @Override // from PagedGrid
    protected Widget createWidget (Object item)
    {
        return new CommentPanel(this, (Comment)item);
    }

    // @Override // from PagedGrid
    protected String getEmptyMessage ()
    {
        return CShell.cmsgs.noComments();
    }

    // @Override // from PagedGrid
    protected void addCustomControls (FlexTable controls)
    {
        super.addCustomControls(controls);

        // if we're logged in, display a button for posting a comment
        if (CShell.getMemberId() != 0) {
            _post = new Button(CShell.cmsgs.postComment(), new ClickListener() {
                public void onClick (Widget sender) {
                    _post.setEnabled(false);
                    add(new PostPanel());
                }
            });
            controls.setWidget(0, 0, _post);
        }
    }

    protected void postComment (String text)
    {
        CShell.commentsvc.postComment(
            CShell.ident, _entityType, _entityId, text, new AsyncCallback() {
            public void onSuccess (Object result) {
                postedComment((Comment)result);
            }
            public void onFailure (Throwable cause) {
                MsoyUI.error(CShell.serverError(cause));
            }
        });
    }

    protected void postedComment (Comment comment)
    {
        if (_page == 0) {
            displayPage(0, true);
        } else {
            MsoyUI.info(CShell.cmsgs.commentPosted());
        }
    }

    protected void deleteComment (final CommentPanel panel, final Comment comment)
    {
        CShell.commentsvc.deleteComment(
            CShell.ident, _entityType, _entityId, comment.posted, new AsyncCallback() {
            public void onSuccess (Object result) {
                MsoyUI.info(CShell.cmsgs.commentDeleted());
                _model.removeItem(comment);
            }
            public void onFailure (Throwable cause) {
                MsoyUI.error(CShell.serverError(cause));
            }
        });
    }

    protected class CommentModel implements DataModel
    {
        public int getItemCount () {
            return _commentCount;
        }

        public void doFetchRows (int start, int count, final AsyncCallback callback) {
            CShell.commentsvc.loadComments(
                _entityType, _entityId, start, count, _commentCount == -1, new AsyncCallback() {
                public void onSuccess (Object result) {
                    CommentService.CommentResult cr = (CommentService.CommentResult)result;
                    if (_commentCount == -1) {
                        _commentCount = cr.commentCount;
                    }
                    callback.onSuccess(cr.comments);
                }
                public void onFailure (Throwable caught) {
                    CShell.log("loadCatalog failed", caught);
                    MsoyUI.error(CShell.serverError(caught));
                }
            });
        }

        public void removeItem (Object item) {
            // TOOD
        }

        protected int _commentCount = -1;
    }

    protected class PostPanel extends VerticalPanel
    {
        public PostPanel () {
            add(new Label(CShell.cmsgs.commentText()));
            add(_text = new TextArea());
            _text.setCharacterWidth(40);
            _text.setVisibleLines(3);
            add(_status = new Label(""));
            RowPanel buttons = new RowPanel();
            buttons.add(new Button(CShell.cmsgs.cancel(), new ClickListener() {
                public void onClick (Widget sender) {
                    clearPost();
                }
            }));
            buttons.add(new Button(CShell.cmsgs.submit(), new ClickListener() {
                public void onClick (Widget sender) {
                    postComment(_text.getText());
                    clearPost();
                }
            }));
            add(buttons);
        }

        protected void clearPost () {
            CommentsPanel.this.remove(this);
            _post.setEnabled(true);
        }

        protected TextArea _text;
        protected Label _status;
    }

    protected int _entityType, _entityId;
    protected int _page = -1;

    protected VerticalPanel _comments;
    protected Button _post;

    protected static final int COMMENTS_PER_PAGE = 5;
}
