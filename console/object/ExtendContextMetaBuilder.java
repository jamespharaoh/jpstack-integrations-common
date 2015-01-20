package wbs.platform.console.object;

import wbs.framework.application.annotations.PrototypeComponent;
import wbs.framework.builder.Builder;
import wbs.framework.builder.annotations.BuildMethod;
import wbs.framework.builder.annotations.BuilderParent;
import wbs.framework.builder.annotations.BuilderSource;
import wbs.framework.builder.annotations.BuilderTarget;
import wbs.platform.console.annotations.ConsoleMetaModuleBuilderHandler;
import wbs.platform.console.context.ConsoleContextMetaBuilderContainer;
import wbs.platform.console.module.ConsoleMetaModuleImpl;

@PrototypeComponent ("extendContextMetaBuilder")
@ConsoleMetaModuleBuilderHandler
public
class ExtendContextMetaBuilder {

	// builder

	@BuilderParent
	ConsoleContextMetaBuilderContainer container;

	@BuilderSource
	ExtendContextSpec spec;

	@BuilderTarget
	ConsoleMetaModuleImpl metaModule;

	// state

	String name;
	String baseName;
	String extensionPointName;

	// build

	@BuildMethod
	public
	void build (
			Builder builder) {

		setDefaults ();

		ConsoleContextMetaBuilderContainer nextContainer =
			new ConsoleContextMetaBuilderContainer ()

			.structuralName (
				baseName)

			.extensionPointName (
				extensionPointName);

		builder.descend (
			nextContainer,
			spec.children (),
			metaModule);

	}

	// defaults

	void setDefaults () {

		name =
			spec.name ();

		baseName =
			spec.baseName ();

		extensionPointName =
			spec.extensionPointName ();

	}

}
