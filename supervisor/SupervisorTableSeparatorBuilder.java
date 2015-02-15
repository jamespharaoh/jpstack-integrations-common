package wbs.platform.supervisor;

import static wbs.framework.utils.etc.Misc.stringFormat;

import javax.inject.Inject;
import javax.inject.Provider;

import lombok.experimental.Accessors;
import wbs.framework.application.annotations.PrototypeComponent;
import wbs.framework.builder.Builder;
import wbs.framework.builder.annotations.BuildMethod;
import wbs.framework.builder.annotations.BuilderParent;
import wbs.framework.builder.annotations.BuilderSource;
import wbs.framework.builder.annotations.BuilderTarget;
import wbs.platform.console.annotations.ConsoleModuleBuilderHandler;
import wbs.platform.console.part.PagePart;
import wbs.platform.text.console.TextPart;

@Accessors (fluent = true)
@PrototypeComponent ("supervisorTableSeparatorBuilder")
@ConsoleModuleBuilderHandler
public
class SupervisorTableSeparatorBuilder {

	// prototype dependencies

	@Inject
	Provider<TextPart> textPart;

	// builder

	@BuilderParent
	SupervisorConfigSpec container;

	@BuilderSource
	SupervisorTableSeparatorSpec spec;

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

				return textPart.get ()

					.text (
						stringFormat (
							"<tr class=\"sep\"></tr>\n"));


			}

		};

		supervisorTablePartBuilder.pagePartFactories ().add (
			pagePartFactory);

	}

}
