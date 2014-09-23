package wbs.apn.chat.adult.daemon;

import static wbs.framework.utils.etc.Misc.stringFormat;

import java.util.Collections;
import java.util.List;

import javax.inject.Inject;

import lombok.Cleanup;
import lombok.extern.log4j.Log4j;
import wbs.apn.chat.contact.logic.ChatSendLogic;
import wbs.apn.chat.scheme.model.ChatSchemeRec;
import wbs.apn.chat.user.core.model.ChatUserObjectHelper;
import wbs.apn.chat.user.core.model.ChatUserRec;
import wbs.framework.application.annotations.SingletonComponent;
import wbs.framework.database.Database;
import wbs.framework.database.Transaction;
import wbs.framework.object.ObjectManager;
import wbs.platform.daemon.SleepingDaemonService;

import com.google.common.base.Optional;

@Log4j
@SingletonComponent ("chatAdultExpiryDaemon")
public
class ChatAdultExpiryDaemon
	extends SleepingDaemonService {

	// dependencies

	@Inject
	ChatSendLogic chatSendLogic;

	@Inject
	ChatUserObjectHelper chatUserHelper;

	@Inject
	Database database;

	@Inject
	ObjectManager objectManager;

	// details

	@Override
	protected
	int getDelayMs () {

		return 30 * 1000;

	}

	@Override
	protected
	String generalErrorSource () {

		return "chat adult expiry daemon";

	}

	@Override
	protected
	String generalErrorSummary () {

		return "error finding users for adult expiry";

	}

	@Override
	protected
	String getThreadName () {

		return "ChatAdultExpiry";

	}

	// implementation

	@Override
	protected
	void runOnce () {

		log.debug (
			"Checking for all users whose adult verification has expired");

		@Cleanup
		Transaction transaction =
			database.beginReadOnly ();

		List<ChatUserRec> chatUsers =
			chatUserHelper.findAdultExpiryLimit (
				1000);

		transaction.close ();

		for (ChatUserRec chatUser
				: chatUsers) {

			doUserAdultExpiry (
				chatUser.getId ());

		}

	}

	void doUserAdultExpiry (
			int chatUserId) {

		@Cleanup
		Transaction transaction =
			database.beginReadWrite ();

		ChatUserRec chatUser =
			chatUserHelper.find (
				chatUserId);

		ChatSchemeRec chatScheme =
			chatUser.getChatScheme ();

		String chatUserPath =
			objectManager.objectPath (
				chatUser);

		// make sure adult expiry is set

		if (chatUser.getAdultExpiry () == null) {

			log.warn (
				stringFormat (
					"Skipped adult expiry for %s ",
					chatUserPath,
					"(field is null)"));

			return;

		}

		// make sure adult expiry time is in past

		if (
			transaction.timestamp ().getTime ()
				< chatUser.getAdultExpiry ().getTime ()
		) {

			log.warn (
				stringFormat (
					"Skipped adult expiry for %s ",
					chatUserPath,
					"(time is in future)"));

			return;

		}

		log.info (
			stringFormat (
				"Performing adult expiry for %s (time is %s)",
				chatUserPath,
				chatUser.getAdultExpiry ()));

		// update the user

		chatUser.setAdultVerified (false);
		chatUser.setAdultExpiry (null);

		if (chatUser.getBlockAll ()) {

			log.info (
				stringFormat (
					"Not sending adult expiry message to %s due to block all",
					chatUserPath));

		} else if (chatUser.getNumber () == null) {

			log.info (
				stringFormat (
					"Not sending adult expiry message to %s due to deletion",
					chatUserPath));

		} else if (chatUser.getChatScheme () == null) {

			log.info (
				stringFormat (
					"Not sending adult expiry message to %s due to lack of scheme",
					chatUserPath));

		} else {

			// send them a message

			if (chatScheme.getRbFreeRouter () != null) {

				chatSendLogic.sendSystemRbFree (
					chatUser,
					Optional.<Integer>absent (),
					"adult_expiry",
					Collections.<String,String>emptyMap ());

			} else {

				log.warn (
					stringFormat (
						"Not sending adult expiry to %s as no route is ",
						chatUserPath,
						"configured"));

			}

		}

		transaction.commit ();

	}

}
