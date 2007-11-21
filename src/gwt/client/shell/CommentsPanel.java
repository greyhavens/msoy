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
import com.threerings.msoy.fora.data.Comment;
import com.threerings.msoy.web.client.CommentService;

import client.util.MsoyUI;
import client.util.PromptPopup;
import client.util.RowPanel;
import client.util.ServiceBackedDataModel;

/**
 * Displays comments on a particular entity and allows posting.
 */
public class CommentsPanel extends PagedGrid
{
    public CommentsPanel (int entityType, int entityId)
    {
        super(Comment.COMMENTS_PER_PAGE, 1, NAV_ON_BOTTOM);
        addStyleName("dottedGrid");

        _entityType = entityType;
        _entityId = entityId;

        add(new Label(CShell.cmsgs.loadingComments()));
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
    protected boolean displayNavi (int items)
    {
        return true; // we always need our navigation because it has the "post" button
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
            _commentCount = -1;
            displayPage(0, true);
        } else {
            MsoyUI.info(CShell.cmsgs.commentPosted());
        }
    }

    protected void deleteComment (final Comment comment, boolean confirmed)
    {
        if (!confirmed) {
            new PromptPopup(CShell.cmsgs.deletePostConfirm()) {
                public void onAffirmative () {
                    deleteComment(comment, true);
                }
            }.setContext("\"" + comment.text + "\"").prompt();
            return;
        }

        CShell.commentsvc.deleteComment(
            CShell.ident, _entityType, _entityId, comment.posted, new AsyncCallback() {
            public void onSuccess (Object result) {
                MsoyUI.info(CShell.cmsgs.commentDeleted());
                _commentCount = -1;
                removeItem(comment);
            }
            public void onFailure (Throwable cause) {
                MsoyUI.error(CShell.serverError(cause));
            }
        });
    }

    protected class CommentModel extends ServiceBackedDataModel
    {
        protected void callFetchService (int start, int count, boolean needCount) {
            CShell.commentsvc.loadComments(_entityType, _entityId, start, count, needCount, this); 
        }
        protected int getCount (Object result) {
            return ((CommentService.CommentResult)result).commentCount;
        }
        protected List getRows (Object result) {
            return ((CommentService.CommentResult)result).comments;
        }
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
    protected int _commentCount = -1;

    protected VerticalPanel _comments;
    protected Button _post;
}
