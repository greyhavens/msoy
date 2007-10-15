package client.whirled;


/**
 * Interface to represent the messages contained in resource  bundle:
 * 	/export/msoy/src/gwt/client/whirled/WhirledMessages.properties'.
 */
public interface WhirledMessages extends com.google.gwt.i18n.client.Messages {
  
  /**
   * Translated "My Popular Places".
   * 
   * @return translated "My Popular Places"
   * @gwt.key headerPlaces
   */
  String headerPlaces();

  /**
   * Translated "View Profile".
   * 
   * @return translated "View Profile"
   * @gwt.key viewProfile
   */
  String viewProfile();

  /**
   * Translated "You currently have no active games.<br/>
   * Check <a href="/#{0}">Whirledwide</a> to see what other people are playing!".
   * 
   * @return translated "You currently have no active games.<br/>
   * Check <a href="/#{0}">Whirledwide</a> to see what other people are playing!"
   * @gwt.key emptyActiveGames
   */
  String emptyActiveGames(String arg0);

  /**
   * Translated "You currently have no popular places.<br/>
   * You can always visit <a href="/#{0}">your home</a> or<br/>
   * check <a href="/#{1}">Whirledwide</a> to get started!".
   * 
   * @return translated "You currently have no popular places.<br/>
   * You can always visit <a href="/#{0}">your home</a> or<br/>
   * check <a href="/#{1}">Whirledwide</a> to get started!"
   * @gwt.key emptyPopularPlaces
   */
  String emptyPopularPlaces(String arg0,  String arg1);

  /**
   * Translated "Current Whirled Population: {0}".
   * 
   * @return translated "Current Whirled Population: {0}"
   * @gwt.key populationDisplay
   */
  String populationDisplay(String arg0);

  /**
   * Translated "Next".
   * 
   * @return translated "Next"
   * @gwt.key next
   */
  String next();

  /**
   * Translated "My Games".
   * 
   * @return translated "My Games"
   * @gwt.key headerGames
   */
  String headerGames();

  /**
   * Translated "Top Games".
   * 
   * @return translated "Top Games"
   * @gwt.key headerTopGames
   */
  String headerTopGames();

  /**
   * Translated "No active group chats".
   * 
   * @return translated "No active group chats"
   * @gwt.key noChats
   */
  String noChats();

  /**
   * Translated "Join {0}''s Game".
   * 
   * @return translated "Join {0}''s Game"
   * @gwt.key goToGame
   */
  String goToGame(String arg0);

  /**
   * Translated "None of your Friends are Online".
   * 
   * @return translated "None of your Friends are Online"
   * @gwt.key noPeople
   */
  String noPeople();

  /**
   * Translated ", including: ".
   * 
   * @return translated ", including: "
   * @gwt.key populationFriends
   */
  String populationFriends();

  /**
   * Translated "News".
   * 
   * @return translated "News"
   * @gwt.key headerNews
   */
  String headerNews();

  /**
   * Translated "Active Chats".
   * 
   * @return translated "Active Chats"
   * @gwt.key headerChats
   */
  String headerChats();

  /**
   * Translated "Prev".
   * 
   * @return translated "Prev"
   * @gwt.key prev
   */
  String prev();

  /**
   * Translated "Whirledwide".
   * 
   * @return translated "Whirledwide"
   * @gwt.key titleWhirledwide
   */
  String titleWhirledwide();

  /**
   * Translated "My Whirled".
   * 
   * @return translated "My Whirled"
   * @gwt.key titleMyWhirled
   */
  String titleMyWhirled();

  /**
   * Translated "What''s going <br/>on in My Whirled <br/>right now.".
   * 
   * @return translated "What''s going <br/>on in My Whirled <br/>right now."
   * @gwt.key myWhirledDescription
   */
  String myWhirledDescription();

  /**
   * Translated "Players".
   * 
   * @return translated "Players"
   * @gwt.key headerPlayers
   */
  String headerPlayers();

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
   * Translated "What''s new and<br/>exciting in the<br/>Whirled at large.".
   * 
   * @return translated "What''s new and<br/>exciting in the<br/>Whirled at large."
   * @gwt.key whirledwideDescription
   */
  String whirledwideDescription();

  /**
   * Translated "Pop. {0}".
   * 
   * @return translated "Pop. {0}"
   * @gwt.key population
   */
  String population(String arg0);
}
