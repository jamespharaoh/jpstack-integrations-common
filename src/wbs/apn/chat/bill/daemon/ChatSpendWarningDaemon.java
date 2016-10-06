package wbs.apn.chat.bill.daemon;

import static wbs.utils.string.StringUtils.stringFormat;

import java.util.Collections;
import java.util.List;

import com.google.common.base.Optional;

import lombok.Cleanup;
import lombok.NonNull;
import lombok.extern.log4j.Log4j;

import org.joda.time.Duration;

import wbs.apn.chat.contact.logic.ChatSendLogic;
import wbs.apn.chat.contact.logic.ChatSendLogic.TemplateMissing;
import wbs.apn.chat.scheme.model.ChatSchemeChargesRec;
import wbs.apn.chat.scheme.model.ChatSchemeRec;
import wbs.apn.chat.user.core.model.ChatUserObjectHelper;
import wbs.apn.chat.user.core.model.ChatUserRec;
import wbs.framework.component.annotations.SingletonComponent;
import wbs.framework.component.annotations.SingletonDependency;
import wbs.framework.database.Database;
import wbs.framework.database.Transaction;
import wbs.framework.exception.ExceptionLogger;
import wbs.framework.exception.GenericExceptionResolution;
import wbs.framework.object.ObjectManager;
import wbs.platform.daemon.SleepingDaemonService;

@Log4j
@SingletonComponent ("chatSpendWarningDaemon")
public
class ChatSpendWarningDaemon
	extends SleepingDaemonService {

	// singleton dependencies

	@SingletonDependency
	ChatSendLogic chatSendLogic;

	@SingletonDependency
	ChatUserObjectHelper chatUserHelper;

	@SingletonDependency
	Database database;

	@SingletonDependency
	ExceptionLogger exceptionLogger;

	@SingletonDependency
	ObjectManager objectManager;

	// details

	@Override
	protected
	String getThreadName () {
		return "ChatSpendWarn";
	}

	@Override
	protected
	Duration getSleepDuration () {

		return Duration.standardSeconds (
			60);

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
				"ChatSpendWarningDaemon.runOnce ()",
				this);

		List<ChatUserRec> chatUsers =
			chatUserHelper.findWantingWarning ();

		transaction.close ();

		for (
			ChatUserRec chatUser
				: chatUsers
		) {

			try {

				doUser (
					chatUser.getId ());

			} catch (Exception exception) {

				exceptionLogger.logThrowable (
					"daemon",
					"ChatSpendWarningDaemon",
					exception,
					Optional.absent (),
					GenericExceptionResolution.tryAgainLater);

			}

		}

	}

	void doUser (
			@NonNull Long chatUserId) {

		@Cleanup
		Transaction transaction =
			database.beginReadWrite (
				"ChatSpendWarningDaemon.doUser (chatUserId)",
				this);

		ChatUserRec chatUser =
			chatUserHelper.findRequired (
				chatUserId);

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
			Optional.<Long>absent (),
			chatUser.getNumSpendWarnings () == 0
				? "spend_warning_1"
				: "spend_warning_2",
			TemplateMissing.error,
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