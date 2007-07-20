//
// $Id$

package client.item;


/**
 * Interface to represent the messages contained in resource  bundle:
 * 	/export/msoy/src/gwt/client/item/ItemMessages.properties'.
 */
public interface ItemMessages extends com.google.gwt.i18n.client.Messages {
  
  /**
   * Translated "Search".
   * 
   * @return translated "Search"
   * @gwt.key searchSearch
   */
  String searchSearch();

  /**
   * Translated "View Profile".
   * 
   * @return translated "View Profile"
   * @gwt.key viewProfile
   */
  String viewProfile();

  /**
   * Translated "(clear)".
   * 
   * @return translated "(clear)"
   * @gwt.key clearCurrentTag
   */
  String clearCurrentTag();

  /**
   * Translated "Filtered by tag: {0}".
   * 
   * @return translated "Filtered by tag: {0}"
   * @gwt.key currentTag
   */
  String currentTag(String arg0);

  /**
   * Translated "Sort by:".
   * 
   * @return translated "Sort by:"
   * @gwt.key searchSortBy
   */
  String searchSortBy();

  /**
   * Translated "Common tags:".
   * 
   * @return translated "Common tags:"
   * @gwt.key cloudCommonTags
   */
  String cloudCommonTags();

  /**
   * Translated "Browse Catalog".
   * 
   * @return translated "Browse Catalog"
   * @gwt.key browseCatalogFor
   */
  String browseCatalogFor();

  /**
   * Translated "by".
   * 
   * @return translated "by"
   * @gwt.key creatorBy
   */
  String creatorBy();

  /**
   * Translated "No tags in use.".
   * 
   * @return translated "No tags in use."
   * @gwt.key msgNoTags
   */
  String msgNoTags();
}
