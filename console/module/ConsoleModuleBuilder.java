package wbs.platform.console.module;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import javax.inject.Provider;

import lombok.NonNull;
import wbs.framework.application.annotations.SingletonComponent;
import wbs.framework.builder.Builder;
import wbs.framework.builder.BuilderFactory;
import wbs.platform.console.annotations.ConsoleModuleBuilderHandler;
import wbs.platform.console.forms.FormField;
import wbs.platform.console.forms.FormFieldBuilderContext;
import wbs.platform.console.forms.FormFieldBuilderContextImpl;
import wbs.platform.console.forms.FormFieldSet;
import wbs.platform.console.forms.FormFieldSetSpec;
import wbs.platform.console.helper.ConsoleHelper;
import wbs.platform.console.helper.ConsoleHelperRegistry;

@SingletonComponent ("consoleModuleBuilder")
public
class ConsoleModuleBuilder
	implements Builder {

	// dependencies

	@Inject
	ConsoleHelperRegistry consoleHelperRegistry;

	// prototype dependencies

	@Inject
	Provider<BuilderFactory> builderFactoryProvider;

	// collection dependencies

	@Inject
	@ConsoleModuleBuilderHandler
	Map<Class<?>,Provider<Object>> consoleModuleBuilders;

	// state

	Builder builder;

	// init

	@PostConstruct
	public
	void init () {

		BuilderFactory builderFactory =
			builderFactoryProvider.get ();

		for (Map.Entry<Class<?>,Provider<Object>> entry
				: consoleModuleBuilders.entrySet ()) {

			builderFactory.addBuilder (
				entry.getKey (),
				entry.getValue ());

		}

		builder =
			builderFactory.create ();

	}

	// implementation

	public
	FormFieldSet buildFormFieldSet (
			@NonNull ConsoleHelper<?> consoleHelper,
			@NonNull String fieldSetName,
			@NonNull List<Object> formFieldSpecs) {

		FormFieldBuilderContext formFieldBuilderContext =
			new FormFieldBuilderContextImpl ()
				.containerClass (consoleHelper.objectClass ())
				.consoleHelper (consoleHelper);

		FormFieldSet formFieldSet =
			new FormFieldSet ();

		builder.descend (
			formFieldBuilderContext,
			formFieldSpecs,
			formFieldSet);

		for (FormField<?,?,?,?> formField
				: formFieldSet.formFields ()) {

			formField.init (
				fieldSetName);

		}

		return formFieldSet;

	}

	// builder

	@Override
	public
	void descend (
			Object parentObject,
			List<?> childObjects,
			Object targetObject) {

		List<Object> firstPass =
			new ArrayList<Object> ();

		List<Object> secondPass =
			new ArrayList<Object> ();

		for (Object childObject
				: childObjects) {

			if (childObject instanceof FormFieldSetSpec) {

				firstPass.add (
					childObject);

			} else {

				secondPass.add (
					childObject);

			}

		}

		builder.descend (
			parentObject,
			firstPass,
			targetObject);

		builder.descend (
			parentObject,
			secondPass,
			targetObject);

	}

}
