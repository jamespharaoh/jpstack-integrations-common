package wbs.platform.console.combo;

import static wbs.framework.utils.etc.Misc.capitalise;
import static wbs.framework.utils.etc.Misc.ifNull;
import static wbs.framework.utils.etc.Misc.stringFormat;

import javax.inject.Inject;
import javax.inject.Provider;

import wbs.framework.application.annotations.PrototypeComponent;
import wbs.framework.builder.Builder;
import wbs.framework.builder.annotations.BuildMethod;
import wbs.framework.builder.annotations.BuilderParent;
import wbs.framework.builder.annotations.BuilderSource;
import wbs.framework.builder.annotations.BuilderTarget;
import wbs.platform.console.annotations.ConsoleModuleBuilderHandler;
import wbs.platform.console.module.ConsoleModuleImpl;
import wbs.platform.console.module.SimpleConsoleBuilderContainer;
import wbs.platform.console.responder.ConsoleFile;

@PrototypeComponent ("simpleActionPageBuilder")
@ConsoleModuleBuilderHandler
public
class SimpleActionPageBuilder {

	// prototype dependencies

	@Inject
	Provider<ConsoleFile> consoleFile;

	// builder

	@BuilderParent
	SimpleConsoleBuilderContainer simpleContainerSpec;

	@BuilderSource
	SimpleActionPageSpec simpleActionPageSpec;

	@BuilderTarget
	ConsoleModuleImpl consoleModule;

	// state

	String name;
	String path;
	String actionName;
	String responderName;
	String responderBeanName;

	// build

	@BuildMethod
	public
	void buildConsoleModule (
			Builder builder) {

		setDefaults ();

		buildFile ();
		buildResponder ();

	}

	void buildFile () {

		consoleModule.addFile (
			path,
			consoleFile.get ()
				.getResponderName (responderName)
				.postActionName (actionName));

	}

	void buildResponder () {

		consoleModule.addResponder (
			responderName,
			consoleModule.beanResponder (
				responderBeanName));

	}

	void setDefaults () {

		name =
			simpleActionPageSpec.name ();

		path =
			simpleActionPageSpec.path ();

		actionName =
			ifNull (
				simpleActionPageSpec.actionName (),
				stringFormat (
					"%s%sAction",
					simpleContainerSpec.existingBeanNamePrefix (),
					capitalise (name)));

		responderName =
			ifNull (
				simpleActionPageSpec.responderName (),
				stringFormat (
					"%s%sResponder",
					simpleContainerSpec.newBeanNamePrefix (),
					capitalise (name)));

		responderBeanName =
			ifNull (
				simpleActionPageSpec.responderBeanName (),
				stringFormat (
					"%s%sResponder",
					simpleContainerSpec.existingBeanNamePrefix (),
					capitalise (name)));

	}

}
