package wbs.integrations.common.fixture;

import lombok.NonNull;

import wbs.framework.component.annotations.ClassSingletonDependency;
import wbs.framework.component.annotations.PrototypeComponent;
import wbs.framework.component.annotations.SingletonDependency;
import wbs.framework.entity.record.GlobalId;
import wbs.framework.fixtures.FixtureProvider;
import wbs.framework.logging.LogContext;
import wbs.framework.logging.TaskLogger;

import wbs.platform.menu.model.MenuGroupObjectHelper;
import wbs.platform.scaffold.model.SliceObjectHelper;

@PrototypeComponent ("integrationsCommonFixtureProvider")
public
class IntegrationsCommonFixtureProvider
	implements FixtureProvider {

	// singleton dependencies

	@ClassSingletonDependency
	LogContext logContext;

	@SingletonDependency
	MenuGroupObjectHelper menuGroupHelper;

	@SingletonDependency
	SliceObjectHelper sliceHelper;

	// implementation

	@Override
	public
	void createFixtures (
			@NonNull TaskLogger parentTaskLogger) {

		TaskLogger taskLogger =
			logContext.nestTaskLogger (
				parentTaskLogger,
				"createFixtures");

		menuGroupHelper.insert (
			taskLogger,
			menuGroupHelper.createInstance ()

			.setSlice (
				sliceHelper.findByCodeRequired (
					GlobalId.root,
					"test"))

			.setCode (
				"integration")

			.setName (
				"Integration")

			.setDescription (
				"")

			.setLabel (
				"Integrations")

			.setOrder (
				60l)

		);

	}

}
