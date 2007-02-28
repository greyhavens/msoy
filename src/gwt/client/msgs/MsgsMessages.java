package client.msgs;


/**
 * Interface to represent the messages contained in resource  bundle:
 * 	/export/msoy/src/gwt/client/msgs/MsgsMessages.properties'.
 */
public interface MsgsMessages extends com.google.gwt.i18n.client.Messages {
  
  /**
   * Translated "Click to".
   * 
   * @return translated "Click to"
   * @gwt.key groupClick
   */
  String groupClick();

  /**
   * Translated "Discard".
   * 
   * @return translated "Discard"
   * @gwt.key btnDiscard
   */
  String btnDiscard();

  /**
   * Translated "Member {0}".
   * 
   * @return translated "Member {0}"
   * @gwt.key memberId
   */
  String memberId(String arg0);

  /**
   * Translated "Compose Mail".
   * 
   * @return translated "Compose Mail"
   * @gwt.key popupHeader
   */
  String popupHeader();

  /**
   * Translated "the group".
   * 
   * @return translated "the group"
   * @gwt.key groupThe
   */
  String groupThe();

  /**
   * Translated "Invite to Group:".
   * 
   * @return translated "Invite to Group:"
   * @gwt.key groupInvite
   */
  String groupInvite();

  /**
   * Translated "To:".
   * 
   * @return translated "To:"
   * @gwt.key hdrTo
   */
  String hdrTo();

  /**
   * Translated "Subject:".
   * 
   * @return translated "Subject:"
   * @gwt.key hdrSubject
   */
  String hdrSubject();

  /**
   * Translated "join".
   * 
   * @return translated "join"
   * @gwt.key groupJoin
   */
  String groupJoin();

  /**
   * Translated "Send".
   * 
   * @return translated "Send"
   * @gwt.key btnSend
   */
  String btnSend();
}
