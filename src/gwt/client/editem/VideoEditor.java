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
    }

    @Override // from ItemEditor
    public Item createBlankItem ()
    {
        return new Video();
    }

    @Override // from BulkMediaEditor
    protected void addMainUploader ()
    {
        _youtubeId = MsoyUI.createTextBox("", YOUTUBE_ID_LENGTH, YOUTUBE_ID_LENGTH);
        addRow(_emsgs.youtubeLabel(), bind(_youtubeId, new Binder() {
            @Override public void textUpdated (String text) {
                String trimmed = text.trim();
                if (!trimmed.equals(text)) {
                    _youtubeId.setText(trimmed);
                    return;
                }
                if (text.length() < YOUTUBE_ID_LENGTH) {
                    return;
                }
                // once the full length has been reached, upload the file.
                String data = "id=" + URL.encodeComponent(text);
                _stuffsvc.publishExternalMedia(data, MediaDesc.EXTERNAL_YOUTUBE,
                    new MsoyCallback<MediaDesc>() {
                    public void onSuccess (MediaDesc desc) {
                        _video.videoMedia = desc;
                        setUploaderMedia(Item.MAIN_MEDIA, desc);
                    }
                });
            }
        }));
//        addRow("- or -"); // TODO
        addRow(_emsgs.videoLabel(), createMainUploader(TYPE_VIDEO, false, new MediaUpdater() {
            public String updateMedia (String name, MediaDesc desc, int width, int height) {
                // TODO: remove this hack?
                if (!desc.isVideo()) {
                    return _emsgs.errVideoNotVideo();
                }
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
                        setWidget(0, 0, FlashClients.createVideoViewer(
                            MediaDesc.THUMBNAIL_WIDTH * 2, MediaDesc.THUMBNAIL_HEIGHT * 2, null));
                    }
                }
            };

        } else {
            return super.createUploaderWidget(mediaIds, type, mode, updater);
        }
    }

    protected Video _video;

    protected TextBox _youtubeId;

    protected static final EditemMessages _emsgs = GWT.create(EditemMessages.class);

    protected static final int YOUTUBE_ID_LENGTH = 11;
}
