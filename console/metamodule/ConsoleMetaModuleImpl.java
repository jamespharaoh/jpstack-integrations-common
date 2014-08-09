package wbs.platform.console.metamodule;

import java.util.ArrayList;
import java.util.List;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import wbs.framework.application.annotations.PrototypeComponent;
import wbs.framework.data.annotations.DataChildren;
import wbs.framework.data.annotations.DataClass;
import wbs.platform.console.context.ConsoleContextExtensionPoint;
import wbs.platform.console.context.ConsoleContextLink;

@Accessors (fluent = true)
@DataClass ("console-meta-module")
@PrototypeComponent ("consoleMetaModule")
public
class ConsoleMetaModuleImpl
	implements ConsoleMetaModule {

	// properties

	@DataChildren
	@Getter @Setter
	List<ConsoleContextExtensionPoint> extensionPoints =
		new ArrayList<ConsoleContextExtensionPoint> ();

	@DataChildren
	@Getter @Setter
	List<ConsoleContextLink> contextLinks =
		new ArrayList<ConsoleContextLink> ();

	// property utils

	public
	ConsoleMetaModuleImpl addExtensionPoint (
			ConsoleContextExtensionPoint extensionPoint) {

		extensionPoints.add (
			extensionPoint);

		return this;

	}

	public
	ConsoleMetaModuleImpl addContextLink (
			ConsoleContextLink contextLink) {

		contextLinks.add (
			contextLink);

		return this;

	}

}
