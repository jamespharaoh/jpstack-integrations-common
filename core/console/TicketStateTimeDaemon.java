package wbs.services.ticket.core.console;

import static wbs.framework.utils.etc.Misc.stringFormat;

import java.util.Collections;
import java.util.List;

import javax.inject.Inject;

import com.google.common.base.Optional;

import lombok.Cleanup;
import lombok.extern.log4j.Log4j;
import wbs.clients.apn.chat.contact.logic.ChatSendLogic;
import wbs.clients.apn.chat.scheme.model.ChatSchemeChargesRec;
import wbs.clients.apn.chat.scheme.model.ChatSchemeRec;
import wbs.clients.apn.chat.user.core.model.ChatUserObjectHelper;
import wbs.clients.apn.chat.user.core.model.ChatUserRec;
import wbs.framework.application.annotations.SingletonComponent;
import wbs.framework.database.Database;
import wbs.framework.database.Transaction;
import wbs.framework.object.ObjectManager;
import wbs.platform.daemon.SleepingDaemonService;
import wbs.platform.exception.logic.ExceptionLogic;

@Log4j
@SingletonComponent ("ticketStateTimeDaemon")
public 
class TicketStateTimeDaemon 
	extends SleepingDaemonService {
	
	// dependencies

	@Inject
	ChatSendLogic chatSendLogic;

	@Inject
	ChatUserObjectHelper chatUserHelper;

	@Inject
	Database database;

	@Inject
	ExceptionLogic exceptionLogic;

	@Inject
	ObjectManager objectManager;


	// details

	@Override
	protected
	String getThreadName () {
		return "ChatSpendWarn";
	}

	@Override
	protected
	int getDelayMs () {
		return 60 * 1000;
	}

	@Override
	protected
	String generalErrorSource () {
		return "chat spend warning daemon";
	}

	@Override
	protected
	String generalErrorSummary () {
		return "error sending chat spend warnings in background";
	}

	// implementation

	@Override
	protected
	void runOnce () {

		log.debug (
			"Looking for users to send spend warning to");

		// get a list of users who need a spend warning

		@Cleanup
		Transaction transaction =
			database.beginReadOnly (
				this);

		List<ChatUserRec> chatUsers =
			chatUserHelper.findWantingWarning ();

		transaction.close ();

		for (ChatUserRec chatUser
				: chatUsers) {

			try {

				doUser (
					chatUser.getId ());

			} catch (Exception exception) {

				exceptionLogic.logThrowable (
					"daemon",
					"ChatSpendWarningDaemon",
					exception,
					Optional.<Integer>absent (),
					false);

			}

		}

	}

	void doUser (
			int chatUserId) {

		@Cleanup
		Transaction transaction =
			database.beginReadWrite (
				this);

		ChatUserRec chatUser =
			chatUserHelper.find (chatUserId);

		ChatSchemeRec chatScheme =
			chatUser.getChatScheme ();

		ChatSchemeChargesRec chatSchemeCharges =
			chatScheme.getCharges ();

		// check warning is due

		if (
			chatUser.getValueSinceWarning ()
				< chatSchemeCharges.getSpendWarningEvery ()
		) {
			return;
		}

		// log message

		log.info (
			stringFormat (
				"Sending warning to user %s",
				objectManager.objectPathMini (
					chatUser)));

		// send message

		chatSendLogic.sendSystemRbFree (
			chatUser,
			Optional.<Integer>absent (),
			chatUser.getNumSpendWarnings () == 0
				? "spend_warning_1"
				: "spend_warning_2",
			Collections.<String,String>emptyMap ());

		// update user

		chatUser

			.setValueSinceWarning (
				+ chatUser.getValueSinceWarning ()
				- chatSchemeCharges.getSpendWarningEvery ())

			.setNumSpendWarnings (
				+ chatUser.getNumSpendWarnings ()
				+ 1);

		// commit and return

		transaction.commit ();

	}

}
