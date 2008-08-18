package client.stuff;

import java.util.ArrayList;
import java.util.List;

import com.google.gwt.user.client.History;
import com.google.gwt.user.client.ui.SimplePanel;
import com.google.gwt.user.client.ui.Widget;
import com.threerings.gwt.util.DataModel;
import com.threerings.msoy.item.data.all.Item;
import com.threerings.msoy.item.data.all.ItemIdent;
import com.threerings.msoy.item.data.all.SubItem;
import com.threerings.msoy.item.gwt.ItemDetail;
import com.threerings.msoy.stuff.gwt.StuffService;

import client.editem.EditorHost;
import client.editem.ItemEditor;
import client.remix.ItemRemixer;
import client.shell.Args;
import client.shell.Pages;
import client.ui.MsoyUI;
import client.util.Link;
import client.util.MsoyCallback;

/**
 * @author mjensen
 */
public class ItemGridPanel extends SimplePanel
{
    public static final String DISPLAY = "d";

    public static final String EDIT = "e";

    public static final String CREATE = "c";

    public static final String REMIX = "r";

    /**
     * An empty array used in the case that the are no prefix args required.
     */
    public static final String[] NO_PREFIX_ARGS = new String[0];

    public ItemGridPanel (Pages parentPage, ItemGrid itemGrid, ItemDataModel model)
    {
        _parentPage = parentPage;
        _itemGrid = itemGrid;
        _model = model;

        addStyleName("itemGridPanel");
    }

    public void setPrefixArgs (String[] prefixArgs)
    {
        if (prefixArgs == null) {
            prefixArgs = NO_PREFIX_ARGS;
        }
        _prefixArgs = prefixArgs;
        _itemGrid.setPrefixArgs(prefixArgs);
    }

    public void onHistoryChanged (Args args)
    {
        if (CStuff.isGuest()) {
            // if we have no creds, just display a message saying login
            setContent(MsoyUI.createLabel(CStuff.msgs.logon(), "infoLabel"));
            return;
        }

        String command = getArg(args, 0, "");

        if (DISPLAY.equals(command)) {
            // if we're displaying an item's detail, do that
            byte type = (byte) getArg(args, 1, _model.getDefaultItemType());
            int itemId = getArg(args, 2, 0);
            ItemIdent ident = new ItemIdent(type, itemId);
            displayItemDetails(ident);
        } else if (EDIT.equals(command)) {
            // if we're editing an item, display that interface
            byte type = (byte) getArg(args, 1, _model.getDefaultItemType());
            int itemId = getArg(args, 2, 0);
            editItem(type, itemId);
        } else if (CREATE.equals(command)) {
            byte parentType = (byte) getArg(args, 2, 0);
            int parentId = getArg(args, 3, 0);
            createItem(parentType, parentId);
        } else if (REMIX.equals(command)) {
            byte type = (byte) getArg(args, 1, _model.getDefaultItemType());
            int itemId = getArg(args, 2, 0);
            if (args.getArgCount() > getArgIndex(3)) {
                remixItem(type, itemId, getArg(args, 3, 0), getArg(args, 4, 0), getArg(args, 5, 0));
            } else {
                remixItem(type, itemId);
            }
        } else {
            // otherwise perform the default action
            displayGrid(args);
        }
    }

    protected int getArg (Args args, int index, int defaultValue)
    {
        return args.get(getArgIndex(index), defaultValue);
    }

    protected String getArg (Args args, int index, String defaultValue)
    {
        return args.get(getArgIndex(index), defaultValue);
    }

    /**
     * Gets an argument index relative to the _prefixArgs.
     */
    protected int getArgIndex (int index)
    {
        return index + _prefixArgs.length;
    }

    /**
     * Prepends the prefix args to the given list of args and returns the composed argument string.
     */
    protected String composeArgs (String... args)
    {
        List<String> allArgs = new ArrayList<String>(_prefixArgs.length + args.length);

        for (String arg : _prefixArgs) {
            allArgs.add(arg);
        }

        for (String arg : args) {
            allArgs.add(arg);
        }

        return Args.compose(allArgs);
    }

