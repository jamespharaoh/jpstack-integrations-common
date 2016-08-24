package wbs.sms.message.outbox.logic;

import static wbs.framework.utils.etc.EnumUtils.enumEqualSafe;
import static wbs.framework.utils.etc.EnumUtils.enumInSafe;
import static wbs.framework.utils.etc.EnumUtils.enumNotEqualSafe;
import static wbs.framework.utils.etc.EnumUtils.enumNotInSafe;
import static wbs.framework.utils.etc.NullUtils.ifNull;
import static wbs.framework.utils.etc.OptionalUtils.optionalIsPresent;
import static wbs.framework.utils.etc.OptionalUtils.optionalOrNull;
import static wbs.framework.utils.etc.StringUtils.stringFormat;
import static wbs.framework.utils.etc.TimeUtils.earliest;

import java.util.List;

import javax.inject.Inject;

import org.joda.time.Duration;
import org.joda.time.Instant;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;

import lombok.NonNull;
import lombok.extern.log4j.Log4j;
import wbs.framework.application.annotations.SingletonComponent;
import wbs.framework.database.Database;
import wbs.framework.database.Transaction;
import wbs.platform.text.model.TextObjectHelper;
import wbs.platform.text.model.TextRec;
import wbs.sms.message.core.logic.SmsMessageLogic;
import wbs.sms.message.core.model.MessageDirection;
import wbs.sms.message.core.model.MessageExpiryObjectHelper;
import wbs.sms.message.core.model.MessageObjectHelper;
import wbs.sms.message.core.model.MessageRec;
import wbs.sms.message.core.model.MessageStatus;
import wbs.sms.message.core.model.MessageTypeRec;
import wbs.sms.message.outbox.model.FailedMessageObjectHelper;
import wbs.sms.message.outbox.model.OutboxDao;
import wbs.sms.message.outbox.model.OutboxObjectHelper;
import wbs.sms.message.outbox.model.OutboxRec;
import wbs.sms.message.outbox.model.SmsOutboxAttemptObjectHelper;
import wbs.sms.message.outbox.model.SmsOutboxAttemptRec;
import wbs.sms.message.outbox.model.SmsOutboxAttemptState;
import wbs.sms.route.core.model.RouteRec;

