package wbs.smsapps.broadcast.logic;

import java.util.List;

import javax.inject.Inject;
import javax.inject.Provider;

import lombok.NonNull;
import wbs.framework.database.Database;
import wbs.framework.object.ObjectHooks;
import wbs.platform.object.core.model.ObjectTypeDao;
import wbs.platform.object.core.model.ObjectTypeRec;
import wbs.sms.message.batch.logic.BatchLogic;
import wbs.sms.message.batch.model.BatchObjectHelper;
import wbs.sms.message.batch.model.BatchSubjectRec;
import wbs.sms.number.format.logic.NumberFormatLogic;
import wbs.sms.number.format.logic.WbsNumberFormatException;
import wbs.smsapps.broadcast.model.BroadcastConfigRec;
import wbs.smsapps.broadcast.model.BroadcastRec;

public
class BroadcastHooks
	implements ObjectHooks<BroadcastRec> {

	// dependencies

	@Inject
	Database database;

	// indirect dependencies

	@Inject
	Provider<BatchObjectHelper> batchHelper;

	@Inject
	Provider<BatchLogic> batchLogic;

	@Inject
	Provider<BroadcastLogic> broadcastLogicProvider;

	@Inject
	Provider<NumberFormatLogic> numberFormatLogicProvider;

	@Inject
	Provider<ObjectTypeDao> objectTypeDao;

	// implementation

	@Override
	public
	void beforeInsert (
			@NonNull BroadcastRec broadcast) {

		BroadcastConfigRec broadcastConfig =
			broadcast.getBroadcastConfig ();

		// set index

		broadcast.setIndex (
			broadcastConfig.getNumTotal ());

	}

	@Override
	public
	void afterInsert (
			@NonNull BroadcastRec broadcast) {

		BroadcastConfigRec broadcastConfig =
			broadcast.getBroadcastConfig ();

		// update parent counts

		broadcastConfig.setNumTotal (
			broadcastConfig.getNumTotal () + 1);

		broadcastConfig.setNumUnsent (
			broadcastConfig.getNumUnsent () + 1);

		// create batch

		BatchSubjectRec batchSubject =
			batchLogic.get ().batchSubject (
				broadcastConfig,
				"broadcast");

		ObjectTypeRec broadcastObjectType =
			objectTypeDao.get ().findByCode (
				"broadcast");

		if (broadcastObjectType == null)
			throw new NullPointerException ();

		batchHelper.get ().insert (
			batchHelper.get ().createInstance ()

			.setParentType (
				broadcastObjectType)

			.setParentId (
				broadcast.getId ())

			.setCode (
				"broadcast")

			.setSubject (
				batchSubject)

		);

		// add default numbers

		if (broadcastConfig.getDefaultNumbers () != null) {

			List<String> numbers;

			try {

				numbers =
					numberFormatLogicProvider.get ().parseLines (
						broadcastConfig.getNumberFormat (),
						broadcastConfig.getDefaultNumbers ());

			} catch (WbsNumberFormatException exception) {

				throw new RuntimeException (
					"Number format error parsing default numbers",
					exception);

			}

			broadcastLogicProvider.get ().addNumbers (
				broadcast,
				numbers,
				null);

		}

	}

}