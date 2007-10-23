package client.catalog;


/**
 * Interface to represent the messages contained in resource  bundle:
 * 	/export/msoy/src/gwt/client/catalog/CatalogMessages.properties'.
 */
public interface CatalogMessages extends com.google.gwt.i18n.client.Messages {
  
  /**
   * Translated "Listed on: {0}".
   * 
   * @return translated "Listed on: {0}"
   * @gwt.key listingListed
   */
  String listingListed(String arg0);

  /**
   * Translated "There are no {0} items listed.".
   * 
   * @return translated "There are no {0} items listed."
   * @gwt.key catalogNoList
   */
  String catalogNoList(String arg0);

  /**
   * Translated "List Date".
   * 
   * @return translated "List Date"
   * @gwt.key sortByListDate
   */
  String sortByListDate();

  /**
   * Translated "Rating".
   * 
   * @return translated "Rating"
   * @gwt.key sortByRating
   */
  String sortByRating();

  /**
   * Translated "Delist Item".
   * 
   * @return translated "Delist Item"
   * @gwt.key listingDelist
   */
  String listingDelist();

  /**
   * Translated "Purchases: {0}".
   * 
   * @return translated "Purchases: {0}"
   * @gwt.key listingPurchases
   */
  String listingPurchases(String arg0);

  /**
   * Translated "(clear)".
   * 
   * @return translated "(clear)"
   * @gwt.key catalogClearFilter
   */
  String catalogClearFilter();

  /**
   * Translated "Buy!".
   * 
   * @return translated "Buy!"
   * @gwt.key listingBuy
   */
  String listingBuy();

  /**
   * Translated "Lowest Price".
   * 
   * @return translated "Lowest Price"
   * @gwt.key sortByPriceAsc
   */
  String sortByPriceAsc();

  /**
   * Translated "Filtered by tag: {0}".
   * 
   * @return translated "Filtered by tag: {0}"
   * @gwt.key catalogTagFilter
   */
  String catalogTagFilter(String arg0);

  /**
   * Translated "Item purchased.".
   * 
   * @return translated "Item purchased."
   * @gwt.key msgListingBought
   */
  String msgListingBought();

  /**
   * Translated "Catalog".
   * 
   * @return translated "Catalog"
   * @gwt.key catalogTitle
   */
  String catalogTitle();

  /**
   * Translated "Filtered by creator".
   * 
   * @return translated "Filtered by creator"
   * @gwt.key catalogCreatorFilter
   */
  String catalogCreatorFilter();

  /**
   * Translated "Purchases".
   * 
   * @return translated "Purchases"
   * @gwt.key sortByPurchases
   */
  String sortByPurchases();

  /**
   * Translated "No {0} items match the query: {1}".
   * 
   * @return translated "No {0} items match the query: {1}"
   * @gwt.key catalogNoMatch
   */
  String catalogNoMatch(String arg0,  String arg1);

  /**
   * Translated "Item delisted.".
   * 
   * @return translated "Item delisted."
   * @gwt.key msgListingDelisted
   */
  String msgListingDelisted();

  /**
   * Translated "No filter.".
   * 
   * @return translated "No filter."
   * @gwt.key catalogNoFilter
   */
  String catalogNoFilter();

  /**
   * Translated "Filtered by search: {0}".
   * 
   * @return translated "Filtered by search: {0}"
   * @gwt.key catalogSearchFilter
   */
  String catalogSearchFilter(String arg0);

  /**
   * Translated "by {0}".
   * 
   * @return translated "by {0}"
   * @gwt.key itemBy
   */
  String itemBy(String arg0);

  /**
   * Translated "No {0} items match the tag: {1}".
   * 
   * @return translated "No {0} items match the tag: {1}"
   * @gwt.key catalogNoTag
   */
  String catalogNoTag(String arg0,  String arg1);

  /**
   * Translated "Unable to find catalog listing to delist.".
   * 
   * @return translated "Unable to find catalog listing to delist."
   * @gwt.key errListingNotFound
   */
  String errListingNotFound();

  /**
   * Translated "You can remove this catalog listing.".
   * 
   * @return translated "You can remove this catalog listing."
   * @gwt.key listingDelistTip
   */
  String listingDelistTip();

  /**
   * Translated "Highest Price".
   * 
   * @return translated "Highest Price"
   * @gwt.key sortByPriceDesc
   */
  String sortByPriceDesc();
}
