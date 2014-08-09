package wbs.platform.supervisor;

import javax.inject.Inject;
import javax.inject.Provider;

import wbs.framework.application.annotations.PrototypeComponent;
import wbs.framework.builder.Builder;
import wbs.framework.builder.annotations.BuildMethod;
import wbs.framework.builder.annotations.BuilderParent;
import wbs.framework.builder.annotations.BuilderSource;
import wbs.framework.builder.annotations.BuilderTarget;
import wbs.platform.console.annotations.ConsoleModuleBuilderHandler;
import wbs.platform.console.part.PagePart;

@PrototypeComponent ("supervisorTableHeadingBuilder")
@ConsoleModuleBuilderHandler
public
class SupervisorTableHeadingBuilder {

	@Inject
	Provider<SupervisorTableHeadingPart> supervisorTableHeadingPart;

	// builder

	@BuilderParent
	SupervisorTablePartSpec supervisorTablePartSpec;

	@BuilderSource
	SupervisorTableHeadingSpec supervisorTableHeadingSpec;

	@BuilderTarget
	SupervisorTablePartBuilder supervisorTablePartBuilder;

	// build

	@BuildMethod
	public
	void build (
			Builder builder) {

		Provider<PagePart> pagePartFactory =
			new Provider<PagePart> () {

			@Override
			public
			PagePart get () {

				return supervisorTableHeadingPart.get ()
					.supervisorTableHeadingSpec (supervisorTableHeadingSpec);

			}

		};

		supervisorTablePartBuilder.pagePartFactories ()
			.add (pagePartFactory);

	}

}