@Log4j
@SingletonComponent ("smsOutboxLogic")
public
class SmsOutboxLogicImplementation
	implements SmsOutboxLogic {

	// dependencies

	@Inject
	Database database;

	@Inject
	FailedMessageObjectHelper failedMessageHelper;

	@Inject
	MessageExpiryObjectHelper messageExpiryHelper;

	@Inject
	MessageObjectHelper messageHelper;

	@Inject
	SmsMessageLogic messageLogic;

	@Inject
	OutboxDao outboxDao;

	@Inject
	OutboxObjectHelper outboxHelper;

	@Inject
	SmsOutboxAttemptObjectHelper smsOutboxAttemptHelper;

	@Inject
	TextObjectHelper textHelper;

	// implementation

	@Override
	public
	MessageRec resendMessage (
			MessageRec old,
			RouteRec route,
			TextRec textRec,
			MessageTypeRec messageTypeRec) {

		Transaction transaction =
			database.currentTransaction ();

		if (textRec == null)
			textRec = old.getText ();

		if (messageTypeRec == null)
			messageTypeRec = old.getMessageType ();

		MessageRec message =
			messageHelper.insert (
				messageHelper.createInstance ()

			.setThreadId (
				old.getThreadId ())

			.setText (
				textRec) // old.getText ()

			.setNumFrom (
				old.getNumFrom ())

			.setNumTo (
				old.getNumTo ())

			.setDirection (
				MessageDirection.out)

			.setStatus (
				MessageStatus.pending)

			.setNumber (
				old.getNumber ())

			.setRoute (
				route) // old.getRoute ()

			.setService (
				old.getService ())

			.setNetwork (
				old.getNetwork ())

			.setBatch (
				old.getBatch ())

			.setCharge (
				old.getCharge ())

			.setAffiliate (
				old.getAffiliate ())

			.setCreatedTime (
				transaction.now ())

			.setDeliveryType (
				old.getDeliveryType ())

			.setRef (
				old.getRef ())

			.setSubjectText (
				old.getSubjectText ())

			.setMessageType (
				messageTypeRec)

			.setMedias (
				ImmutableList.copyOf (
					old.getMedias ()))

			.setTags (
				ImmutableSet.copyOf (
					old.getTags ()))

			.setNumAttempts (
				0l)

		);

		outboxHelper.insert (
			outboxHelper.createInstance ()

			.setMessage (
				message)

			.setRoute (
				message.getRoute ())

			.setCreatedTime (
				transaction.now ())

			.setRetryTime (
				transaction.now ())

			.setRemainingTries (
				message.getRoute ().getMaxTries ()));

		return message;

	}

	@Override
	public
	void unholdMessage (
			@NonNull MessageRec message) {

		Transaction transaction =
			database.currentTransaction ();

		if (message.getStatus () != MessageStatus.held) {

			throw new RuntimeException (
				stringFormat (
					"Trying to unhold message in state: %s",
					message.getStatus ()));

		}

		messageLogic.messageStatus (
			message,
			MessageStatus.pending);

		outboxHelper.insert (
			outboxHelper.createInstance ()

			.setMessage (
				message)

			.setRoute (
				message.getRoute ())

			.setCreatedTime (
				transaction.now ())

			.setRetryTime (
				transaction.now ())

			.setRemainingTries (
				message.getRoute ().getMaxTries ())

		);

	}

	@Override
	public
	void cancelMessage (
			MessageRec message) {

		// check message state

		if (
			enumEqualSafe (
				message.getStatus (),
				MessageStatus.pending)
		) {

			// lookup outbox

			OutboxRec outbox =
				outboxHelper.findRequired (
					message.getId ());

			// check message is not being sent

			if (outbox.getSending () != null) {

				throw new RuntimeException (
					"Message is being sent");

			}

			// cancel message

			messageLogic.messageStatus (
				message,
				MessageStatus.cancelled);

			// remove outbox

			outboxHelper.remove (
				outbox);

		} else if (
			enumEqualSafe (
				message.getStatus (),
				MessageStatus.held)
		) {

			// cancel message

			messageLogic.messageStatus (
				message,
				MessageStatus.cancelled);

		} else {

			throw new RuntimeException (
				"Message is not pending/held");

		}

	}

	@Override
	public
	OutboxRec claimNextMessage (
			@NonNull RouteRec route) {

		Transaction transaction =
			database.currentTransaction ();

		OutboxRec outbox =
			outboxHelper.findNext (
				transaction.now (),
				route);

		if (outbox == null)
			return null;

		outbox

			.setSending (
				transaction.now ())

			.setRemainingTries (
				outbox.getRemainingTries () != null
					? outbox.getRemainingTries () - 1
					: null);

		return outbox;

	}

	@Override
	public
	List <OutboxRec> claimNextMessages (
			@NonNull RouteRec route,
			@NonNull Long limit) {

		Transaction transaction =
			database.currentTransaction ();

		List <OutboxRec> outboxes =
			outboxDao.findNextLimit (
				transaction.now (),
				route,
				limit);

		for (
			OutboxRec outbox
				: outboxes
		) {

			outbox

				.setSending (
					transaction.now ());

			if (outbox.getRemainingTries () != null) {

				outbox

					.setRemainingTries (
						outbox.getRemainingTries () - 1);

			}

		}

		return outboxes;

	}

	@Override
	public
	void messageSuccess (
			@NonNull MessageRec message,
			@NonNull Optional<List<String>> otherIds) {

		Transaction transaction =
			database.currentTransaction ();

		log.debug ("outbox success id = " + message.getId ());

		OutboxRec outbox =
			outboxHelper.findRequired (
				message.getId ());

		if (

			enumNotEqualSafe (
				message.getStatus (),
				MessageStatus.pending)

			&& enumNotEqualSafe (
				message.getStatus (),
				MessageStatus.cancelled)

		) {

			throw new RuntimeException (
				stringFormat (
					"Invalid message status %s for message %d",
					message.getStatus ().toString (),
					message.getId ()));

		}

		if (outbox.getSending () == null)
			throw new RuntimeException (
				"Outbox not marked as sending!");

		outboxHelper.remove (
			outbox);

		messageLogic.messageStatus (
			message,
			MessageStatus.sent);

		if (
			optionalIsPresent (
				otherIds)
		) {

			message

				.setOtherId (
					otherIds.get ().get (0));

		}

		message

			.setProcessedTime (
				transaction.now ());

		// create expiry if appropriate

		RouteRec route =
			message.getRoute ();

		if (
			route.getDeliveryReports ()
			&& route.getExpirySecs () != null
		) {

			messageExpiryHelper.insert (
				messageExpiryHelper.createInstance ()

				.setMessage (
					message)

				.setExpiryTime (
					Instant.now ().plus (
						Duration.standardSeconds (
							route.getExpirySecs ())))

			);

		}

		database.flush ();

		// create multipart companions if appropriate

		if (
			optionalIsPresent (
				otherIds)
		) {

			otherIds.get ().stream ().skip (1).forEach (
				otherId ->
					messageHelper.insert (
						messageHelper.createInstance ()

				.setThreadId (
					message.getThreadId ())

				.setOtherId (
					otherId)

				.setText (
					textHelper.findOrCreate (
						stringFormat (
							"[multipart companion for %s]",
							message.getId ())))

				.setNumFrom (
					message.getNumFrom ())

				.setNumTo (
					message.getNumTo ())

				.setDirection (
					MessageDirection.out)

				.setNumber (
					message.getNumber ())

				.setCharge (
					message.getCharge ())

				.setMessageType (
					message.getMessageType ())

				.setRoute (
					message.getRoute ())

				.setService (
					message.getService ())

				.setNetwork (
					message.getNetwork ())

				.setBatch (
					message.getBatch ())

				.setAffiliate (
					message.getAffiliate ())

				.setStatus (
					MessageStatus.sent)

				.setCreatedTime (
					message.getCreatedTime ())

				.setProcessedTime (
					message.getProcessedTime ())

				.setNetworkTime (
					null)

			));

			// TODO expire multipart companions?

		}

	}

	@Override
	public
	void messageFailure (
			@NonNull MessageRec message,
			@NonNull String error,
			@NonNull FailureType failureType) {

		Transaction transaction =
			database.currentTransaction ();

		log.debug (
			"outbox failure id = " + message.getId ());

		OutboxRec outbox =
			outboxHelper.findRequired (
				message.getId ());

		if (
			enumNotInSafe (
				message.getStatus (),
				MessageStatus.pending,
				MessageStatus.cancelled)
		) {

			throw new RuntimeException (
				"Invalid message status");

		}

		if (outbox.getSending () == null) {

			throw new RuntimeException (
				"Outbox not marked as sending!");

		}

		if (failureType == FailureType.permanent) {

			outboxHelper.remove (
				outbox);

			messageLogic.messageStatus (
				message,
				MessageStatus.failed);

			message

				.setProcessedTime (
					transaction.now ());

			failedMessageHelper.insert (
				failedMessageHelper.createInstance ()

				.setMessage (
					message)

				.setError (
					error)

			);

		} else {

			outbox

				.setRetryTime (
					transaction.now ().plus (
						Duration.standardSeconds (
							outbox.getTries () * 10)))

				.setTries (
					outbox.getTries () + 1)

				.setDailyFailure (
					failureType == FailureType.daily)

				.setError (
					error)

				.setSending (
					null);

		}

	}

	@Override
	public
	void retryMessage (
			@NonNull MessageRec message) {

		Transaction transaction =
			database.currentTransaction ();

		if (
			enumNotEqualSafe (
				message.getDirection (),
				MessageDirection.out)
		) {

			throw new RuntimeException ();

		} else if (
			enumInSafe (
				message.getStatus (),
				MessageStatus.failed,
				MessageStatus.cancelled,
				MessageStatus.blacklisted)
		) {

			outboxHelper.insert (
				outboxHelper.createInstance ()

				.setMessage (
					message)

				.setRoute (
					message.getRoute ())

				.setCreatedTime (
					transaction.now ())

				.setRetryTime (
					transaction.now ())

				.setRemainingTries (
					message.getRoute ().getMaxTries ())

			);

			messageLogic.messageStatus (
				message,
				MessageStatus.pending);

		} else if (
			enumInSafe (
				message.getStatus (),
				MessageStatus.pending)
		) {

			OutboxRec existingOutbox =
				outboxHelper.find (
					message);

			existingOutbox

				.setRetryTime (
					earliest (
						existingOutbox.getRetryTime (),
						transaction.now ()))

				.setRemainingTries (
					existingOutbox.getRemainingTries () != null
						? Math.max (
							existingOutbox.getRemainingTries (),
							1)
						: null);

		} else {

			throw new RuntimeException ();

		}

	}

	@Override
	public
	SmsOutboxAttemptRec beginSendAttempt (
			@NonNull OutboxRec smsOutbox,
			@NonNull Optional<byte[]> requestTrace) {

		Transaction transaction =
			database.currentTransaction ();

		MessageRec smsMessage =
			smsOutbox.getMessage ();

		SmsOutboxAttemptRec smsOutboxAttempt =
			smsOutboxAttemptHelper.insert (
				smsOutboxAttemptHelper.createInstance ()

			.setMessage (
				smsMessage)

			.setIndex (
				smsMessage.getNumAttempts ())

			.setState (
				SmsOutboxAttemptState.sending)

			.setStatusMessage (
				"Sending")

			.setRoute (
				smsOutbox.getRoute ())

			.setStartTime (
				transaction.now ())

			.setRequestTrace (
				optionalOrNull (
					requestTrace))

		);

		smsMessage

			.setNumAttempts (
				smsMessage.getNumAttempts () + 1);

		return smsOutboxAttempt;

	}

	@Override
	public
	void completeSendAttemptSuccess (
			@NonNull SmsOutboxAttemptRec smsOutboxAttempt,
			@NonNull Optional<List<String>> otherIds,
			@NonNull Optional<byte[]> requestTrace,
			@NonNull Optional<byte[]> responseTrace) {

		Transaction transaction =
			database.currentTransaction ();

		MessageRec smsMessage =
			smsOutboxAttempt.getMessage ();

		smsOutboxAttempt

			.setState (
				SmsOutboxAttemptState.success)

			.setStatusMessage (
				"Success")

			.setEndTime (
				transaction.now ())

			.setRequestTrace (
				ifNull (
					requestTrace.orNull (),
					smsOutboxAttempt.getRequestTrace ()))

			.setResponseTrace (
				optionalOrNull (
					responseTrace));

		messageSuccess (
			smsMessage,
			otherIds);

	}

	@Override
	public
	void completeSendAttemptFailure (
			@NonNull SmsOutboxAttemptRec smsOutboxAttempt,
			@NonNull FailureType failureType,
			@NonNull String errorMessage,
			@NonNull Optional<byte[]> requestTrace,
			@NonNull Optional<byte[]> responseTrace,
			@NonNull Optional<byte[]> errorTrace) {

		Transaction transaction =
			database.currentTransaction ();

		MessageRec smsMessage =
			smsOutboxAttempt.getMessage ();

		smsOutboxAttempt

			.setState (
				SmsOutboxAttemptState.failure)

			.setStatusMessage (
				errorMessage)

			.setEndTime (
				transaction.now ())

			.setRequestTrace (
				ifNull (
					requestTrace.orNull (),
					smsOutboxAttempt.getRequestTrace ()))

			.setResponseTrace (
				responseTrace.orNull ())

			.setErrorTrace (
				errorTrace.orNull ());

		messageFailure (
			smsMessage,
			errorMessage,
			failureType);

	}

}