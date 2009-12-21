//
// $Id$

package client.apps;

import java.util.ArrayList;
import java.util.List;

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.Command;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.TextBox;

import com.threerings.gwt.ui.SmartTable;
import com.threerings.msoy.apps.gwt.AppInfo;
import com.threerings.msoy.apps.gwt.AppService;
import com.threerings.msoy.apps.gwt.AppServiceAsync;
import com.threerings.msoy.data.all.MediaDesc;
import com.threerings.msoy.data.all.MediaDescSize;
import com.threerings.msoy.facebook.gwt.FeedThumbnail;

import client.edutil.EditorTable;
import client.edutil.ThumbnailSet;
import client.edutil.EditorUtil.ConfigException;
import client.ui.MsoyUI;
import client.util.ClickCallback;
import client.util.InfoCallback;
import client.util.MediaUtil;

public class FeedThumbnailsPanel extends FlowPanel
{
    public FeedThumbnailsPanel (AppInfo info)
    {
        addStyleName("feedThumbEditor");
        _appId = info.appId;

        // view thumbnails
        add(MsoyUI.createHTML(_msgs.feedThumbsTitle(info.name), "Title"));
        add(_display = new ThumbnailList());

        // add new variant
        add(new AddNewThumbnails());

        _appsvc.loadThumbnails(_appId, new InfoCallback<List<FeedThumbnail>>() {
            @Override public void onSuccess (List<FeedThumbnail> result) {
                _display.setThumbnails(new ThumbnailSet(result));
            }
        });
    }

    protected class ThumbnailList extends SmartTable
    {
        public ThumbnailList ()
        {
            super("thumbnailList", 5, 0);
            setWidget(0, 0, MsoyUI.createNowLoading());
        }

        public void setThumbnails (ThumbnailSet thumbnails)
        {
            _thumbnails = thumbnails;
            refresh();
        }

        public void addRows (ThumbnailSet thumbnails)
        {
            _thumbnails.addAll(thumbnails.toList());
            refresh();
        }

        public boolean contains (String code, String variant)
        {
            return _thumbnails.get(code, variant).size() > 0;
        }

        public List<FeedThumbnail> toList ()
        {
            return _thumbnails.toList();
        }

        protected void refresh ()
        {
            while (getRowCount() > 0) {
                removeRow(getRowCount() - 1);
            }

            if (_thumbnails.isEmpty()) {
                setText(0, 0, _msgs.feedThumbsEmpty());
                return;
            }

            setText(0, CODE, _msgs.feedThumbsCodeHdr(), 1, "Header");
            setText(0, VARIANT, _msgs.feedThumbsVariantHdr(), 1, "Header");
            setText(0, IMAGE, _msgs.feedThumbsImagesHdr(), FeedThumbnail.COUNT, "Header", "Images");
            getRowFormatter().setStyleName(0, "Row");

            for (String code : _thumbnails.getCodes()) {
                for (String variant : _thumbnails.getVariants(code)) {
                    addRow(code, variant, _thumbnails.get(code, variant));
                }
            }
        }

        protected void addRow (
            final String code, final String variant, List<FeedThumbnail> content)
        {
            int row = getRowCount();
            setText(row, CODE, code);
            setText(row, VARIANT, variant);
            for (int ii = 0, ll = Math.min(FeedThumbnail.COUNT, content.size()); ii < ll; ++ii) {
                setWidget(row, IMAGE + ii, MediaUtil.createMediaView(
                    content.get(ii).media, MediaDescSize.FB_FEED_SIZE), 1, "Image");
            }
            setWidget(row, DELETE_BTN, MsoyUI.createCloseButton(new ClickHandler() {
                @Override public void onClick (ClickEvent event) {
                    _thumbnails.remove(code, variant);
                    refresh();
                }
            }));
            getRowFormatter().setStyleName(row, "Row");
            if (row % 2 == 1) {
                getRowFormatter().addStyleName(row, "AltRow");
            }
        }

        protected ThumbnailSet _thumbnails;
        protected static final int CODE = 0, VARIANT = 1, IMAGE = 2,
            DELETE_BTN = IMAGE + FeedThumbnail.COUNT;
    }

    protected class AddNewThumbnails extends EditorTable
    {
        public AddNewThumbnails ()
        {
            addWidget(MsoyUI.createHTML(_msgs.feedThumbsAddTitle(), "Title"), 2);
            final TextBox code = MsoyUI.createTextBox("", 15, 8);
            addRow(_msgs.feedThumbsCodeLabel(), code,
                new Command() {
                @Override public void execute () {
                    _code = code.getText().trim();
                    if (_code.equals("")) {
                        throw new ConfigException(_msgs.feedThumbsCodeRequiredErr());
                    }
                }
            });
            final TextBox variant = MsoyUI.createTextBox("", 6, 4);
            addRow(_msgs.feedThumbsVariantLabel(), variant, new Command() {
                @Override public void execute () {
                    _variant = variant.getText().trim();
                }
            });
            for (int ii = 0; ii < FeedThumbnail.COUNT; ++ii) {
                _images.add(null);
                addMediaSlot(ii);
            }
            addRow("", new Button (_msgs.feedThumbsAddNewBtn(), new ClickHandler() {
                @Override public void onClick (ClickEvent event) {
                    addNew();
                }
            }), null);

            Button save = addSaveRow();
            new ClickCallback<Void>(save) {
                @Override public boolean callService () {
                    _appsvc.updateThumbnails(_appId, _display.toList(), this);
                    return true;
                }
                @Override public boolean gotResult (Void result) {
                    MsoyUI.info(_msgs.feedThumbsSaved());
                    return true;
                }
            };
        }

        protected void addMediaSlot (final int index)
        {
            final MediaBox media = new MediaBox(MediaDescSize.FB_FEED_SIZE, "fdthumb" + index, null) {
                @Override public void mediaUploaded (
                    String name, MediaDesc desc, int w, int h) {
                    if (checkSize(w, h)) {
                        super.mediaUploaded(name, desc, w, h);
                    }
                }
            };
            addRow(_msgs.feedThumbsImageLabel(String.valueOf(index+1)), media, new Command() {
                @Override public void execute () {
                    if (media.getMedia() == null) {
                        throw new ConfigException(
                            _msgs.feedThumbsNullImageErr(String.valueOf(FeedThumbnail.COUNT)));
                    }
                    _images.set(index, media.getMedia());
                }
            });
        }

        protected void addNew ()
        {
            if (!bindChanges()) {
                return;
            }
            if (_display.contains(_code, _variant)) {
                MsoyUI.error(_msgs.feedThumbsDuplicateErr(_code, _variant));
                return;
            }
            byte pos = 0;
            ThumbnailSet thumbs = new ThumbnailSet();
            for (MediaDesc image : _images) {
                thumbs.add(new FeedThumbnail(image, _code, _variant, pos++));
            }
            _display.addRows(thumbs);
        }

        protected String _code, _variant;
        protected List<MediaDesc> _images = new ArrayList<MediaDesc>();
    }

    protected int _appId;
    protected ThumbnailList _display;

    protected static final AppsMessages _msgs = GWT.create(AppsMessages.class);
    protected static final AppServiceAsync _appsvc = GWT.create(AppService.class);
}
