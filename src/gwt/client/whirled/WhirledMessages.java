package client.whirled;


/**
 * Interface to represent the messages contained in resource  bundle:
 * 	/export/msoy/src/gwt/client/whirled/WhirledMessages.properties'.
 */
public interface WhirledMessages extends com.google.gwt.i18n.client.Messages {
  
  /**
   * Translated "What''s going <br/>on in My Whirled <br/>right now.".
   * 
   * @return translated "What''s going <br/>on in My Whirled <br/>right now."
   * @gwt.key myWhirledDescription
   */
  String myWhirledDescription();

  /**
   * Translated "My Popular Places".
   * 
   * @return translated "My Popular Places"
   * @gwt.key headerPlaces
   */
  String headerPlaces();

  /**
   * Translated "My Whirled".
   * 
   * @return translated "My Whirled"
   * @gwt.key titleMyWhirled
   */
  String titleMyWhirled();

  /**
   * Translated "Whirledwide".
   * 
   * @return translated "Whirledwide"
   * @gwt.key titleWhirledwide
   */
  String titleWhirledwide();

  /**
   * Translated "Nobody Playing Games".
   * 
   * @return translated "Nobody Playing Games"
   * @gwt.key noGames
   */
  String noGames();

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

  /**
   * Translated "My Active Games".
   * 
   * @return translated "My Active Games"
   * @gwt.key headerGames
   */
  String headerGames();

  /**
   * Translated "My Friends Online".
   * 
   * @return translated "My Friends Online"
   * @gwt.key headerPeople
   */
  String headerPeople();

  /**
   * Translated "My Rooms".
   * 
   * @return translated "My Rooms"
   * @gwt.key headerRooms
   */
  String headerRooms();

  /**
   * Translated "Active Chats".
   * 
   * @return translated "Active Chats"
   * @gwt.key headerChats
   */
  String headerChats();

  /**
   * Translated "Playin'' {0}".
   * 
   * @return translated "Playin'' {0}"
   * @gwt.key inGame
   */
  String inGame(String arg0);

  /**
   * Translated "Chillin'' in {0}".
   * 
   * @return translated "Chillin'' in {0}"
   * @gwt.key inRoom
   */
  String inRoom(String arg0);
}
