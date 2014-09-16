package wbs.platform.console.forms;

import static wbs.framework.utils.etc.Misc.joinWithSeparator;
import static wbs.framework.utils.etc.Misc.stringFormat;

import javax.inject.Inject;

import wbs.framework.application.annotations.PrototypeComponent;
import wbs.framework.builder.Builder;
import wbs.framework.builder.annotations.BuildMethod;
import wbs.framework.builder.annotations.BuilderParent;
import wbs.framework.builder.annotations.BuilderSource;
import wbs.framework.builder.annotations.BuilderTarget;
import wbs.platform.console.annotations.ConsoleModuleBuilderHandler;
import wbs.platform.console.helper.ConsoleHelper;
import wbs.platform.console.helper.ConsoleHelperRegistry;
import wbs.platform.console.helper.ConsoleObjectManager;
import wbs.platform.console.module.ConsoleModuleImpl;
import wbs.platform.console.spec.ConsoleSimpleBuilderContainer;

@PrototypeComponent ("formFieldSetBuilder")
@ConsoleModuleBuilderHandler
public
class FormFieldSetBuilder {

	// dependencies

	@Inject
	ConsoleHelperRegistry consoleHelperRegistry;

	@Inject
	ConsoleObjectManager objectManager;

	// builder

	@BuilderParent
	ConsoleSimpleBuilderContainer container;

	@BuilderSource
	FormFieldSetSpec spec;

	@BuilderTarget
	ConsoleModuleImpl consoleModule;

	// build

	@BuildMethod
	public
	void build (
			Builder builder) {

		if (
			spec.objectName () == null
			&& spec.className () == null
		) {

			throw new RuntimeException (
				stringFormat (
					"Form field set %s ",
					spec.name (),
					"in console module %s ",
					spec.consoleModule ().name (),
					"has neither object name nor class"));

		}

		if (
			spec.objectName () != null
			&& spec.className () != null
		) {

			throw new RuntimeException (
				stringFormat (
					"Form field set %s ",
					spec.name (),
					"in console module %s ",
					spec.consoleModule ().name (),
					"has both object name and class"));

		}

		ConsoleHelper<?> consoleHelper;
		Class<?> objectClass;

		if (spec.objectName () != null) {

			consoleHelper =
				consoleHelperRegistry.findByObjectName (
					spec.objectName ());

			objectClass =
				consoleHelper.objectClass ();

		} else {

			consoleHelper = null;

			try {

				objectClass =
					Class.forName (
						spec.className ());

			} catch (ClassNotFoundException exception) {

				throw new RuntimeException (
					stringFormat (
						"Error getting object class %s ",
						spec.className (),
						"for form field set %s ",
						spec.name (),
						"in console module %s",
						spec.consoleModule ().name ()));

			}

		}

		FormFieldBuilderContext formFieldBuilderContext =
			new FormFieldBuilderContextImpl ()

			.containerClass (
				objectClass)

			.consoleHelper (
				consoleHelper);

		FormFieldSet formFieldSet =
			new FormFieldSet ();

		builder.descend (
			formFieldBuilderContext,
			spec.formFieldSpecs (),
			formFieldSet);

		String fullName =
			joinWithSeparator (
				".",
				spec.consoleModule ().name (),
				spec.name ());

		for (FormField<?,?,?,?> formField
				: formFieldSet.formFields ()) {

			formField.init (
				fullName);

		}

		consoleModule.addFormFieldSet (
			spec.name (),
			formFieldSet);

	}

}
