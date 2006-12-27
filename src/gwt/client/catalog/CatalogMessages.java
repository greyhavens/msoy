package client.catalog;


/**
 * Interface to represent the messages contained in resource  bundle:
 * 	/export/msoy/src/gwt/client/catalog/CatalogMessages.properties'.
 */
public interface CatalogMessages extends com.google.gwt.i18n.client.Messages {
  
  /**
   * Translated "Delist Item".
   * 
   * @return translated "Delist Item"
   * @gwt.key listingDelist
   */
  String listingDelist();

  /**
   * Translated "by {0}".
   * 
   * @return translated "by {0}"
   * @gwt.key itemBy
   */
  String itemBy(String arg0);

  /**
   * Translated "Rating".
   * 
   * @return translated "Rating"
   * @gwt.key sortByRating
   */
  String sortByRating();

  /**
   * Translated "Item delisted.".
   * 
   * @return translated "Item delisted."
   * @gwt.key msgListingDelisted
   */
  String msgListingDelisted();

  /**
   * Translated "Item purchased.".
   * 
   * @return translated "Item purchased."
   * @gwt.key msgListingBought
   */
  String msgListingBought();

  /**
   * Translated "Unable to find catalog listing to delist.".
   * 
   * @return translated "Unable to find catalog listing to delist."
   * @gwt.key errListingNotFound
   */
  String errListingNotFound();

  /**
   * Translated "List Date".
   * 
   * @return translated "List Date"
   * @gwt.key sortByListDate
   */
  String sortByListDate();

  /**
   * Translated "Buy!".
   * 
   * @return translated "Buy!"
   * @gwt.key listingBuy
   */
  String listingBuy();
}
