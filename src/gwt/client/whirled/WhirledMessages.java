package client.whirled;


/**
 * Interface to represent the messages contained in resource  bundle:
 * 	/export/msoy/src/gwt/client/whirled/WhirledMessages.properties'.
 */
public interface WhirledMessages extends com.google.gwt.i18n.client.Messages {
  
  /**
   * Translated "My Online Friends".
   * 
   * @return translated "My Online Friends"
   * @gwt.key headerPeople
   */
  String headerPeople();

  /**
   * Translated "Active Games".
   * 
   * @return translated "Active Games"
   * @gwt.key headerGames
   */
  String headerGames();

  /**
   * Translated "My Friend''s Locations".
   * 
   * @return translated "My Friend''s Locations"
   * @gwt.key headerRooms
   */
  String headerRooms();

  /**
   * Translated "Playin'' {0}".
   * 
   * @return translated "Playin'' {0}"
   * @gwt.key inGame
   */
  String inGame(String arg0);

  /**
   * Translated "My Whirled".
   * 
   * @return translated "My Whirled"
   * @gwt.key titleMyWhirled
   */
  String titleMyWhirled();

  /**
   * Translated "Chillin'' in {0}".
   * 
   * @return translated "Chillin'' in {0}"
   * @gwt.key inRoom
   */
  String inRoom(String arg0);

  /**
   * Translated "Nobody Playing Games".
   * 
   * @return translated "Nobody Playing Games"
   * @gwt.key noGames
   */
  String noGames();

  /**
   * Translated "Whirledwide".
   * 
   * @return translated "Whirledwide"
   * @gwt.key titleWhirledwide
   */
  String titleWhirledwide();

  /**
   * Translated "No Online Friends".
   * 
   * @return translated "No Online Friends"
   * @gwt.key noPeople
   */
  String noPeople();

  /**
   * Translated "No Friends in Rooms".
   * 
   * @return translated "No Friends in Rooms"
   * @gwt.key noRooms
   */
  String noRooms();
}
