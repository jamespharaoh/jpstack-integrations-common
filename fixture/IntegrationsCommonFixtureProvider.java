package wbs.integrations.common.fixture;

import javax.inject.Inject;

import wbs.framework.application.annotations.PrototypeComponent;
import wbs.framework.fixtures.FixtureProvider;
import wbs.framework.record.GlobalId;
import wbs.platform.menu.model.MenuGroupObjectHelper;
import wbs.platform.menu.model.MenuGroupRec;
import wbs.platform.scaffold.model.SliceObjectHelper;

@PrototypeComponent ("integrationsCommonFixtureProvider")
public
class IntegrationsCommonFixtureProvider
	implements FixtureProvider {

	// dependencies

	@Inject
	MenuGroupObjectHelper menuGroupHelper;

	@Inject
	SliceObjectHelper sliceHelper;

	// implementation

	@Override
	public
	void createFixtures () {

		menuGroupHelper.insert (
			new MenuGroupRec ()

			.setSlice (
				sliceHelper.findByCode (
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
				60)

		);

	}

}
