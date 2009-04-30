//
// $Id$

package client.adminz;

import java.util.Date;
import java.util.List;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.Command;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.HasAlignment;
import com.google.gwt.user.client.ui.HasVerticalAlignment;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.PushButton;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.Widget;

import org.gwt.advanced.client.ui.widget.DatePicker;

import com.threerings.gwt.ui.SmartTable;

import com.threerings.msoy.admin.gwt.AdminService;
import com.threerings.msoy.admin.gwt.AdminServiceAsync;
import com.threerings.msoy.data.all.MediaDesc;
import com.threerings.msoy.web.gwt.DateUtil;
import com.threerings.msoy.web.gwt.Promotion;

import client.item.ImageChooserPopup;
import client.ui.LimitedTextArea;
import client.ui.MsoyUI;
import client.ui.PromotionBox;
import client.ui.TongueBox;
import client.util.ClickCallback;
import client.util.MediaUtil;
import client.util.InfoCallback;
import client.util.ServiceUtil;
import client.util.TextBoxUtil;

/**
 * Displays all promotions registered with the system, allows adding and deletion.
 */
public class PromotionEditor extends FlowPanel
{
    public PromotionEditor ()
    {
        setStyleName("promoEditor");
        add(MsoyUI.createLabel(_msgs.promoLoading(), null));

        _adminsvc.loadPromotions(new InfoCallback<List<Promotion>>() {
            public void onSuccess (List<Promotion> promos) {
                init(promos);
            }
        });
    }

    protected void init (List<Promotion> promos)
    {
        clear();

        // set up the header
        int col = 0;
        _ptable.setText(0, col++, _msgs.promoId(), 1, "Header");
        _ptable.setText(0, col++, _msgs.promoIcon(), 1, "Header");
        _ptable.setText(0, col++, _msgs.promoBlurb(), 1, "Header");
        _ptable.setText(0, col++, _msgs.promoStarts(), 1, "Header");
        _ptable.setText(0, col++, _msgs.promoEnds(), 1, "Header");

        // add the promotions
        for (Promotion promo : promos) {
            addPromotion(_ptable, promo);
        }
        add(new TongueBox(_msgs.promoTitle(), _ptable));

        editPromotion(_edit, createBlankPromotion());
        // TODO: update the title of the tongue box depending on mode
        add(new TongueBox(_msgs.promoCreateOrEdit(), _edit));
    }

    protected void editPromotion (final SmartTable create, Promotion promo)
    {
        int row = 0;
        create.setText(row, 0, _msgs.promoId());
        create.setWidget(row++, 1, _promoId = MsoyUI.createTextBox(promo.promoId, 80, 20), 2, null);
        create.setText(row, 0, _msgs.promoStarts());
        create.setWidget(row++, 1, _starts = new DatePicker(promo.starts));
        _starts.setTimeVisible(true);
        _starts.display(); // fucking crack smokers
        create.setText(row, 0, _msgs.promoEnds());
        create.setWidget(row++, 1, _ends = new DatePicker(promo.ends));
        _ends.setTimeVisible(true);
        _ends.display(); // can't anyone write a sane library?
        create.setText(row, 0, _msgs.promoIcon());
        create.setWidget(row++, 1, new Button(_msgs.promoChange(), new ClickHandler() {
            public void onClick (ClickEvent event) {
                ImageChooserPopup.displayImageChooser(true, new InfoCallback<MediaDesc>() {
                    public void onSuccess (MediaDesc photo) {
                        if (photo != null) {
                            _promoIcon = photo;
                            create.setWidget(_previewRow, 1, new PromotionBox(createPromotion()));
                        }
                    }
                });
            }
        }));
        create.setText(row, 0, _msgs.promoBlurb());
        create.setWidget(row++, 1, _blurb = new LimitedTextArea(255, 60, 5), 2, null);
        _blurb.setText(promo.blurb);
        TextBoxUtil.addTypingListener(_blurb.getTextArea(), new Command() {
            public void execute () {
                create.setWidget(_previewRow, 1, new PromotionBox(createPromotion()));
            }
        });

        _promoIcon = promo.icon;
        create.setText(row, 0, _msgs.promoPreview());
        create.setWidget(_previewRow = row++, 1, new PromotionBox(createPromotion()));

        create.setWidget(row, 0, new Button(_msgs.promoAdd(), new ClickHandler() {
            public void onClick (ClickEvent event) {
                publishPromotion(createPromotion());
            }
        }), 2, null);
        create.getFlexCellFormatter().setHorizontalAlignment(row, 0, HasAlignment.ALIGN_RIGHT);
    }

