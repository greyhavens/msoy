package client.mail;


/**
 * Interface to represent the messages contained in resource  bundle:
 * 	/export/msoy/src/gwt/client/mail/MailMessages.properties'.
 */
public interface MailMessages extends com.google.gwt.i18n.client.Messages {
  
  /**
   * Translated "Next".
   * 
   * @return translated "Next"
   * @gwt.key appBtnNext
   */
  String appBtnNext();

  /**
   * Translated "From:".
   * 
   * @return translated "From:"
   * @gwt.key appHdrFrom
   */
  String appHdrFrom();

  /**
   * Translated "Reply".
   * 
   * @return translated "Reply"
   * @gwt.key appBtnReply
   */
  String appBtnReply();

  /**
   * Translated "Mail".
   * 
   * @return translated "Mail"
   * @gwt.key mailTitle
   */
  String mailTitle();

  /**
   * Translated "Prev".
   * 
   * @return translated "Prev"
   * @gwt.key appBtnPrevious
   */
  String appBtnPrevious();

  /**
   * Translated "Delete Selected".
   * 
   * @return translated "Delete Selected"
   * @gwt.key appBtnDeleteSel
   */
  String appBtnDeleteSel();

  /**
   * Translated "Subject: {0}".
   * 
   * @return translated "Subject: {0}"
   * @gwt.key appHdrSubject
   */
  String appHdrSubject(String arg0);

  /**
   * Translated "Delete".
   * 
   * @return translated "Delete"
   * @gwt.key appBtnDelete
   */
  String appBtnDelete();

  /**
   * Translated "<no subject>".
   * 
   * @return translated "<no subject>"
   * @gwt.key appHdrNoSubject
   */
  String appHdrNoSubject();

  /**
   * Translated "Mail".
   * 
   * @return translated "Mail"
   * @gwt.key appMail
   */
  String appMail();

  /**
   * Translated "Please logon above to access your Mail.".
   * 
   * @return translated "Please logon above to access your Mail."
   * @gwt.key logon
   */
  String logon();

  /**
   * Translated "Forward".
   * 
   * @return translated "Forward"
   * @gwt.key appBtnForward
   */
  String appBtnForward();

  /**
   * Translated "Reply All".
   * 
   * @return translated "Reply All"
   * @gwt.key appBtnReplyAll
   */
  String appBtnReplyAll();

  /**
   * Translated "Toggle All".
   * 
   * @return translated "Toggle All"
   * @gwt.key appBtnToggle
   */
  String appBtnToggle();

  /**
   * Translated "Search".
   * 
   * @return translated "Search"
   * @gwt.key appBtnSearch
   */
  String appBtnSearch();
}
