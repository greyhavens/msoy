//
// $Id$

package com.threerings.msoy.person.server;

import java.io.StringWriter;
import java.util.List;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import com.samskivert.util.Invoker;
import com.samskivert.velocity.VelocityUtil;

import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.VelocityEngine;

import com.threerings.presents.annotation.BlockingThread;
import com.threerings.presents.annotation.MainInvoker;

import com.threerings.presents.dobj.RootDObjectManager;

import com.threerings.msoy.data.all.DeploymentConfig;
import com.threerings.msoy.server.MemberNodeActions;
import com.threerings.msoy.server.ServerConfig;
import com.threerings.msoy.server.ServerMessages;
import com.threerings.msoy.server.persist.MemberRecord;
import com.threerings.msoy.server.persist.MemberRepository;
import com.threerings.msoy.server.util.JSONMarshaller;
import com.threerings.msoy.server.util.MailSender;

import com.threerings.msoy.item.data.all.Item;
import com.threerings.msoy.item.data.all.ItemIdent;
import com.threerings.msoy.item.server.ItemLogic;
import com.threerings.msoy.item.server.persist.ItemRecord;
import com.threerings.msoy.item.server.persist.ItemRepository;

import com.threerings.msoy.web.data.ServiceCodes;
import com.threerings.msoy.web.data.ServiceException;

import com.threerings.msoy.mail.gwt.FriendInvitePayload;
import com.threerings.msoy.mail.gwt.MailPayload;
import com.threerings.msoy.mail.gwt.PresentPayload;
import com.threerings.msoy.mail.server.persist.ConvMessageRecord;
import com.threerings.msoy.mail.server.persist.ConversationRecord;
import com.threerings.msoy.mail.server.persist.MailRepository;

import static com.threerings.msoy.Log.log;

/**
 * Provides mail related services to servlets and other blocking thread entities.
 */
@Singleton @BlockingThread
public class MailLogic
{
    /**
     * Sends a friend invitation email from the supplied inviter to the specified member.
     */
    public void sendFriendInvite (int inviterId, int friendId)
        throws ServiceException
    {
        MemberRecord sender = _memberRepo.loadMember(inviterId);
        MemberRecord recip = _memberRepo.loadMember(friendId);
        if (sender == null || recip == null) {
            log.warning("Missing records for friend invite [iid=" + inviterId +
                        ", tid=" + friendId + ", irec=" + sender + ", trec=" + recip + "].");
            throw new ServiceException(ServiceCodes.E_INTERNAL_ERROR);
        }
        String subj = _serverMsgs.getBundle("server").get("m.friend_invite_subject");
        String body = _serverMsgs.getBundle("server").get("m.friend_invite_body");
        startConversation(sender, recip, subj, body, new FriendInvitePayload());
    }

    /**
     * Starts a mail conversation between the specified two parties.
     */
    public void startConversation (MemberRecord sender, MemberRecord recip,
                                   String subject, String body, MailPayload attachment)
        throws ServiceException
    {
        // if the payload is an item attachment, transfer it to the recipient
        processPayload(sender.memberId, recip.memberId, attachment);

        // now start the conversation (and deliver the message)
        _mailRepo.startConversation(recip.memberId, sender.memberId, subject, body, attachment);

        // potentially send a real email to the recipient
        sendMailEmail(sender, recip, subject, body);

        // let recipient know they've got mail
        MemberNodeActions.reportUnreadMail(
            recip.memberId, _mailRepo.loadUnreadConvoCount(recip.memberId));
    }

    /**
     * Continues the specified mail conversation.
     */
    public ConvMessageRecord continueConversation (MemberRecord poster, int convoId, String body,
                                                   MailPayload attachment)
        throws ServiceException
    {
        ConversationRecord conrec = _mailRepo.loadConversation(convoId);
        if (conrec == null) {
            log.warning("Requested to continue non-existent conversation [by=" + poster.who() +
                        ", convoId=" + convoId + "].");
            throw new ServiceException(ServiceCodes.E_INTERNAL_ERROR);
        }

        // make sure this member is a conversation participant
        Long lastRead = _mailRepo.loadLastRead(convoId, poster.memberId);
        if (lastRead == null) {
            log.warning("Request to continue conversation by non-member [who=" + poster.who() +
                        ", convoId=" + convoId + "].");
            throw new ServiceException(ServiceCodes.E_INTERNAL_ERROR);
        }

        // TODO: make sure body.length() is not too long

        // encode the attachment if we have one
        int payloadType = 0;
        byte[] payloadState = null;
        if (attachment != null) {
            payloadType = attachment.getType();
            try {
                payloadState = JSONMarshaller.getMarshaller(
                    attachment.getClass()).getStateBytes(attachment);
            } catch (Exception e) {
                log.warning("Failed to encode message attachment [for=" + poster.who() +
                            ", attachment=" + attachment + "].");
                throw new ServiceException(ServiceCodes.E_INTERNAL_ERROR);
            }
        }

        // if the payload is an item attachment, transfer it to the recipient
        processPayload(poster.memberId, conrec.getOtherId(poster.memberId), attachment);

        // store the message in the repository
        ConvMessageRecord cmr =
            _mailRepo.addMessage(conrec, poster.memberId, body, payloadType, payloadState);

        // update our last read for this conversation to reflect that we've read our message
        _mailRepo.updateLastRead(convoId, poster.memberId, cmr.sent.getTime());

        // let other conversation participant know they've got mail
        int otherId = conrec.getOtherId(poster.memberId);
        MemberNodeActions.reportUnreadMail(otherId, _mailRepo.loadUnreadConvoCount(otherId));

        // potentially send a real email to the recipient
        MemberRecord recip = _memberRepo.loadMember(otherId);
        if (recip != null) {
            String subject = _serverMsgs.getBundle("server").get("m.reply_subject", conrec.subject);
            sendMailEmail(poster, recip, subject, body);
        }

        return cmr;
    }

    /**
     * Sends email to all players who have not opted out of Whirled announcements.
     */
    public void spamPlayers (String subject, String body)
    {
        // TODO: if we want to continue to use this mechanism to send mass emails to our members,
        // we will need to farm out the mail deliver task to all nodes in the network so that we
        // don't task one node with sending out a million email messages

        // convert the body into proper-ish HTML
        body = formatSpam(body);
        if (body == null) {
            return;
        }

        String[] headers = makeSpamHeaders(subject);
        String from = ServerConfig.getFromAddress();
        int count = 0;

        // load up the emails of everyone we want to spam
        List<String> emails = _memberRepo.loadMemberEmailsForAnnouncement();
        for (String recip : emails) {
            _mailer.sendEmail(recip, from, headers, subject, body, true);
            count++;
        }

        // lastly send out mails to our friends at Returnpath (as long as we're not on dev)
        if (!DeploymentConfig.devDeployment) {
            for (String rpaddr : RETURNPATH_ADDRS) {
                _mailer.sendEmail(rpaddr, from, headers, subject, body, true);
                count++;
            }
        }

        log.info("Queued up announcement email", "subject", subject, "count", count);
    }

