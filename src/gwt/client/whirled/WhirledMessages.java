package client.whirled;


/**
 * Interface to represent the messages contained in resource  bundle:
 * 	/export/msoy/src/gwt/client/whirled/WhirledMessages.properties'.
 */
public interface WhirledMessages extends com.google.gwt.i18n.client.Messages {
  
  /**
   * Translated ", including:".
   * 
   * @return translated ", including:"
   * @gwt.key populationFriends
   */
  String populationFriends();

  /**
   * Translated "Current Whirled Population: {0}".
   * 
   * @return translated "Current Whirled Population: {0}"
   * @gwt.key populationDisplay
   */
  String populationDisplay(String arg0);

  /**
   * Translated "Join {0}''s Game".
   * 
   * @return translated "Join {0}''s Game"
   * @gwt.key goToGame
   */
  String goToGame(String arg0);

  /**
   * Translated "No news is good news.<br/>
   * Check <a href="/#{0}">Whirledwide</a> to see what other people are doing!".
   * 
   * @return translated "No news is good news.<br/>
   * Check <a href="/#{0}">Whirledwide</a> to see what other people are doing!"
   * @gwt.key emptyFeed
   */
  String emptyFeed(String arg0);

  /**
   * Translated "My Popular Places".
   * 
   * @return translated "My Popular Places"
   * @gwt.key headerPlaces
   */
  String headerPlaces();

  /**
   * Translated "Next".
   * 
   * @return translated "Next"
   * @gwt.key next
   */
  String next();

  /**
   * Translated "None of your Friends are Online".
   * 
   * @return translated "None of your Friends are Online"
   * @gwt.key noPeople
   */
  String noPeople();

  /**
   * Translated "<a href="/#{0}">Show older messages</a>.".
   * 
   * @return translated "<a href="/#{0}">Show older messages</a>."
   * @gwt.key fullFeed
   */
  String fullFeed(String arg0);

  /**
   * Translated "My Friends Online".
   * 
   * @return translated "My Friends Online"
   * @gwt.key headerPeople
   */
  String headerPeople();

  /**
   * Translated "Prev".
   * 
   * @return translated "Prev"
   * @gwt.key prev
   */
  String prev();

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
   * Translated "{0} has updated their <a href="/#{1}">room</a>.".
   * 
   * @return translated "{0} has updated their <a href="/#{1}">room</a>."
   * @gwt.key friendUpdatedRoom
   */
  String friendUpdatedRoom(String arg0,  String arg1);

  /**
   * Translated "{0} earned the {1} trophy.".
   * 
   * @return translated "{0} earned the {1} trophy."
   * @gwt.key friendWonTrophy
   */
  String friendWonTrophy(String arg0,  String arg1);

  /**
   * Translated "Players".
   * 
   * @return translated "Players"
   * @gwt.key headerPlayers
   */
  String headerPlayers();

  /**
   * Translated "What''s going <br/>on in My Whirled <br/>right now.".
   * 
   * @return translated "What''s going <br/>on in My Whirled <br/>right now."
   * @gwt.key myWhirledDescription
   */
  String myWhirledDescription();

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
   * Translated "News".
   * 
   * @return translated "News"
   * @gwt.key headerNews
   */
  String headerNews();

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
   * Translated "{0} has added the {1} {2} to the catalog.".
   * 
   * @return translated "{0} has added the {1} {2} to the catalog."
   * @gwt.key friendListedItem
   */
  String friendListedItem(String arg0,  String arg1,  String arg2);

  /**
   * Translated "Top Games".
   * 
   * @return translated "Top Games"
   * @gwt.key headerTopGames
   */
  String headerTopGames();

  /**
   * Translated "What's Whirling?".
   * 
   * @return translated "What's Whirling?"
   * @gwt.key headerFeed
   */
  String headerFeed();

  /**
   * Translated "My Games".
   * 
   * @return translated "My Games"
   * @gwt.key headerGames
   */
  String headerGames();

  /**
   * Translated "No active group chats".
   * 
   * @return translated "No active group chats"
   * @gwt.key noChats
   */
  String noChats();

  /**
   * Translated "View Profile".
   * 
   * @return translated "View Profile"
   * @gwt.key viewProfile
   */
  String viewProfile();

  /**
   * Translated "Pop. {0}".
   * 
   * @return translated "Pop. {0}"
   * @gwt.key population
   */
  String population(String arg0);

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
   * Translated "{0} has added {1} as a friend.".
   * 
   * @return translated "{0} has added {1} as a friend."
   * @gwt.key friendAddedFriend
   */
  String friendAddedFriend(String arg0,  String arg1);

  /**
   * Translated "What''s new and<br/>exciting in the<br/>Whirled at large.".
   * 
   * @return translated "What''s new and<br/>exciting in the<br/>Whirled at large."
   * @gwt.key whirledwideDescription
   */
  String whirledwideDescription();
}