    protected void displayGrid (Args args)
    {
        byte itemType = (byte) getArg(args, 0, _model.getDefaultItemType());
        DataModel<Item> gridModel = _model.getGridModel(itemType);
        if (gridModel != null) {
            int page = getArg(args, 1, 0);
            _itemGrid.setModel(gridModel, page);
            _itemGrid.displayPage(page, true);
        }
        setContent(_itemGrid);
    }

    protected void setContent (Widget content)
    {
        setWidget(content);
    }

    protected void displayItemDetails (final ItemIdent ident)
    {
        _model.loadItemDetail(ident,
            new MsoyCallback<StuffService.DetailOrIdent>() {
                public void onSuccess (StuffService.DetailOrIdent result) {
                    if (result.detail != null) {
                        _model.itemUpdated(result.detail.item); // is this call necessary?
                        // setContent(getTitle(ident.type), createItemDetailPanel(result.detail));
                        setContent(createItemDetailPanel(result.detail));
                    } else {
                        // We didn't have access to that specific item, but have been given
                        // the catalog id for the prototype.
                        ItemIdent id = result.ident;
                        Link.go(Pages.SHOP, Args.compose("l", "" + id.type, "" + id.itemId));
                    }
                }
            });
    }

    protected void editItem (byte itemType, int itemId)
    {
        ItemEditor editor = ItemEditor.createItemEditor(itemType, createEditorHost());
        Item item = _model.findItem(itemType, itemId);
        if (item == null) {
            editor.setItem(itemId);
        } else {
            editor.setItem(item);
        }
        setContent(editor);
    }

    protected void createItem (byte parentType, int parentItemId)
    {
        ItemEditor editor = ItemEditor.createItemEditor(parentType, createEditorHost());
        editor.setItem(editor.createBlankItem());

        if (parentType != 0) {
            editor.setParentItem(new ItemIdent(parentType, parentItemId));
        }

        setContent(editor);
    }

    protected void remixItem (byte itemType, int itemId, int catalogId, int flowCost, int goldCost)
    {
        ItemRemixer remixer = remixItem(itemType, itemId);
        remixer.setCatalogInfo(catalogId, flowCost, goldCost);
    }

    protected ItemRemixer remixItem (byte itemType, int itemId)
    {
        ItemRemixer remixer = new ItemRemixer(createEditorHost());
        Item item = _model.findItem(itemType, itemId);
        if (item != null) {
            remixer.setItem(item);
        } else {
            remixer.setItem(itemType, itemId);
        }
        setContent(remixer);

        return remixer;
    }

    protected ItemDetailPanel createItemDetailPanel (ItemDetail itemDetail)
    {
        final ItemDetailPanel panel = new ItemDetailPanel(_model, itemDetail);

        // load any subitems
        if (itemDetail.item.getSubTypes().length > 0) {
            setSubTypeModels(itemDetail, panel);
        }

        return panel;
    }

    protected EditorHost createEditorHost ()
    {
        return new EditorHost() {
            public void editComplete (Item item) {
                if (item != null) {
                    _model.itemUpdated(item);
                    Link.go(_parentPage,
                        composeArgs(DISPLAY, "" + item.getType(), "" + item.itemId));
                } else {
                    History.back();
                }
            }
        };
    }

    protected void setSubTypeModels (ItemDetail itemDetail, final ItemDetailPanel panel)
    {
        boolean isSourceItem = itemDetail.item.sourceId == 0;

        for (final SubItem subType : itemDetail.item.getSubTypes()) {
            // if this is not an original item, only show salable subtypes
            if (isSourceItem || subType.isSalable()) {
                _model.loadModel(subType.getType(), itemDetail.item.getSuiteId(),
                    new MsoyCallback<DataModel<Item>>() {
                        public void onSuccess (DataModel<Item> subTypeModel) {
                            panel.addSubTypeModel(subType, subTypeModel);
                        }
                });
            }
        }
    }

    /**
     * The parent page on which this panel lives.
     */
    protected Pages _parentPage;

    /**
     * These are arguments passed to and expected by the parent page. This panel needs to take these
     * args into account when reading or passing page args.
     */
    protected String[] _prefixArgs = NO_PREFIX_ARGS;

    protected ItemGrid _itemGrid;

    protected ItemDataModel _model;
}
