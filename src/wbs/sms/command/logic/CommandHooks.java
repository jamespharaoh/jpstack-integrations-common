package wbs.sms.command.logic;

import static wbs.framework.utils.etc.Misc.doesNotContain;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.annotation.PostConstruct;
import javax.inject.Inject;

import lombok.Cleanup;
import lombok.NonNull;
import wbs.framework.database.Database;
import wbs.framework.database.Transaction;
import wbs.framework.entity.record.Record;
import wbs.framework.object.ObjectHelper;
import wbs.framework.object.ObjectHooks;
import wbs.platform.object.core.model.ObjectTypeDao;
import wbs.platform.object.core.model.ObjectTypeRec;
import wbs.sms.command.model.CommandRec;
import wbs.sms.command.model.CommandTypeDao;
import wbs.sms.command.model.CommandTypeRec;

public
class CommandHooks
	implements ObjectHooks<CommandRec> {

	// dependencies

	@Inject
	CommandTypeDao commandTypeDao;

	@Inject
	Database database;

	@Inject
	ObjectTypeDao objectTypeDao;

	// state

	Map<Long,List<Long>> commandTypeIdsByParentTypeId =
		new HashMap<> ();

	// lifecycle

	@PostConstruct
	public
	void init () {

		@Cleanup
		Transaction transaction =
			database.beginReadOnly (
				"commandHooks.init ()",
				this);

		commandTypeIdsByParentTypeId =
			commandTypeDao.findAll ().stream ()

			.collect (
				Collectors.groupingBy (

				commandType ->
					commandType.getParentType ().getId (),

				Collectors.mapping (
					commandType ->
						commandType.getId (),
					Collectors.toList ())

			));

	}

	// implementation

	@Override
	public
	void createSingletons (
			@NonNull ObjectHelper<CommandRec> commandHelper,
			@NonNull ObjectHelper<?> parentHelper,
			@NonNull Record<?> parent) {

		if (
			doesNotContain (
				commandTypeIdsByParentTypeId.keySet (),
				parentHelper.objectTypeId ())
		) {
			return;
		}

		ObjectTypeRec parentType =
			objectTypeDao.findById (
				parentHelper.objectTypeId ());

		for (
			Long commandTypeId
				: commandTypeIdsByParentTypeId.get (
					parentHelper.objectTypeId ())
		) {

			CommandTypeRec commandType =
				commandTypeDao.findRequired (
					commandTypeId);

			commandHelper.insert (
				commandHelper.createInstance ()

				.setCommandType (
					commandType)

				.setCode (
					commandType.getCode ())

				.setParentType (
					parentType)

				.setParentId (
					parent.getId ())

			);

		}

	}

}