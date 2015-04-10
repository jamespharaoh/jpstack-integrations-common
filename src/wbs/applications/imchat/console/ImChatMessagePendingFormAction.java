package wbs.applications.imchat.console;

import static wbs.framework.utils.etc.Misc.equal;
import static wbs.framework.utils.etc.Misc.ifNull;
import static wbs.framework.utils.etc.Misc.stringFormat;
import static wbs.framework.utils.etc.Misc.toInteger;

import javax.inject.Inject;

import lombok.Cleanup;
import wbs.applications.imchat.model.ImChatConversationRec;
import wbs.applications.imchat.model.ImChatCustomerRec;
import wbs.applications.imchat.model.ImChatMessageRec;
import wbs.applications.imchat.model.ImChatRec;
import wbs.applications.imchat.model.ImChatTemplateRec;
import wbs.framework.application.annotations.PrototypeComponent;
import wbs.framework.database.Database;
import wbs.framework.database.Transaction;
import wbs.framework.web.Responder;
import wbs.platform.console.action.ConsoleAction;
import wbs.platform.console.request.ConsoleRequestContext;
import wbs.platform.currency.logic.CurrencyLogic;
import wbs.platform.queue.logic.QueueLogic;
import wbs.platform.user.model.UserObjectHelper;
import wbs.platform.user.model.UserRec;

@PrototypeComponent ("imChatMessagePendingFormAction")
public
class ImChatMessagePendingFormAction
	extends ConsoleAction {

	// dependencies

	@Inject
	CurrencyLogic currencyLogic;

	@Inject
	Database database;

	@Inject
	ImChatMessageConsoleHelper imChatMessageHelper;

	@Inject
	ImChatTemplateConsoleHelper imChatTemplateHelper;

	@Inject
	QueueLogic queueLogic;

	@Inject
	ConsoleRequestContext requestContext;

	@Inject
	UserObjectHelper userHelper;

	// details

	@Override
	public
	Responder backupResponder () {

		return responder (
			"imChatMessagePendingFormResponder");

	}

	// implementation

	@Override
	public
	Responder goReal () {

		// begin transaction

		@Cleanup
		Transaction transaction =
			database.beginReadWrite (
				this);

		// find user

		UserRec myUser =
			userHelper.find (
				requestContext.userId ());

		// find message

		ImChatMessageRec customerMessage =
			imChatMessageHelper.find (
				requestContext.stuffInt (
					"imChatMessageId"));

		ImChatConversationRec conversation =
			customerMessage.getImChatConversation ();

		ImChatCustomerRec customer =
			conversation.getImChatCustomer ();

		ImChatRec imChat =
			customer.getImChat ();

		// sanity check

		if (customerMessage.getQueueItem () == null)
			throw new RuntimeException ();

		// select template

		String templateString =
			requestContext.parameter ("template");

		boolean bill;
		int minLength;
		int maxLength;

		ImChatTemplateRec template;

		String messageText;

		if (equal (templateString, "bill")) {

			bill = true;
			template = null;

			messageText =
				requestContext.parameter ("message-bill");

			minLength =
				imChat.getBillMessageMinChars ();

			maxLength =
				imChat.getBillMessageMaxChars ();

		} else if (equal (templateString, "free")) {

			bill = false;
			template = null;

			messageText =
				requestContext.parameter ("message-free");

			minLength =
				imChat.getFreeMessageMinChars ();

			maxLength =
				imChat.getFreeMessageMaxChars ();

		} else {

			bill = false;

			template =
				imChatTemplateHelper.find (
					toInteger (templateString));

			if (template == null)
				throw new RuntimeException ();

			if (template.getImChat () != imChat)
				throw new RuntimeException ();

			messageText =
				template.getText ();

			minLength = 0;
			maxLength = Integer.MAX_VALUE;

		}

		// check message type

		if (
			bill
			&& customer.getBalance () < imChat.getMessageCost ()
		) {

			requestContext.addError (
				stringFormat (
					"Billed message pricse is %s, ",
					currencyLogic.formatText (
						imChat.getCurrency (),
						(long) imChat.getMessageCost ()),
					"but customer's balancer is only %s",
					currencyLogic.formatText (
						imChat.getCurrency (),
						(long) customer.getBalance ())));

		}

		// check length

		int messageLength =
			messageText.length ();

		if (messageLength < minLength) {

			requestContext.addError (
				stringFormat (
					"Message has %s ",
					messageLength,
					"characters, but the minimum is %s, ",
					minLength,
					"please add %s more",
					minLength - messageLength));

			return null;

		}

		if (messageLength > maxLength) {

			requestContext.addError (
				stringFormat (
					"Message has %s ",
					messageLength,
					"characters, but the maximum is %s, ",
					maxLength,
					"please remove %s",
					messageLength - maxLength));

			return null;

		}

		// create reply

		ImChatMessageRec operatorMessage =
			imChatMessageHelper.insert (
				new ImChatMessageRec ()

			.setImChatConversation (
				conversation)

			.setIndex (
				conversation.getNumMessages ())

			.setSenderUser (
				myUser)

			.setTimestamp (
				transaction.now ())

			.setMessageText (
				messageText)

			.setImChatTemplate (
				template)

			.setPartnerImChatMessage (
				customerMessage)

			.setPrice (
				bill
					? imChat.getMessageCost ()
					: null)

		);

		customerMessage

			.setPartnerImChatMessage (
				operatorMessage);

		// update conversation

		conversation

			.setNumMessages (
				conversation.getNumMessages () + 1);

		// update customer

		customer

			.setBalance (
				+ customer.getBalance ()
				- ifNull (operatorMessage.getPrice (), 0));

		// remove queue item

		queueLogic.processQueueItem (
			customerMessage.getQueueItem (),
			myUser);

		// done

		transaction.commit ();

		requestContext.addNotice (
			"Reply sent");

		// return

		return responder (
			"queueHomeResponder");

	}


}