    /**
     * Sends a spam preview mailing to the specified address.
     */
    public void previewSpam (String recip, String subject, String body)
    {
        // convert the body into proper-ish HTML
        body = formatSpam(body);
        if (body == null) {
            return;
        }
        _mailer.sendEmail(recip, ServerConfig.getFromAddress(), makeSpamHeaders(subject),
                          subject, body, true);
    }

    /**
     * Handles any side-effects of mail payload delivery. Currently that is only the transfer of an
     * item from the sender to the recipient for {@link PresentPayload}.
     */
    protected void processPayload (final int senderId, final int recipId, MailPayload attachment)
        throws ServiceException
    {
        if (attachment instanceof PresentPayload) {
            ItemIdent ident = ((PresentPayload)attachment).ident;
            ItemRepository<?> repo = _itemLogic.getRepository(ident.type);
            final ItemRecord item = repo.loadItem(ident.itemId);

            // validate that they're allowed to gift this item (these are all also checked on the
            // client so we don't need useful error messages)
            String errmsg = null;
            if (item == null) {
                errmsg = "Trying to gift non-existent item";
            } else if (item.ownerId != senderId) {
                errmsg = "Trying to gift un-owned item";
            } else if (item.used != Item.UNUSED) {
                errmsg = "Trying to gift in-use item";
            }
            if (errmsg != null) {
                log.warning(errmsg + " [sender=" + senderId + ", recip=" + recipId +
                            ", ident=" + ident + "].");
                throw new ServiceException(ServiceCodes.E_INTERNAL_ERROR);
            }

            final ItemRecord oitem = (ItemRecord)item.clone();
            repo.updateOwnerId(item, recipId);

            // notify the item system that the item has moved
            _itemLogic.itemUpdated(oitem, item);
        }
    }

    /**
     * Wraps the supplied (HTML) spam body in some basic necessaries.
     */
    protected String formatSpam (String body)
    {
        // convert the body into proper-ish HTML
        try {
            StringWriter swout = new StringWriter();
            VelocityContext ctx = new VelocityContext();
            ctx.put("base_url", ServerConfig.getServerURL());
            ctx.put("content", body);
            VelocityEngine ve = VelocityUtil.createEngine();
            ve.mergeTemplate("rsrc/email/wrapper/message.html", "UTF-8", ctx, swout);
            return swout.toString();

        } catch (Exception e) {
            log.warning("Unable to format spam message", e);
            return null;
        }
    }

    /**
     * Generates the headers needed by Return Path to track our mails.
     */
    protected String[] makeSpamHeaders (String subject)
    {
        return new String[] {
            RP_CAMPAIGN_HEADER, RP_CAMPAIGN_PREFIX + subject.toLowerCase().replace(" ", "_"),
        };
    }

    /**
     * Send an email to a Whirled mail recipient to report that they received a Whirled mail. Does
     * nothing if the recipient has requested not to receive such mails.
     */
    protected void sendMailEmail (MemberRecord sender, MemberRecord recip,
                                  String subject, String body)
    {
        // if they don't want to hear about it, stop now
        if (recip.isSet(MemberRecord.Flag.NO_WHIRLED_MAIL_TO_EMAIL)) {
            return;
        }
        _mailer.sendTemplateEmail(
            recip.accountName, ServerConfig.getFromAddress(), "gotMail",
            "subject", subject,"sender", sender.name, "senderId", sender.memberId,
            "body", body, "server_url", ServerConfig.getServerURL());
    }

    @Inject protected RootDObjectManager _omgr;
    @Inject protected ServerMessages _serverMsgs;
    @Inject protected @MainInvoker Invoker _invoker;
    @Inject protected MailSender _mailer;
    @Inject protected ItemLogic _itemLogic;
    @Inject protected MailRepository _mailRepo;
    @Inject protected MemberRepository _memberRepo;

    protected static final int MEMBERS_PER_LOOP = 100;

    protected static final String RP_CAMPAIGN_HEADER = "X-campaignid";
    protected static final String RP_CAMPAIGN_PREFIX = "threeringsdesign_";

