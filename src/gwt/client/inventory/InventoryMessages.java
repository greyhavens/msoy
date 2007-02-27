package client.inventory;


/**
 * Interface to represent the messages contained in resource  bundle:
 * 	/export/msoy/src/gwt/client/inventory/InventoryMessages.properties'.
 */
public interface InventoryMessages extends com.google.gwt.i18n.client.Messages {
  
  /**
   * Translated "Remix...".
   * 
   * @return translated "Remix..."
   * @gwt.key detailRemix
   */
  String detailRemix();

  /**
   * Translated "Please configure the name of this item. Click Edit above to do so.".
   * 
   * @return translated "Please configure the name of this item. Click Edit above to do so."
   * @gwt.key errItemMissingName
   */
  String errItemMissingName();

  /**
   * Translated "List in Catalog...".
   * 
   * @return translated "List in Catalog..."
   * @gwt.key detailList
   */
  String detailList();

  /**
   * Translated "Please configure a description for this item. Click Edit above to do so.".
   * 
   * @return translated "Please configure a description for this item. Click Edit above to do so."
   * @gwt.key errItemMissingDescrip
   */
  String errItemMissingDescrip();

  /**
   * Translated "Delete".
   * 
   * @return translated "Delete"
   * @gwt.key detailDelete
   */
  String detailDelete();

  /**
   * Translated "Item deleted.".
   * 
   * @return translated "Item deleted."
   * @gwt.key msgItemDeleted
   */
  String msgItemDeleted();

  /**
   * Translated "Item remixed.".
   * 
   * @return translated "Item remixed."
   * @gwt.key msgItemRemixed
   */
  String msgItemRemixed();

  /**
   * Translated "Create new...".
   * 
   * @return translated "Create new..."
   * @gwt.key panelCreateNew
   */
  String panelCreateNew();

  /**
   * Translated "Inventory".
   * 
   * @return translated "Inventory"
   * @gwt.key inventoryTitle
   */
  String inventoryTitle();

  /**
   * Translated "You have no {0} items.".
   * 
   * @return translated "You have no {0} items."
   * @gwt.key panelNoItems
   */
  String panelNoItems(String arg0);

  /**
   * Translated "Item listed.".
   * 
   * @return translated "Item listed."
   * @gwt.key msgItemListed
   */
  String msgItemListed();

  /**
   * Translated "Edit...".
   * 
   * @return translated "Edit..."
   * @gwt.key detailEdit
   */
  String detailEdit();
}
