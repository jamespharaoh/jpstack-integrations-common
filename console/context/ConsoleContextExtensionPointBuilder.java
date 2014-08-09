package wbs.platform.console.context;

import javax.inject.Inject;

import lombok.experimental.Accessors;
import wbs.framework.application.annotations.PrototypeComponent;
import wbs.framework.builder.Builder;
import wbs.framework.builder.annotations.BuildMethod;
import wbs.framework.builder.annotations.BuilderParent;
import wbs.framework.builder.annotations.BuilderSource;
import wbs.framework.builder.annotations.BuilderTarget;
import wbs.platform.console.annotations.ConsoleModuleBuilderHandler;
import wbs.platform.console.metamodule.ConsoleMetaManager;
import wbs.platform.console.module.ConsoleModuleImpl;

@Accessors (fluent = true)
@PrototypeComponent ("consoleContextExtensionPointBuilder")
@ConsoleModuleBuilderHandler
public
class ConsoleContextExtensionPointBuilder {

	// dependencies

	@Inject
	ConsoleMetaManager consoleMetaManager;

	// builder

	@BuilderParent
	ConsoleContextBuilderContainer container;

	@BuilderSource
	ConsoleContextExtensionPointSpec spec;

	@BuilderTarget
	ConsoleModuleImpl consoleModule;

	// build

	@BuildMethod
	public
	void build (
			Builder builder) {

		for (ResolvedConsoleContextExtensionPoint resolvedExtensionPoint
				: consoleMetaManager.resolveExtensionPoint (
					container.extensionPointName ())) {

			consoleModule.addTabLocation (
				container.tabLocation (),
				spec.name (),
				resolvedExtensionPoint.contextTypeNames ());

		}

	}

}
