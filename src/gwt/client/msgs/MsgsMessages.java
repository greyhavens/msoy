package client.msgs;


/**
 * Interface to represent the messages contained in resource  bundle:
 * 	/export/msoy/src/gwt/client/msgs/MsgsMessages.properties'.
 */
public interface MsgsMessages extends com.google.gwt.i18n.client.Messages {
  
  /**
   * Translated "This friendship invitation is still pending.".
   * 
   * @return translated "This friendship invitation is still pending."
   * @gwt.key friendPending
   */
  String friendPending();

  /**
   * Translated "You are inviting the recipient of this message to be your friend.".
   * 
   * @return translated "You are inviting the recipient of this message to be your friend."
   * @gwt.key friendInviting
   */
  String friendInviting();

  /**
   * Translated "Choose Different Item".
   * 
   * @return translated "Choose Different Item"
   * @gwt.key giftBtnAnother
   */
  String giftBtnAnother();

  /**
   * Translated "Be Friends!".
   * 
   * @return translated "Be Friends!"
   * @gwt.key friendBtnAccept
   */
  String friendBtnAccept();

  /**
   * Translated "Attach Item".
   * 
   * @return translated "Attach Item"
   * @gwt.key btnAttach
   */
  String btnAttach();

  /**
   * Translated "Please finish selecting an item to attach before sending your message.".
   * 
   * @return translated "Please finish selecting an item to attach before sending your message."
   * @gwt.key giftNoItem
   */
  String giftNoItem();

  /**
   * Translated "There is an item attached to this message. Click the thumbnail to place the item in your inventory.".
   * 
   * @return translated "There is an item attached to this message. Click the thumbnail to place the item in your inventory."
   * @gwt.key giftItem
   */
  String giftItem();

  /**
   * Translated "Send".
   * 
   * @return translated "Send"
   * @gwt.key btnSend
   */
  String btnSend();

  /**
   * Translated "You have no items of this type in your inventory.".
   * 
   * @return translated "You have no items of this type in your inventory."
   * @gwt.key giftNoItems
   */
  String giftNoItems();

  /**
   * Translated "Cancel Attachment".
   * 
   * @return translated "Cancel Attachment"
   * @gwt.key giftCancel
   */
  String giftCancel();

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
   * Translated "Subject:".
   * 
   * @return translated "Subject:"
   * @gwt.key hdrSubject
   */
  String hdrSubject();

  /**
   * Translated "This message once had an item attached to it.".
   * 
   * @return translated "This message once had an item attached to it."
   * @gwt.key giftGone
   */
  String giftGone();

  /**
   * Translated "Message successfully delivered!".
   * 
   * @return translated "Message successfully delivered!"
   * @gwt.key messageSent
   */
  String messageSent();

  /**
   * Translated "Please choose the item to attach by clicking on its thumbnail.".
   * 
   * @return translated "Please choose the item to attach by clicking on its thumbnail."
   * @gwt.key giftChoose
   */
  String giftChoose();

  /**
   * Translated "Member {0}".
   * 
   * @return translated "Member {0}"
   * @gwt.key memberId
   */
  String memberId(String arg0);

  /**
   * Translated "This is an old friendship invitation from "{0}", who has since become your friend.".
   * 
   * @return translated "This is an old friendship invitation from "{0}", who has since become your friend."
   * @gwt.key friendAlreadyFriend
   */
  String friendAlreadyFriend(String arg0);

  /**
   * Translated "Discard".
   * 
   * @return translated "Discard"
   * @gwt.key btnDiscard
   */
  String btnDiscard();

  /**
   * Translated "Invite to Group:".
   * 
   * @return translated "Invite to Group:"
   * @gwt.key groupInviteTo
   */
  String groupInviteTo();

  /**
   * Translated "You can't delete this message until you've accepted the attached item.".
   * 
   * @return translated "You can't delete this message until you've accepted the attached item."
   * @gwt.key giftNoDelete
   */
  String giftNoDelete();

  /**
   * Translated "This was an invitation to the group "{0}", which you have since joined.".
   * 
   * @return translated "This was an invitation to the group "{0}", which you have since joined."
   * @gwt.key groupAlreadyMember
   */
  String groupAlreadyMember(String arg0);

  /**
   * Translated "Your friendship invitation to "{0}" was accepted, and they are now a friend of yours. ".
   * 
   * @return translated "Your friendship invitation to "{0}" was accepted, and they are now a friend of yours. "
   * @gwt.key friendReplyBody
   */
  String friendReplyBody(String arg0);

  /**
   * Translated "Invitation accepted!".
   * 
   * @return translated "Invitation accepted!"
   * @gwt.key friendReplySubject
   */
  String friendReplySubject();

  /**
   * Translated "You''ve accepted this friendship invitation, and "{0}" is now your friend.".
   * 
   * @return translated "You''ve accepted this friendship invitation, and "{0}" is now your friend."
   * @gwt.key friendAccepted
   */
  String friendAccepted(String arg0);

  /**
   * Translated "This is a friendship invitation. You may accept it by clicking the button below, you can reply to it, or you can simply delete it.".
   * 
   * @return translated "This is a friendship invitation. You may accept it by clicking the button below, you can reply to it, or you can simply delete it."
   * @gwt.key friendInvitation
   */
  String friendInvitation();

  /**
   * Translated "Yes, I want to join!".
   * 
   * @return translated "Yes, I want to join!"
   * @gwt.key groupBtnJoin
   */
  String groupBtnJoin();

  /**
   * Translated "The item you're sending:".
   * 
   * @return translated "The item you're sending:"
   * @gwt.key giftChosen
   */
  String giftChosen();

  /**
   * Translated "You have received an invitation to become a member of the group "{0}". You may join the group by clicking on the button below, or you can reply to this message if you have any questions for the person who invited you.".
   * 
   * @return translated "You have received an invitation to become a member of the group "{0}". You may join the group by clicking on the button below, or you can reply to this message if you have any questions for the person who invited you."
   * @gwt.key groupInvitation
   */
  String groupInvitation(String arg0);
}
