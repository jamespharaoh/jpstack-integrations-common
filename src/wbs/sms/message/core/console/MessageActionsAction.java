package wbs.sms.message.core.console;

import static wbs.framework.utils.etc.Misc.isNotNull;
import static wbs.framework.utils.etc.Misc.notEqual;
import static wbs.framework.utils.etc.Misc.notIn;

import javax.inject.Inject;
import javax.servlet.ServletException;

import lombok.Cleanup;
import lombok.NonNull;

import wbs.console.action.ConsoleAction;
import wbs.console.request.ConsoleRequestContext;
import wbs.framework.application.annotations.PrototypeComponent;
import wbs.framework.database.Database;
import wbs.framework.database.Transaction;
import wbs.framework.web.Responder;
import wbs.platform.event.logic.EventLogic;
import wbs.platform.user.console.UserConsoleHelper;
import wbs.platform.user.model.UserRec;
import wbs.sms.message.core.logic.MessageLogic;
import wbs.sms.message.core.model.MessageDirection;
import wbs.sms.message.core.model.MessageRec;
import wbs.sms.message.core.model.MessageStatus;
import wbs.sms.message.outbox.logic.OutboxLogic;

@PrototypeComponent ("messageActionsAction")
public
class MessageActionsAction
	extends ConsoleAction {

	// dependencies

	@Inject
	Database database;

	@Inject
	EventLogic eventLogic;

	@Inject
	MessageConsoleHelper messageHelper;

	@Inject
	MessageLogic messageLogic;

	@Inject
	OutboxLogic outboxLogic;

	@Inject
	ConsoleRequestContext requestContext;

	@Inject
	UserConsoleHelper userHelper;

	// state

	MessageRec message;
	UserRec myUser;

	// details

	@Override
	protected
	Responder backupResponder () {

		return responder (
			"messageActionsResponder");

	}

	// implementation

	@Override
	protected
	Responder goReal ()
		throws ServletException {

		// begin transaction

		@Cleanup
		Transaction transaction =
			database.beginReadWrite (
				this);

		// load data

		myUser =
			userHelper.find (
				requestContext.userId ());

		message =
			messageHelper.find (
				requestContext.stuffInt (
					"messageId"));

		// hand off to appropriate method

		if (
			isNotNull (
				requestContext.parameter (
					"manuallyUndeliver"))
		) {

			return manuallyUndeliver (
				transaction);

		} else if (
			isNotNull (
				requestContext.parameter (
					"manuallyDeliver"))
		) {

			return manuallyDeliver (
				transaction);

		} else if (
			isNotNull (
				requestContext.parameter (
					"manuallyUnhold"))
		) {

			return manuallyUnhold (
				transaction);

		} else {

			throw new RuntimeException ();

		}

	}

	private
	Responder manuallyUndeliver (
			@NonNull Transaction transaction) {

		if (
			notEqual (
				message.getDirection (),
				MessageDirection.out)
		) {
			throw new RuntimeException ();
		}

		if (
			notIn (
				message.getStatus (),
				MessageStatus.sent,
				MessageStatus.submitted,
				MessageStatus.delivered)
		) {

			requestContext.addError (
				"Message in invalid state for this operation");

			return null;

		}

		messageLogic.messageStatus (
			message,
			MessageStatus.manuallyUndelivered);

		eventLogic.createEvent (
			"message_manually_undelivered",
			myUser,
			message);

		transaction.commit ();

		requestContext.addNotice (
			"Message manually undelivered");

		return null;

	}

	private
	Responder manuallyDeliver (
			@NonNull Transaction transaction) {

		if (
			notEqual (
				message.getDirection (),
				MessageDirection.out)
		) {
			throw new RuntimeException ();
		}

		if (
			notIn (
				message.getStatus (),
				MessageStatus.undelivered,
				MessageStatus.reportTimedOut,
				MessageStatus.manuallyUndelivered)
		) {

			requestContext.addError (
				"Message in invalid state for this operation");

			return null;

		}

		messageLogic.messageStatus (
			message,
			MessageStatus.manuallyDelivered);

		eventLogic.createEvent (
			"message_manually_delivered",
			myUser,
			message);

		transaction.commit ();

		requestContext.addNotice (
			"Message manually undelivered");

		return null;

	}

	private
	Responder manuallyUnhold (
			@NonNull Transaction transaction) {

		if (
			notEqual (
				message.getStatus (),
				MessageStatus.held)
		) {

			requestContext.addError (
				"Message in invalid state for this operation");

			return null;

		}

		outboxLogic.unholdMessage (
			message);

		eventLogic.createEvent (
			"message_manually_unheld",
			myUser,
			message);

		transaction.commit ();

		requestContext.addNotice (
			"Message manually unheld");

		return null;

	}

}
