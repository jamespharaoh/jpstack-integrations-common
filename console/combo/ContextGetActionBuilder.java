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
import wbs.platform.console.context.ConsoleContextBuilderContainer;
import wbs.platform.console.context.ResolvedConsoleContextExtensionPoint;
import wbs.platform.console.module.ConsoleMetaManager;
import wbs.platform.console.module.ConsoleModuleImpl;
import wbs.platform.console.responder.ConsoleFile;

@PrototypeComponent ("contextGetActionBuilder")
@ConsoleModuleBuilderHandler
public
class ContextGetActionBuilder {

	// dependencies

	@Inject
	Provider<ConsoleFile> consoleFile;

	@Inject
	ConsoleMetaManager consoleMetaManager;

	// builder

	@BuilderParent
	ConsoleContextBuilderContainer container;

	@BuilderSource
	ContextGetActionSpec spec;

	@BuilderTarget
	ConsoleModuleImpl consoleModule;

	// state

	String name;
	String contextFileName;
	String actionName;

	// build

	@BuildMethod
	public
	void build (
			Builder builder) {

		setDefaults ();

		for (ResolvedConsoleContextExtensionPoint resolvedExtensionPoint
				: consoleMetaManager.resolveExtensionPoint (
					container.extensionPointName ())) {

			buildContextFile (
				resolvedExtensionPoint);

		}

	}

	void buildContextFile (
			ResolvedConsoleContextExtensionPoint resolvedExtensionPoint) {

		consoleModule.addContextFile (
			contextFileName,
			consoleFile.get ()
				.getActionName (actionName),
			resolvedExtensionPoint.contextTypeNames ());

	}

	void setDefaults () {

		name =
			spec.name ();

		contextFileName =
			ifNull (
				spec.contextFileName (),
				stringFormat (
					"%s.%s",
					container.pathPrefix (),
					name));

		actionName =
			ifNull (
				spec.actionName (),
				stringFormat (
					"%s%sAction",
					container.existingBeanNamePrefix (),
					capitalise (name)));

	}

}
