//
// $Id$

package client.editem;

import com.google.gwt.core.client.GWT;
import com.google.gwt.http.client.URL;
import com.google.gwt.user.client.ui.TextBox;

import com.threerings.msoy.data.all.MediaDesc;
import com.threerings.msoy.item.data.all.Video;
import com.threerings.msoy.item.data.all.Item;

import client.ui.MsoyUI;
import client.util.FlashClients;
import client.util.MsoyCallback;

/**
 * A class for creating and editing {@link Video} digital items.
 */
public class VideoEditor extends BulkMediaEditor
{
    @Override // from ItemEditor
    public void setItem (Item item)
    {
        super.setItem(item);
        _video = (Video)item;
        setUploaderMedia(Item.MAIN_MEDIA, _video.videoMedia);

        _originalDesc = _video.videoMedia;
    }

    @Override // from ItemEditor
    public Item createBlankItem ()
    {
        return new Video();
    }

    @Override
    protected void onLoad ()
    {
        super.onLoad();
        configureBridge();
    }

    @Override // from BulkMediaEditor
    protected void addMainUploader ()
    {
        _youtubeIdBox = MsoyUI.createTextBox("", YOUTUBE_ID_LENGTH, YOUTUBE_ID_LENGTH);
        addRow(_emsgs.youtubeLabel(), bind(_youtubeIdBox, new Binder() {
            @Override public void textUpdated (String text) {
                String trimmed = text.trim();
                if (!trimmed.equals(text)) {
                    _youtubeIdBox.setText(trimmed);
                    return;
                }

                _video.videoMedia = text.equals(_originalYoutubeId) ? _originalDesc : null;
                relayYoutubeId((text.length() < YOUTUBE_ID_LENGTH) ? "" : text);
            }
        }));
        addRow(_emsgs.videoLabel(), createMainUploader(TYPE_VIDEO, false, new MediaUpdater() {
            public String updateMedia (String name, MediaDesc desc, int width, int height) {
                // TODO: remove this hack?
                if (!desc.isVideo()) {
                    return _emsgs.errVideoNotVideo();
                }
                _youtubeIdBox.setText("");
                _video.videoMedia = desc;
                return null;
            }
        }), _emsgs.videoTip());
    }

    @Override protected ItemMediaUploader createUploaderWidget (
        String mediaIds, String type, int mode, MediaUpdater updater)
    {
        if (type.equals(TYPE_VIDEO)) {
            return new ItemMediaUploader(mediaIds, type, mode, updater) {
                @Override public void setMedia (MediaDesc desc) {
                    if (desc != null) {
                        super.setMedia(desc);
                    } else {
                        // create a blank video viewer, ready to receive data when
                        // the user enters it
                        setWidget(0, 0, FlashClients.createVideoPlayer(
                            MediaDesc.THUMBNAIL_WIDTH * 2, MediaDesc.THUMBNAIL_HEIGHT * 2, null));
                    }
                }
            };

        } else {
            return super.createUploaderWidget(mediaIds, type, mode, updater);
        }
    }

    @Override
    protected void commitEdit ()
    {
        // see if we need to update the external media.
        if (_video.videoMedia == null) {
            String id = _youtubeIdBox.getText();
            if (id.length() == YOUTUBE_ID_LENGTH) {
                uploadExternalMedia(id);
                return;
            }
            // else, return a "not valid" error from superclass..
        }

        super.commitEdit();
    }

    protected void uploadExternalMedia (String youtubeId)
    {
        _youtubeIdBox.setEnabled(false);
        // once the full length has been reached, upload the file.
        String data = "id=" + URL.encodeComponent(youtubeId);
        _stuffsvc.publishExternalMedia(data, MediaDesc.EXTERNAL_YOUTUBE,
            new MsoyCallback<MediaDesc>() {
            public void onSuccess (MediaDesc desc) {
                _video.videoMedia = desc;
                _youtubeIdBox.setEnabled(true);
                commitEdit();
            }
        });
    }

    /**
     * We've received the youtubeId from the video player.
     */
    protected void setYoutubeId (String id)
    {
        _originalYoutubeId = id;
        _youtubeIdBox.setText(id);
    }

    /**
     * Called by javascript native method to set the youtube id from the player.
     */
    protected static void gotYoutubeId (String id)
    {
        // "fix" the id from javascript
        String fid = "" + id;
        ((VideoEditor) _singleton).setYoutubeId(fid);
    }

    /**
     * Wire up the communication between this editor and the videoPlayer.
     */
    protected static native void configureBridge () /*-{
        $wnd.gotYoutubeId = function (id) {
            @client.editem.VideoEditor::gotYoutubeId(Ljava/lang/String;)(id);
        };
    }-*/;

    /**
     * Send the youtubeId to the video player.
     */
    protected static native void relayYoutubeId (String id) /*-{
        var player = $doc.getElementById("videoPlayer");
        if (player != null) {
            try { player.setYoutubeId(id) } catch (e) {}
        }
    }-*/;

    protected Video _video;

    protected TextBox _youtubeIdBox;

    /** The original youtube id of this item. */
    protected String _originalYoutubeId;

    /** The original MediaDesc, referenced when editing a youtube id. */
    protected MediaDesc _originalDesc;

    protected static final EditemMessages _emsgs = GWT.create(EditemMessages.class);

    protected static final int YOUTUBE_ID_LENGTH = 11;
}