    /** 982 email accounts that we include in our user spammage so that we can have Returnpath tell
     * us whether or not our mails are getting through. */
    protected static final String[] RETURNPATH_ADDRS = new String[] {
        "12garrabrandt_man@earthlink.net",
        "aaa01bluewin@bluewin.ch",
        "aaa01hana@hanafos.com",
        "aaa01rp3@aol.co.uk",
        "aaa01vsnl@vsnl.net",
        "aaa111rp@charter.net",
        "Aaa141929@aol.fr",
        "aaa1@pacific.net.sg",
        "aaa1@uol.com.br",
        "aaa1asi@aol.com",
        "aaa1asi@bellsouth.net",
        "aaa1asi@comcast.net",
        "aaa1asi@cox.net",
        "aaa1asi@earthlink.net",
        "aaa1asi@gmail.com",
        "aaa1asi@hotmail.com",
        "aaa1asi@mac.com",
        "aaa1asi@mail.com",
        "aaa1asi@netzero.com",
        "aaa1asi@optonline.com",
        "aaa1asi@usa.net",
        "aaa1asi@worldnet.att.net",
        "aaa1asi@yahoo.com",
        "aaa1cantv@cantv.net",
        "aaa1datanet@mail.datanet.hu",
        "aaa1elisa@kolumbus.fi",
        "aaa1globo@globo.com",
        "aaa1hot@msn.com",
        "aaa1likos@lycos.com",
        "aaa1neuf@neuf.fr",
        "aaa1sif@sify.com",
        "aaa1talk@talktalk.net",
        "aaa1tele2@tele2.fr",
        "aaa1telu@telus.net",
        "aaa1terrabr@terra.com.br",
        "aaa2as1@msn.com",
        "aaa2asi@comcast.net",
        "aaa2asi@earthlink.net",
        "aaa3asi@cs.com",
        "aaa4asi@aol.com",
        "aaa522asi@biz.rr.com",
        "aaa5asi@netscape.net",
        "aaaa1paradise@paradise.net.nz",
        "aaretpath@arcor.de",
        "acacius.page@mac.com",
        "adresmaybar@yahoo.com",
        "agent20mcclurg@aol.com",
        "agiordano_14@msn.com",
        "al.alleborn@comcast.net",
        "al@apaca.wanadoo.co.uk",
        "alexander1brent@yahoo.es",
        "allen2kenderick@yahoo.es",
        "allen3will@yahoo.es",
        "andrew38jeffery@rogers.com",
        "angie75will@primus.ca",
        "anita1232@optonline.com",
        "anna87wright@primus.ca",
        "auoptus1313@optusnet.com.au",
        "auoptus1414@optusnet.com.au",
        "auoptus1515@optusnet.com.au",
        "auoptus1616@optusnet.com.au",
        "auoptus1717@optusnet.com.au",
        "auoptus1818@optusnet.com.au",
        "auoptus1919@optusnet.com.au",
        "axelnortby@hotmail.com",
        "ba9sel@freesurf.ch",
        "baconracer33@aol.com",
        "barber4tiki@yahoo.es",
        "bartender85aybar@yahoo.com",
        "bbb.2@uol.com.br",
        "bbb02redifmail@rediffmail.com",
        "bbb02tele2@tele2.se",
        "bbb02tutopia@tutopia.com",
        "bbb02yah@yahoo.com.cn",
        "bbb2@pacific.net.sg",
        "bbb2aliceadsl@aliceadsl.fr",
        "bbb2bigp@bigpond.com",
        "bbb2bol@bol.com.br",
        "bbb2bt@iolfree.ie",
        "bbb2cantv@cantv.net",
        "bbb2caramail@caramail.com",
        "bbb2elisa@kolumbus.fi",
        "bbb2fullzero@fullzero.com.ar",
        "bbb2globo@globo.com",
        "bbb2igb@ig.com.br",
        "bbb2mail@mail.ru",
        "bbb2mipunto@mipunto.com",
        "bbb2netfin@returnpath.hk",
        "bbb2nl@aol.nl",
        "bbb2one@onerp.dk",
        "bbb2orange@orange.fr",
        "bbb2orange@orangerp.es",
        "bbb2ozemail@ozemail.com.au",
        "bbb2sapo@sapo.pt",
        "bbb2talk@talktalk.net",
        "bbb2tele2@tele2.fr",
        "bbb2Telefonica@iDisc.e.telefonica.net",
        "bbb2telmex@prodigy.net.mx",
        "bbb2telu@telus.net",
        "bbb2tesco@tesco.net",
        "bbb2tiscal@tiscali.it",
        "bbb2yah@yahoo.com.sg",
        "bbb2yhhk@yahoo.com.hk",
        "bbb2yho@yahoo.com.au",
        "bbb2yindi@yahoo.co.in",
        "bbb2yjp@yahoo.co.jp",
        "bbb315amv@21cn.com",
        "bbb315amv@tom.com",
        "bbb@bbbukorange.orangehome.co.uk",
        "BbbA946@aol.fr",
        "bbbb2paradise@paradise.net.nz",
        "bbbb2shaw@shaw.ca",
        "bbbtwo.belgacom@belgacom.net",
        "beaubillinslea3@yahoo.com",
        "beauburnside@mac.com",
        "becky6german@yahoo.fr",
        "belle4tulipe@wanadoo.fr",
        "beniangiatti12@optonline.com",
        "bill.taylor@mail.com",
        "bill.the.cat@hotmail.co.uk",
        "billy_macchio2@msn.com",
        "black_barry@cox.net",
        "bobbi.harlow@hotmail.co.uk",
        "brenda34coxon@aol.com",
        "brsmith17@netzero.com",
        "bwa31958@nifty.com",
        "bwa31960@nifty.com",
        "bwa31965@nifty.com",
        "bwa31967@nifty.com",
        "bwa31969@nifty.com",
        "c.grazer@worldnet.att.net",
        "cami8roberto@yahoo.co.uk",
        "cami9roberto@yahoo.es",
        "cammie88deakins@optonline.com",
        "carla66teale@globetrotter.net",
        "carpetman_miller22@hotmail.com",
        "ccc.3@uol.com.br",
        "ccc03rp3@aol.co.uk",
        "ccc03vsnl@vsnl.net",
        "ccc316amv@21cn.com",
        "ccc316amv@tom.com",
        "ccc3cantv@cantv.net",
        "ccc3datanet@mail.datanet.hu",
        "ccc3elisa@kolumbus.fi",
        "ccc3globo@globo.com",
        "ccc3hot@msn.com",
        "ccc3neuf@neuf.fr",
        "ccc3skynet@sky.cz",
        "ccc3talk@talktalk.net",
        "ccc3tele2@tele2.fr",
        "ccc3terrabr@terra.com.br",
        "ccc3tesco@tesco.net",
        "Ccc54965@aol.fr",
        "cccc3paradise@paradise.net.nz",
        "ccknight3@hotmail.com",
        "charles_larryparks12@yahoo.com",
        "charlieshull@yahoo.com",
        "chaykin_tipton92@msn.com",
        "cheriwells@usa.net",
        "chien2chaud@wanadoo.fr",
        "chipdarlington@earthlink.net",
        "chris26panton@cgocable.ca",
        "chrissy@chrissysnow.wanadoo.co.uk",
        "claire37inglis@rogers.com",
        "clubi4quatre@club-internet.fr",
        "clubi5cinq@club-internet.fr",
        "clubi6six@club-internet.fr",
        "clubi7sept@club-internet.fr",
        "clubi8huit@club-internet.fr",
        "clubi9neuf@club-internet.fr",
        "cmeisch_1114@msn.com",
        "cochran31_meyrink@yahoo.com",
        "commanderette9@btinternet.com",
        "corkigrazer12@usa.net",
        "csweigart@worldnet.att.net",
        "cusackliz55@cox.net",
        "cutter.john@hotmail.co.uk",
        "dan@dhansen6.wanadoo.co.uk",
        "daniel_ades@yahoo.com",
        "danny@dchow.wanadoo.co.uk",
        "dardensevern@cs.com",
        "darlington_chip41@yahoo.com",
        "daryl34atkin@globetrotter.net",
        "dave13early@aol.com",
        "david.marvit@sbcglobal.net",
        "david.stipes@mail.com",
        "david.ursin@sbcglobal.net",
        "david54hyde@rogers.com",
        "dbabcock1@mac.com",
        "dc07@mail.rtr-server.de",
        "dc08@mail.rtr-server.de",
        "dc09@mail.rtr-server.de",
        "dc10@mail.rtr-server.de",
        "dc11@mail.rtr-server.de",
        "dc25@mail.rtr-server.de",
        "ddd04rp3@aol.co.uk",
        "ddd04tele2@tele2.se",
        "ddd04tutopia@tutopia.com",
        "ddd04yah@yahoo.com.cn",
        "ddd111rp@charter.net",
        "ddd1likos@lycos.com",
        "ddd1sify@sify.com",
        "ddd1xiteB@excite.com",
        "ddd1yahmx@yahoo.com.mx",
        "ddd317amv@21cn.com",
        "ddd317amv@tom.com",
        "ddd4@uol.com.br",
        "ddd4aliceadsl@aliceadsl.fr",
        "ddd4bol@bol.com.br",
        "ddd4bt@iolfree.ie",
        "ddd4caramail@caramail.com",
        "ddd4fullzero@fullzero.com.ar",
        "ddd4globo@globo.com",
        "ddd4goon@goo.jp",
        "ddd4igd@ig.com.br",
        "ddd4mail@mail.ru",
        "ddd4mipunto@mipunto.com",
        "ddd4netfin@returnpath.hk",
        "ddd4nl@aol.nl",
        "ddd4one@onerp.dk",
        "ddd4orange@orange.fr",
        "ddd4orange@orangerp.es",
        "ddd4sapo@sapo.pt",
        "ddd4talk@talktalk.net",
        "ddd4Telefonica@iDisc.e.telefonica.net",
        "ddd4telmex@prodigy.net.mx",
        "ddd4tiscal@tiscali.it",
        "ddd4yah@yahoo.co.in",
        "ddd4yasing@yahoo.com.sg",
        "ddd4yhhk@yahoo.com.hk",
        "ddd4yho@yahoo.com.au",
        "ddd4yjp@yahoo.co.jp",
        "Ddd65950@aol.fr",
        "ddd@dddukorange.orangehome.co.uk",
        "dddd4paradise@paradise.net.nz",
        "dddd4shaw@shaw.ca",
        "dddfour.BELGACOM@belgacom.net",
        "dddvrzn@verizon.net",
        "ddelivery01@hanmail.net",
        "defecircle@t-online.de",
        "delauter3@yahoo.com",
        "delta3droman@yahoo.co.uk",
        "delta8droman@gmx.de",
        "delta8droman@yahoo.es",
        "devlin_miltondean3@hotmail.com",
        "dmuller_10@msn.com",
        "doloris@dejung.wanadoo.co.uk",
        "donal38nolan@cgocable.ca",
        "doobiehankin@aol.com",
        "dorsey_congressman77@hotmail.com",
        "dreyfus_22randolph@yahoo.com",
        "dsmonterosso@sbcglobal.net",
        "dvdunderwood312@aol.com",
        "ecircle1@hotmail.fr",
        "ecircle1@laposte.net",
        "ecircle2@laposte.net",
        "ecircle3@hotmail.fr",
        "ecircle3@laposte.net",
        "ecircle4@hotmail.fr",
        "ecircletest@smartemail.co.uk",
        "ed.mcnamara@worldnet.att.net",
        "eddie@eprimus.wanadoo.co.uk",
        "eee05rp3@aol.co.uk",
        "eee22likos@lycos.com",
        "eee2eir@eircom.net",
        "eee2sify@sify.com",
        "eee2xiteB@excite.com",
        "eee2yahmx@yahoo.com.mx",
        "eee318amv@21cn.com",
        "eee318amv@tom.com",
        "eee3telu@telus.net",
        "eee4bigp@bigpond.com",
        "eee4ozemail@ozemail.com.au",
        "eee5@pacific.net.sg",
        "eee5@uol.com.br",
        "eee5hot@msn.com",
        "eee5neuf@neuf.fr",
        "eee5talk@talktalk.net",
        "eee5terrabr@terra.com.br",
        "eee5tesco@tesco.net",
        "Eee855@aol.fr",
        "eeee5paradise@paradise.net.nz",
        "elton44john@globetrotter.net",
        "elvis76costello@globetrotter.net",
        "ess9en@freenet.de",
        "eugenezador@cs.com",
        "fa3fon@yahoo.fr",
        "farley3_mcgill@msn.com",
        "fenton_okomoto12@earthlink.net",
        "fenton_okomoto12@hotmail.com",
        "ferrero47clerk@aol.com",
        "fff06bluewin@bluewin.ch",
        "fff06redifmail@rediffmail.com",
        "fff06rp3@aol.co.uk",
        "fff06tele2@tele2.se",
        "fff06tutopia@tutopia.com",
        "fff06yah@yahoo.com.cn",
        "fff319amv@21cn.com",
        "fff319amv@tom.com",
        "fff4telu@telus.net",
        "fff6aliceadsl@aliceadsl.fr",
        "fff6bol@bol.com.br",
        "fff6bt@iolfree.ie",
        "fff6caramail@caramail.com",
        "fff6fullzero@fullzero.com.ar",
        "fff6goon@goo.jp",
        "fff6igf@ig.com.br",
        "fff6mail@mail.ru",
        "fff6mipunto@mipunto.com",
        "fff6netfin@returnpath.hk",
        "fff6nl@aol.nl",
        "fff6one@onerp.dk",
        "fff6orange@orange.fr",
        "fff6sapo@sapo.pt",
        "fff6telmex@prodigy.net.mx",
        "fff6tiscal@tiscali.it",
        "fff6yah@yahoo.com.sg",
        "fff6yhhk@yahoo.com.hk",
        "fff6yho@yahoo.com.au",
        "fff6yindi@yahoo.co.in",
        "fff6yjp@yahoo.co.jp",
        "fff@fffukorange.orangehome.co.uk",
        "ffff6paradise@paradise.net.nz",
        "ffff6shaw@shaw.ca",
        "fffg637a@aol.fr",
        "fffsix.BELGACOM@belgacom.net",
        "flo64zachau@primus.ca",
        "force92backes@earthlink.net",
        "fran7krempo@tiscali.co.uk",
        "fran9krempo@yahoo.co.uk",
        "frankbreur@aol.com",
        "fre4quatre@free.fr",
        "fre5cinq@free.fr",
        "fre6six@free.fr",
        "fre7sept@free.fr",
        "fre8huit@free.fr",
        "fre9neuf@free.fr",
        "free0null@freenet.de",
        "frickermackenzie@netscape.net",
        "friedhofer.hugo2@comcast.net",
        "gabriellaborzell@cs.com",
        "gad3cool@free.fr",
        "gavi54round@cgocable.ca",
        "ggg02hana@hanafos.com",
        "ggg07rp3@aol.co.uk",
        "ggg07vsnl@vsnl.net",
        "ggg111rp@charter.net",
        "ggg259v@aol.fr",
        "ggg3yahmx@yahoo.com.mx",
        "ggg7@pacific.net.sg",
        "ggg7@uol.com.br",
        "ggg7datanet@mail.datanet.hu",
        "ggg7hot@msn.com",
        "ggg7neuf@neuf.fr",
        "ggg7skynet@sky.cz",
        "ggg7terrabr@terra.com.br",
        "gggvrzn@verizon.net",
        "gghan02@hanmail.net",
        "ghiecircle@t-online.de",
        "gia33sadhwani@cgocable.ca",
        "gjarrett919@hotmail.com",
        "goodman512stiller@biz.rr.com",
        "gordon.cusack2@cox.net",
        "grant45connor@aol.com",
        "grazer19_produce@earthlink.net",
        "gritscook33walker@msn.com",
        "gros8plouf@yahoo.fr",
        "guardgenevie_21@msn.com",
        "gupson@mac.com",
        "hans377mohlman@dc.rr.com",
        "hansonbrothers1977@comcast.net",
        "harriet4michaels@netscape.net",
        "hathaway_jerry85@yahoo.com",
        "helen54judd@rogers.com",
        "hellen@hellen.wanadoo.co.uk",
        "henry.roth2@gmail.com",
        "hhh08redifmail@rediffmail.com",
        "hhh08rp3@aol.co.uk",
        "hhh08tele2@tele2.se",
        "hhh08tutopia@tutopia.com",
        "hhh08yah@yahoo.com.cn",
        "hhh3likos@lycos.com",
        "hhh3sify@sify.com",
        "hhh3xiteB@excite.com",
        "hhh4yahmx@yahoo.com.mx",
        "hhh8@uol.com.br",
        "hhh8aliceadsl@aliceadsl.fr",
        "hhh8bol@bol.com.br",
        "hhh8bt@iolfree.ie",
        "hhh8caramail@caramail.com",
        "hhh8fullzero@fullzero.com.ar",
        "hhh8goon@goo.jp",
        "hhh8igh@ig.com.br",
        "hhh8mipunto@mipunto.com",
        "hhh8netfin@returnpath.hk",
        "hhh8nl@aol.nl",
        "hhh8one@onerp.dk",
        "hhh8orange@orange.fr",
        "hhh8orange@orangerp.es",
        "hhh8sapo@sapo.pt",
        "hhh8Telefonica@iDisc.e.telefonica.net",
        "hhh8telmex@prodigy.net.mx",
        "hhh8tiscal@tiscali.it",
        "hhh8yah@yahoo.com.sg",
        "hhh8yhhk@yahoo.com.hk",
        "hhh8yho@yahoo.com.au",
        "hhh8yindi@yahoo.co.in",
        "hhh@hhhukorange.orangehome.co.uk",
        "hhheight.BELGACOM@belgacom.net",
        "hhhh8shaw@shaw.ca",
        "hodge.podge@hotmail.co.uk",
        "homie99plimpton@videotron.ca",
        "hymanmyron@aol.com",
        "iii09rp3@aol.co.uk",
        "iii4eir@eircom.net",
        "iii5yahmx@yahoo.com.mx",
        "iii6bigp@bigpond.com",
        "iii6ozemail@ozemail.com.au",
        "iii9@uol.com.br",
        "iii9hot@msn.com",
        "iii9terrabr@terra.com.br",
        "ina_gould@earthlink.net",
        "irving38glick@globetrotter.net",
        "irving48berlin@videotron.ca",
        "isabelcooley@earthlink.net",
        "isabellewalker@usa.net",
        "israel_neal22@earthlink.net",
        "izzy5montrose@yahoo.co.uk",
        "izzy7montrose@gmx.de",
        "izzy7montrose@yahoo.es",
        "j_blum33@bellsouth.net",
        "jack93black@globetrotter.net",
        "james32lamb@rogers.com",
        "james_carrington@sbcglobal.net",
        "james_leicester33@comcast.net",
        "jaystuler720@aol.com",
        "jean49charest@globetrotter.net",
        "jeanne.mori@worldnet.att.net",
        "jeffhiggins303@aol.com",
        "jjgries2002@yahoo.com",
        "jjj.10@uol.com.br",
        "jjj10aliceadsl@aliceadsl.fr",
        "jjj10bol@bol.com.br",
        "jjj10bt@iolfree.ie",
        "jjj10caramail@caramail.com",
        "jjj10fullzero@fullzero.com.ar",
        "jjj10igj@ig.com.br",
        "jjj10mail@mail.ru",
        "jjj10mipunto@mipunto.com",
        "jjj10netfin@returnpath.hk",
        "jjj10nl@aol.nl",
        "jjj10one@onerp.dk",
        "jjj10orange@orange.fr",
        "jjj10orange@orangerp.es",
        "jjj10redifmail@rediffmail.com",
        "jjj10rp3@aol.co.uk",
        "jjj10sapo@sapo.pt",
        "jjj10tele2@tele2.se",
        "jjj10Telefonica@iDisc.e.telefonica.net",
        "jjj10tiscal@tiscali.it",
        "jjj10tutopia@tutopia.com",
        "jjj10yah@yahoo.com.sg",
        "jjj10yhhk@yahoo.com.hk",
        "jjj10yho@yahoo.com.au",
        "jjj10yindi@yahoo.co.in",
        "jjj111rp@charter.net",
        "jjj11yah@yahoo.com.cn",
        "jjj325amv@21cn.com",
        "jjj325amv@tom.com",
        "jjj4likos@lycos.com",
        "jjj4sify@sify.com",
        "jjj4xiteB@excite.com",
        "jjj5telu@telus.net",
        "jjj@jjjukorange.orangehome.co.uk",
        "jjjten.BELGACOM@belgacom.net",
        "jjjvrzn@verizon.net",
        "jjr19@comcast.net",
        "jklecircle@t-online.de",
        "joannewbarron@hotmail.com",
        "joe.dorsey@sbcglobal.net",
        "joe12strummer@globetrotter.net",
        "john09kozak@rogers.com",
        "john49mayall@globetrotter.net",
        "john67jordan@globetrotter.net",
        "johnny84haliday@sympatico.ca",
        "johnvasily@usa.net",
        "joietom.bartel@comcast.net",
        "josanrusso@bellsouth.net",
        "joy_ferro@sbcglobal.net",
        "jpmarino67@optonline.com",
        "jpmoore14@netzero.com",
        "judge_gwynne67@msn.com",
        "judy44grabowski@sympatico.ca",
        "julia.evershade@netzero.com",
        "jxdubois@aol.com",
        "karlie@karliesimpson.wanadoo.co.uk",
        "kathy76wise@primus.ca",
        "kendrew8taylor@cox.net",
        "kevinhurley1964@sbcglobal.net",
        "kilmerknight85@yahoo.com",
        "kilometers47davis@videotron.ca",
        "king5roland@btinternet.com",
        "kkk11bluewin@bluewin.ch",
        "kkk11hot@msn.com",
        "kkk11rp3@aol.co.uk",
        "kkk5sify@sify.com",
        "kkk5xiteB@excite.com",
        "kkk6telu@telus.net",
        "kkkk10shaw@shaw.ca",
        "kristian49gravenor@sympatico.ca",
        "lang9fristig@tiscali.de",
        "lapaglia8giardin@netscape.net",
        "larraine2@globetrotter.net",
        "laser_ray_victim32@yahoo.com",
        "laser_specialist85knox@hotmail.com",
        "laura88avtin@sympatico.ca",
        "laurent55paul@cgocable.ca",
        "lavonne3louse@yahoo.co.uk",
        "lavonne4louse@gmx.de",
        "lavonne4louse@yahoo.es",
        "lazlo_holyfeldd22@yahoo.com",
        "lemon9buster@tiscali.co.uk",
        "lionel303hutz@nyc.rr.com",
        "llan798jensen@rogers.com",
        "lll12neuf@neuf.fr",
        "lll12orange@orangerp.es",
        "lll12skynet@sky.cz",
        "lll12Telefonica@iDisc.e.telefonica.net",
        "lllvrzn@verizon.net",
        "lorraine99johnson@rogers.com",
        "louisgcarnagle@yahoo.com",
        "lowellcornell@usa.net",
        "lucht100a@aol.de",
        "Lucht101a@aol.de",
        "Lucht102a@aol.de",
        "lucht103a@aol.de",
        "lucht104a@aol.de",
        "lucht105a@aol.de",
        "lucht106a@aol.de",
        "lucht107a@aol.de",
        "lucht108a@aol.de",
        "lucht109a@aol.de",
        "lucht110a@aol.de",
        "lucy.whitmore3@gmail.com",
        "lyndaweismeir@usa.net",
        "lynn87mcglaughlin@sympatico.ca",
        "mabakes@sbcglobal.net",
        "maratwingordon@optonline.com",
        "mariimak@usa.net",
        "mariosoldati@cs.com",
        "marlin.whitmore4@gmail.com",
        "marshall_winn@netzero.com",
        "martin75davison@aol.com",
        "matts49naslund@sympatico.ca",
        "merche56delgado@aol.com",
        "mgcrabtree@sbcglobal.net",
        "michael.binkley@hotmail.co.uk",
        "michaelalbert11014@msn.com",
        "michellemeyrink90@hotmail.com",
        "mick56jones@sympatico.ca",
        "midori44mannix@sympatico.ca",
        "mike.reedy1@netzero.com",
        "miles74davis@sympatico.ca",
        "milo.bloom@hotmail.co.uk",
        "miym31@dial.pipex.com",
        "miym33@dial.pipex.com",
        "miym37@dial.pipex.com",
        "miym39@dial.pipex.com",
        "mmm03hana@hanafos.com",
        "mmm111rp@charter.net",
        "mmm1asi@aol.com",
        "mmm1asi@bellsouth.net",
        "mmm1asi@comcast.net",
        "mmm1asi@cox.net",
        "mmm1asi@earthlink.net",
        "mmm1asi@gmail.com",
        "mmm1asi@hotmail.com",
        "mmm1asi@mac.com",
        "mmm1asi@mail.com",
        "mmm1asi@msn.com",
        "mmm1asi@netzero.com",
        "mmm1asi@optonline.com",
        "mmm1asi@usa.net",
        "mmm1asi@worldnet.att.net",
        "mmm1asi@yahoo.com",
        "mmm2asi@comcast.net",
        "mmm326amv@21cn.com",
        "mmm326amv@tom.com",
        "mmm3asi@cs.com",
        "mmm4asi@aol.com",
        "mmm517asi@biz.rr.com",
        "mmm5asi@netscape.net",
        "mmm5likos@lycos.com",
        "mmm6eir@eircom.net",
        "mmm6xiteB@excite.com",
        "moe59ron@videotron.ca",
        "monalisa_tomei30@msn.com",
        "montedodd@cs.com",
        "monterosso6_7cafeteria@hotmail.com",
        "MsKiki9Douveas@netscape.net",
        "n_solomon_1534@msn.com",
        "nadinevix@earthlink.net",
        "nate@natearmstrong.wanadoo.co.uk",
        "ndeforestiii@aol.com",
        "nnn14neuf@neuf.fr",
        "nnn327amv@21cn.com",
        "nnn327amv@tom.com",
        "nnnvrzn@verizon.net",
        "nnok04@hanmail.net",
        "norahmuldoon@mac.com",
        "nwokorie2chukie@terra.es",
        "ok1doc@club-internet.fr",
        "oldlady_gould44@yahoo.com",
        "oliver.wendell.jones@hotmail.co.uk",
        "ooo328amv@21cn.com",
        "ooo328amv@tom.com",
        "opus.penguin@hotmail.co.uk",
        "owen65baker@aol.com",
        "p.galvin42@comcast.net",
        "palepoi3anton@terra.es",
        "pati3hanson@yahoo.co.uk",
        "paul12franklin@aol.com",
        "paul45edwards@aol.com",
        "paul93norris@cgocable.ca",
        "paulhutchings303@aol.com",
        "pendleton_4gibbons@msn.com",
        "pennybaker3@earthlink.net",
        "per23karefelt@rogers.com",
        "peter_likidis720@hotmail.com",
        "pic2poc@wanadoo.fr",
        "pierce4terry@terra.es",
        "pieroportalupi@cs.com",
        "pinkard5mike@terra.es",
        "plummer6jake@terra.es",
        "plus3fou@club-internet.fr",
        "poft_pat99@earthlink.net",
        "pope7monsanto@terra.es",
        "portnoy@hotmail.co.uk",
        "ppp111rp@charter.net",
        "ppp16neuf@neuf.fr",
        "ppp16skynet@sky.cz",
        "ppp329amv@21cn.com",
        "ppp329amv@tom.com",
        "ppp7likos@lycos.com",
        "ppp7xiteB@excite.com",
        "ppp8yahmx@yahoo.com.mx",
        "princess5vespa@btinternet.com",
        "pryce8trevor@terra.es",
        "ptulley@earthlink.net",
        "publicist48_eisenberg@hotmail.com",
        "putzier9jeb@terra.es",
        "qmpartseed9@netscape.net",
        "qqq17aliceadsl@aliceadsl.fr",
        "qqq17bol@bol.com.br",
        "qqq17caramail@caramail.com",
        "qqq17fullzero@fullzero.com.ar",
        "qqq17igq@ig.com.br",
        "qqq17mail@mail.ru",
        "qqq17mipunto@mipunto.com",
        "qqq17netfin@returnpath.hk",
        "qqq17one@onerp.dk",
        "qqq17orange@orange.fr",
        "qqq17sapo@sapo.pt",
        "qqq17tel@telus.net",
        "qqq17tele2@tele2.se",
        "qqq17telmex@prodigy.net.mx",
        "qqq17tutopia@tutopia.com",
        "qqq17vsnl@vsnl.net",
        "qqq17yah@yahoo.com.sg",
        "qqq17yhhk@yahoo.com.hk",
        "qqq17yho@yahoo.com.au",
        "qqq17yindi@yahoo.co.in",
        "qqq18yah@yahoo.com.cn",
        "qqqseventeen.BELGACOM@belgacom.net",
        "R.Path1@gmx.at",
        "R.Path@gmx.at",
        "ralph7tresvant@tiscali.co.uk",
        "ras0cou@yahoo.fr",
        "rat3mort@wanadoo.fr",
        "razatos12_porter@hotmail.com",
        "rebecca53wate@primus.ca",
        "reggie_dunlop77@comcast.net",
        "renteria1d@terra.es",
        "res6t8vo@verizon.net",
        "retpath@arcor.de",
        "return.path002@ntlworld.com",
        "return.path004@ntlworld.com",
        "return.path006@ntlworld.com",
        "return.path008@ntlworld.com",
        "return.path010@ntlworld.com",
        "return.path017@ntlworld.com",
        "return.path019@ntlworld.com",
        "return.path021@ntlworld.com",
        "return.path023@ntlworld.com",
        "return.path025@ntlworld.com",
        "Return.Path1@gmx.at",
        "Return.Path@gmx.at",
        "Return1@gmx.at",
        "Return@gmx.at",
        "ReturnPath1@gmx.at",
        "returnpath400@aol.com",
        "ReturnPath@gmx.at",
        "rich11robert@cgocable.ca",
        "richardchew@bellsouth.net",
        "rita4muraz@gmx.de",
        "rita4muraz@yahoo.es",
        "rita6muraz@yahoo.co.uk",
        "robbins33raymond@cox.net",
        "robertprescott@worldnet.att.net",
        "robinstober@worldnet.att.net",
        "robjauregui22@aol.com",
        "roger77ramjet@videotron.ca",
        "roman3aykroyd@optonline.com",
        "rose7plummer@netscape.net",
        "roy53willco@primus.ca",
        "RPath1@gmx.at",
        "RPath@gmx.at",
        "rrr18goon@goo.jp",
        "rrr18neuf@neuf.fr",
        "rrr18nl@aol.nl",
        "rrr18tiscal@tiscali.it",
        "rrr8likos@lycos.com",
        "rrr8xiteB@excite.com",
        "rrr9yahmx@yahoo.com.mx",
        "rrrr17shaw@shaw.ca",
        "rrrvrzn@verizon.net",
        "rstout116@msn.com",
        "sallycato@mac.com",
        "sandymmeredith@cs.com",
        "sara@sprimus6.wanadoo.co.uk",
        "schaff1hausen@freesurf.ch",
        "science_fairstudent99@yahoo.com",
        "serina5miller@tiscali.co.uk",
        "seymour374skinner@nyc.rr.com",
        "sheila3e@tiscali.co.uk",
        "sherrynugil@hotmail.com",
        "shortordercook9@btinternet.com",
        "shuttle_peralta12@yahoo.com",
        "simon76johnson@rogers.com",
        "simpson99neckbrace@msn.com",
        "sirreturnpath1@smartemail.co.uk",
        "sirreturnpath2@smartemail.co.uk",
        "skate_tangen32@earthlink.net",
        "snapjap5don@terra.es",
        "spiro_23ulrich@hotmail.com",
        "sss10yahmx@yahoo.com.mx",
        "sss11bigp@bigpond.com",
        "sss11ozemail@ozemail.com.au",
        "sss19aliceadsl@aliceadsl.fr",
        "sss19bol@bol.com.br",
        "sss19caramail@caramail.com",
        "sss19fullzero@fullzero.com.ar",
        "sss19mail@mail.ru",
        "sss19mipunto@mipunto.com",
        "sss19netfin@returnpath.hk",
        "sss19one@onerp.dk",
        "sss19orange@orange.fr",
        "sss19sapo@sapo.pt",
        "sss19tel@telus.net",
        "sss19tele2@tele2.se",
        "sss19telmex@prodigy.net.mx",
        "sss19tutopia@tutopia.com",
        "sss19yah@yahoo.com.sg",
        "sss19yhhk@yahoo.com.hk",
        "sss19yho@yahoo.com.au",
        "sss19yindi@yahoo.co.in",
        "sss20yah@yahoo.com.cn",
        "sss@sssukorange.orangehome.co.uk",
        "sssnineteen.BELGACOM@belgacom.net",
        "stephen.mceveety@netzero.com",
        "stephen12elson@aol.com",
        "steve_dallas@hotmail.co.uk",
        "stillo.ashmore@cox.net",
        "stober_waitress912@hotmail.com",
        "stuart2mackenzie@netscape.net",
        "susanfdecker@worldnet.att.net",
        "sweigart_gate44@hotmail.com",
        "syddutton@netzero.com",
        "t_atlee@msn.com",
        "tammy1jack@yahoo.co.uk",
        "tammy3jack@club-internet.fr",
        "tammy3jack@yahoo.es",
        "tanner06@tanner06.wanadoo.co.uk",
        "tarquingotch@aol.com",
        "taylor573veatch@biz.rr.com",
        "tbraun2@dc.rr.com",
        "tensecond.tom5@gmail.com",
        "testaddress@smartemail.co.uk",
        "tim2piere@yahoo.co.uk",
        "tim76keene@rogers.com",
        "tim_mccracken77@comcast.net",
        "tina44puente@cgocable.ca",
        "tip7top@free.fr",
        "tisc0null@tiscali.de",
        "tisc1eins@tiscali.de",
        "tisc2zwei@tiscali.de",
        "tisc3drei@tiscali.de",
        "tisc4vier@tiscali.de",
        "tisc5funf@tiscali.de",
        "tisc6sechs@tiscali.de",
        "tisc7sieben@tiscali.de",
        "tisc8acht@tiscali.de",
        "tisc9neun@tiscali.de",
        "tom22rolfe@cgocable.ca",
        "top2cool@free.fr",
        "trotter5_lane@msn.com",
        "ttangen@bellsouth.net",
        "tthan06@hanmail.net",
        "ttt111rp@charter.net",
        "ttt11yahmx@yahoo.com.mx",
        "ttt20goon@goo.jp",
        "ttt20neuf@neuf.fr",
        "ttt20skynet@sky.cz",
        "ttt20tel@telus.net",
        "ttt20tiscal@tiscali.it",
        "ttt9likos@lycos.com",
        "ttt9xiteB@excite.com",
        "tttt19shaw@shaw.ca",
        "ulla3martin@yahoo.co.uk",
        "uuu10xiteB@excite.com",
        "uuu111rp@charter.net",
        "uuu12yahmx@yahoo.com.mx",
        "uuu21aliceadsl@aliceadsl.fr",
        "uuu21bol@bol.com.br",
        "uuu21caramail@caramail.com",
        "uuu21fullzero@fullzero.com.ar",
        "uuu21igu@ig.com.br",
        "uuu21mail@mail.ru",
        "uuu21mipunto@mipunto.com",
        "uuu21netfin@returnpath.hk",
        "uuu21one@onerp.dk",
        "uuu21orange@orange.fr",
        "uuu21sapo@sapo.pt",
        "uuu21tele2@tele2.se",
        "uuu21telmex@prodigy.net.mx",
        "uuu21tutopia@tutopia.com",
        "uuu21vsnl@vsnl.net",
        "uuu21yah@yahoo.com.sg",
        "uuu21yhhk@yahoo.com.hk",
        "uuu21yho@yahoo.com.au",
        "uuu21yindi@yahoo.co.in",
        "uuu22yah@yahoo.com.cn",
        "uuu@uuuukorange.orangehome.co.uk",
        "uuutwentyone.BELGACOM@belgacom.net",
        "uuuvrzn@verizon.net",
        "venetia73penna@cgocable.ca",
        "vera5fredrick@club-internet.fr",
        "vera5fredrick@yahoo.es",
        "vera7fredrick@yahoo.co.uk",
        "vilmos@bellsouth.net",
        "vince523lafleur@biz.rr.com",
        "vincent77_pesci@msn.com",
        "vor9her@yahoo.de",
        "vvhan07@hanmail.net",
        "vvv10eir@eircom.net",
        "vvv22goon@goo.jp",
        "vvv22nl@aol.nl",
        "vvv22tiscal@tiscali.it",
        "vvvv21shaw@shaw.ca",
        "wallyprosky88@optonline.com",
        "wan4quatre@wanadoo.fr",
        "wan5cinq@wanadoo.fr",
        "wan6six@wanadoo.fr",
        "wan7sept@wanadoo.fr",
        "wan8huit@wanadoo.fr",
        "wan9neuf@wanadoo.fr",
        "washburntaxidriver@gmail.com",
        "web1eins@web.de",
        "web2zwei@web.de",
        "web3drei@web.de",
        "web4vier@web.de",
        "web5funf@web.de",
        "web6sechs@web.de",
        "web7sieben@web.de",
        "web8acht@web.de",
        "web9neun@web.de",
        "wiatress12keller@aol.com",
        "wilber_rebhorn4@msn.com",
        "will33powloski@cgocable.ca",
        "willette.joanne@earthlink.net",
        "willie.jocko@gmail.com",
        "willyknox@sbcglobal.net",
        "winter8thur@freesurf.ch",
        "www10likos@lycos.com",
        "www13bigp@bigpond.com",
        "www13ozemail@ozemail.com.au",
        "www23telmex@prodigy.net.mx",
        "www23yho@yahoo.com.au",
        "www24yah@yahoo.com.cn",
        "wwwvrzn@verizon.net",
        "xav5stov@free.fr",
        "xxhan08@hanmail.net",
        "xxx12eir@eircom.net",
        "xxx23aliceadsl@aliceadsl.fr",
        "xxx23bol@bol.com.br",
        "xxx23caramail@caramail.com",
        "xxx23fullzero@fullzero.com.ar",
        "xxx23igx@ig.com.br",
        "xxx23mail@mail.ru",
        "xxx23mipunto@mipunto.com",
        "xxx23netfin@returnpath.hk",
        "xxx23one@onerp.dk",
        "xxx23orange@orange.fr",
        "xxx23sapo@sapo.pt",
        "xxx23tele2@tele2.se",
        "xxx23tutopia@tutopia.com",
        "xxx23yah@yahoo.com.sg",
        "xxx23yhhk@yahoo.com.hk",
        "xxx23yindi@yahoo.co.in",
        "xxx24nl@aol.nl",
        "xxx@xxxukorange.orangehome.co.uk",
        "xxxtwentythree.BELGACOM@belgacom.net",
        "ya0null@yahoo.de",
        "ya1eins@yahoo.de",
        "ya2zwei@yahoo.de",
        "ya3drei@yahoo.de",
        "ya4vier@yahoo.de",
        "ya5cinq@yahoo.fr",
        "ya5funf@yahoo.de",
        "ya6sechs@yahoo.de",
        "ya6six@yahoo.fr",
        "ya7sept@yahoo.fr",
        "ya7sieben@yahoo.de",
        "ya8acht@yahoo.de",
        "ya8huit@yahoo.fr",
        "ya9neuf@yahoo.fr",
        "yuki.shimoda@mac.com",
        "yyy24goon@goo.jp",
        "yyy24skynet@sky.cz",
        "yyy24tiscal@tiscali.it",
        "yyy24vsnl@vsnl.net",
        "yyy25telmex@prodigy.net.mx",
        "yyy25yho@yahoo.com.au",
        "yyy26yah@yahoo.com.cn",
        "yyyy23shaw@shaw.ca",
        "zeta_nicholson4@cox.net",
        "zoot98alors@videotron.ca",
        "zsigmond@bellsouth.net",
        "zsigmond_chew33@hotmail.com",
        "zu7rich@freesurf.ch",
        "zzz14eir@eircom.net",
        "zzz15bigp@bigpond.com",
        "zzz15ozemail@ozemail.com.au",
        "zzz1asi@aol.com",
        "zzz1asi@bellsouth.net",
        "zzz1asi@comcast.net",
        "zzz1asi@cox.net",
        "zzz1asi@earthlink.net",
        "zzz1asi@gmail.com",
        "zzz1asi@hotmail.com",
        "zzz1asi@mac.com",
        "zzz1asi@mail.com",
        "zzz1asi@msn.com",
        "zzz1asi@netzero.com",
        "zzz1asi@optonline.com",
        "zzz1asi@usa.net",
        "zzz1asi@worldnet.att.net",
        "zzz1asi@yahoo.com",
        "zzz25aliceadsl@aliceadsl.fr",
        "zzz25bol@bol.com.br",
        "zzz25caramail@caramail.com",
        "zzz25fullzero@fullzero.com.ar",
        "zzz25igz@ig.com.br",
        "zzz25mail@mail.ru",
        "zzz25mipunto@mipunto.com",
        "zzz25netfin@returnpath.hk",
        "zzz25one@onerp.dk",
        "zzz25orange@orange.fr",
        "zzz25sapo@sapo.pt",
        "zzz25tele2@tele2.se",
        "zzz25tutopia@tutopia.com",
        "zzz25yah@yahoo.com.sg",
        "zzz25yhhk@yahoo.com.hk",
        "zzz25yindi@yahoo.co.in",
        "zzz2asi@comcast.net",
        "zzz3asi@cs.com",
        "zzz4asi@aol.com",
        "zzz593asi@biz.rr.com",
        "zzz5asi@netscape.net",
        "zzz@zzzukorange.orangehome.co.uk",
        "zzztwentyfive.BELGACOM@belgacom.net",
        "zzzz24shaw@shaw.ca",
    };
}
