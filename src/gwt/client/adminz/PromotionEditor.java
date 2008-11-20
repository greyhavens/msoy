//
// $Id$

package client.adminz;

import java.util.Date;
import java.util.List;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.ClickListener;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.HasAlignment;
import com.google.gwt.user.client.ui.KeyboardListenerAdapter;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.Widget;

import org.gwt.advanced.client.ui.widget.DatePicker;

import com.threerings.gwt.ui.SmartTable;

import com.threerings.msoy.admin.gwt.AdminService;
import com.threerings.msoy.admin.gwt.AdminServiceAsync;
import com.threerings.msoy.data.all.MediaDesc;
import com.threerings.msoy.web.gwt.Promotion;

import client.item.ImageChooserPopup;
import client.ui.LimitedTextArea;
import client.ui.MsoyUI;
import client.ui.PromotionBox;
import client.ui.TongueBox;
import client.util.ClickCallback;
import client.util.MediaUtil;
import client.util.MsoyCallback;
import client.util.ServiceUtil;

/**
 * Displays all promotions registered with the system, allows adding and deletion.
 */
public class PromotionEditor extends FlowPanel
{
    public PromotionEditor ()
    {
        setStyleName("promoEditor");
        add(MsoyUI.createLabel(_msgs.promoLoading(), null));

        _adminsvc.loadPromotions(new MsoyCallback<List<Promotion>>() {
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

        final SmartTable create = new SmartTable("Create", 0, 10);
        int row = 0;
        create.setText(row, 0, _msgs.promoId());
        create.setWidget(row++, 1, _promoId = MsoyUI.createTextBox("", 80, 20), 2, null);
        create.setText(row, 0, _msgs.promoStarts());
        create.setWidget(row++, 1, _starts = new DatePicker(new Date()));
        _starts.setTimeVisible(true);
        _starts.display(); // fucking crack smokers
        create.setText(row, 0, _msgs.promoEnds());
        create.setWidget(row++, 1, _ends = new DatePicker(new Date(199, 0, 1)));
        _ends.setTimeVisible(true);
        _ends.display(); // can't anyone write a sane library?
        create.setText(row, 0, _msgs.promoIcon());
        create.setWidget(row++, 1, new Button(_msgs.promoChange(), new ClickListener() {
            public void onClick (Widget source) {
                ImageChooserPopup.displayImageChooser(true, new MsoyCallback<MediaDesc>() {
                    public void onSuccess (MediaDesc photo) {
                        _promoIcon = photo;
                        create.setWidget(_previewRow, 1, new PromotionBox(createPromotion()));
                    }
                });
            }
        }));
        create.setText(row, 0, _msgs.promoBlurb());
        create.setWidget(row++, 1, _blurb = new LimitedTextArea(255, 60, 5), 2, null);
        _blurb.getTextArea().addKeyboardListener(new KeyboardListenerAdapter() {
            public void onKeyPress (Widget sender, char keyCode, int modifiers) {
                create.setWidget(_previewRow, 1, new PromotionBox(createPromotion()));
            }
        });

        create.setText(row, 0, _msgs.promoPreview());
        _previewRow = row++;

        create.setWidget(row, 0, new Button(_msgs.promoAdd(), new ClickListener() {
            public void onClick (Widget sender) {
                publishPromotion(createPromotion());
            }
        }), 2, null);
        create.getFlexCellFormatter().setHorizontalAlignment(row, 0, HasAlignment.ALIGN_RIGHT);

        add(new TongueBox(_msgs.promoCreate(), create));
    }

    protected void addPromotion (SmartTable ptable, final Promotion promo)
    {
        final int row = ptable.getRowCount();
        int col = 0;
        ptable.setText(row, col++, promo.promoId);
        if (promo.icon != null) {
            ptable.setWidget(row, col++, MediaUtil.createMediaView(
                                 promo.icon, MediaDesc.HALF_THUMBNAIL_SIZE));
        }
        ptable.setWidget(row, col++, MsoyUI.createHTML(promo.blurb, null));
        ptable.setText(row, col++, MsoyUI.formatDateTime(promo.starts));
        ptable.setText(row, col++, MsoyUI.formatDateTime(promo.ends));

        Button delete = new Button("X");
        delete.setTitle("Delete this promotion");
        ptable.setWidget(row, col++, delete);
        new ClickCallback<Void>(delete) {
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

    protected void publishPromotion (final Promotion promo)
    {
        if (promo.promoId.length() == 0 || promo.blurb.length() == 0) {
            return;
        }

        _adminsvc.addPromotion(promo, new MsoyCallback<Void>() {
            public void onSuccess (Void result) {
                _promoId.setText("");
                _blurb.setText("");
                _promoIcon = null;
                addPromotion(_ptable, promo);
            }
        });
    }

    protected SmartTable _ptable = new SmartTable("Promos", 0, 10);

    protected TextBox _promoId;
    protected DatePicker _starts, _ends;
    protected LimitedTextArea _blurb;
    protected MediaDesc _promoIcon;
    protected int _previewRow;

    protected static final AdminMessages _msgs = GWT.create(AdminMessages.class);
    protected static final AdminServiceAsync _adminsvc = (AdminServiceAsync)
        ServiceUtil.bind(GWT.create(AdminService.class), AdminService.ENTRY_POINT);
}
