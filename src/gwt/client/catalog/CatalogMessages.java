package client.catalog;


/**
 * Interface to represent the messages contained in resource  bundle:
 * 	/export/msoy/src/gwt/client/catalog/CatalogMessages.properties'.
 */
public interface CatalogMessages extends com.google.gwt.i18n.client.Messages {
  
  /**
   * Translated "Rating".
   * 
   * @return translated "Rating"
   * @gwt.key sortByRating
   */
  String sortByRating();

  /**
   * Translated "List Date".
   * 
   * @return translated "List Date"
   * @gwt.key sortByListDate
   */
  String sortByListDate();

  /**
   * Translated "No {0} items match the tag: {1}".
   * 
   * @return translated "No {0} items match the tag: {1}"
   * @gwt.key catalogNoTag
   */
  String catalogNoTag(String arg0,  String arg1);

  /**
   * Translated "Catalog".
   * 
   * @return translated "Catalog"
   * @gwt.key catalogTitle
   */
  String catalogTitle();

  /**
   * Translated "Buy!".
   * 
   * @return translated "Buy!"
   * @gwt.key listingBuy
   */
  String listingBuy();

  /**
   * Translated "There are no {0} items listed.".
   * 
   * @return translated "There are no {0} items listed."
   * @gwt.key catalogNoList
   */
  String catalogNoList(String arg0);

  /**
   * Translated "Listed on: {0}".
   * 
   * @return translated "Listed on: {0}"
   * @gwt.key listingListed
   */
  String listingListed(String arg0);

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
   * Translated "No {0} items match the query: {1}".
   * 
   * @return translated "No {0} items match the query: {1}"
   * @gwt.key catalogNoMatch
   */
  String catalogNoMatch(String arg0,  String arg1);

  /**
   * Translated "Unable to find catalog listing to delist.".
   * 
   * @return translated "Unable to find catalog listing to delist."
   * @gwt.key errListingNotFound
   */
  String errListingNotFound();
}
