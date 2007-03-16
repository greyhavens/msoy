package client.msgs;


/**
 * Interface to represent the messages contained in resource  bundle:
 * 	/export/msoy/src/gwt/client/msgs/MsgsMessages.properties'.
 */
public interface MsgsMessages extends com.google.gwt.i18n.client.Messages {
  
  /**
   * Translated "This was an invitation to the group "{0}", which you have since joined.".
   * 
   * @return translated "This was an invitation to the group "{0}", which you have since joined."
   * @gwt.key groupAlreadyMember
   */
  String groupAlreadyMember(String arg0);

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
   * Translated "Attach Item".
   * 
   * @return translated "Attach Item"
   * @gwt.key btnAttach
   */
  String btnAttach();

  /**
   * Translated "Compose Mail".
   * 
   * @return translated "Compose Mail"
   * @gwt.key popupHeader
   */
  String popupHeader();

  /**
   * Translated "To:".
   * 
   * @return translated "To:"
   * @gwt.key hdrTo
   */
  String hdrTo();

  /**
   * Translated "Invite to Group:".
   * 
   * @return translated "Invite to Group:"
   * @gwt.key groupInviteTo
   */
  String groupInviteTo();

  /**
   * Translated "Subject:".
   * 
   * @return translated "Subject:"
   * @gwt.key hdrSubject
   */
  String hdrSubject();

  /**
   * Translated "Yes, I want to join!".
   * 
   * @return translated "Yes, I want to join!"
   * @gwt.key groupBtnJoin
   */
  String groupBtnJoin();

  /**
   * Translated "Send".
   * 
   * @return translated "Send"
   * @gwt.key btnSend
   */
  String btnSend();

  /**
   * Translated "You have received an invitation to become a member of the group "{0}". You may join the group by clicking on the button below, or you can reply to this message if you have any questions for the person who invited you.".
   * 
   * @return translated "You have received an invitation to become a member of the group "{0}". You may join the group by clicking on the button below, or you can reply to this message if you have any questions for the person who invited you."
   * @gwt.key groupInvitation
   */
  String groupInvitation(String arg0);
}