    protected void addPromotion (SmartTable ptable, final Promotion promo)
    {
        setPromotion(ptable, ptable.getRowCount(), promo);
    }

    protected void setPromotion (SmartTable ptable, final int row, final Promotion promo)
    {
        int col = 0;
        ptable.setText(row, col++, promo.promoId);
        if (promo.icon != null) {
            ptable.setWidget(row, col++, MediaUtil.createMediaView(
                                 promo.icon, MediaDesc.HALF_THUMBNAIL_SIZE));
        }
        ptable.setWidget(row, col++, MsoyUI.createHTML(promo.blurb, null));
        ptable.setText(row, col++, MsoyUI.formatDateTime(promo.starts));
        ptable.setText(row, col++, MsoyUI.formatDateTime(promo.ends));

        HorizontalPanel buttons = new HorizontalPanel();
        buttons.setSpacing(5);
        buttons.setVerticalAlignment(HasVerticalAlignment.ALIGN_MIDDLE);
        Button edit = new Button(_msgs.promoEdit(), new ClickHandler () {
            public void onClick (ClickEvent event) {
                editPromotion(_edit, promo);
            }
        });
        buttons.add(edit);
        PushButton delete = MsoyUI.createCloseButton(null);
        delete.setTitle(_msgs.promoDeleteTip());
        buttons.add(delete);
        ptable.setWidget(row, col++, buttons);
        new ClickCallback<Void>(delete, _msgs.promoDeleteConfirm()) {
            protected boolean callService () {
                _adminsvc.deletePromotion(promo.promoId, this);
                return true;
            }
            protected boolean gotResult (Void result) {
                _ptable.removeRow(row);
                return false;
            }
        };
    }

    protected Promotion createPromotion ()
    {
        Promotion promo = new Promotion();
        promo.promoId = _promoId.getText().trim();
        promo.blurb = _blurb.getText().trim();
        promo.icon = _promoIcon;
        promo.starts = _starts.getDate();
        promo.ends = _ends.getDate();
        return promo;
    }

    protected Promotion createBlankPromotion ()
    {
        Promotion promo = new Promotion();
        promo.promoId = "";
        promo.blurb = "";
        promo.icon = null;
        promo.starts = new Date();
        promo.ends = DateUtil.toDate(THE_FUTURE);
        return promo;
    }

    protected void publishPromotion (final Promotion promo)
    {
        if (promo.promoId.length() == 0 || promo.blurb.length() == 0) {
            return;
        }

        int row = -1;
        for (int ii = 0; ii < _ptable.getRowCount(); ++ii) {
            if (_ptable.getText(ii, 0).equals(promo.promoId)) {
                row = ii;
                break;
            }
        }

        if (row == -1) {
            _adminsvc.addPromotion(promo, new InfoCallback<Void>() {
                public void onSuccess (Void result) {
                    _promoId.setText("");
                    _blurb.setText("");
                    _promoIcon = null;
                    addPromotion(_ptable, promo);
                }
            });

        } else {
            final int frow = row;
            _adminsvc.updatePromotion(promo, new InfoCallback<Void>() {
                public void onSuccess (Void result) {
                    _promoId.setText("");
                    _blurb.setText("");
                    _promoIcon = null;
                    setPromotion(_ptable, frow, promo);
                }
            });
        }
    }

    protected SmartTable _ptable = new SmartTable("Promos", 0, 10);
    protected SmartTable _edit = new SmartTable("Create", 0, 10);

    protected TextBox _promoId;
    protected DatePicker _starts, _ends;
    protected LimitedTextArea _blurb;
    protected MediaDesc _promoIcon;
    protected int _previewRow;

    protected static final AdminMessages _msgs = GWT.create(AdminMessages.class);
    protected static final AdminServiceAsync _adminsvc = (AdminServiceAsync)
        ServiceUtil.bind(GWT.create(AdminService.class), AdminService.ENTRY_POINT);

    protected static final int[] THE_FUTURE = { 2099, 0, 1 };
}
