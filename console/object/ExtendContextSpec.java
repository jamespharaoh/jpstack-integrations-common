package wbs.platform.console.object;

import java.util.ArrayList;
import java.util.List;

import lombok.Data;
import lombok.experimental.Accessors;
import wbs.framework.application.annotations.PrototypeComponent;
import wbs.framework.data.annotations.DataAncestor;
import wbs.framework.data.annotations.DataAttribute;
import wbs.framework.data.annotations.DataChildren;
import wbs.framework.data.annotations.DataClass;
import wbs.platform.console.module.ConsoleModuleData;
import wbs.platform.console.module.ConsoleModuleSpec;

@Accessors (fluent = true)
@Data
@DataClass ("extend-context")
@PrototypeComponent ("extendContextSpec")
@ConsoleModuleData
public
class ExtendContextSpec {

	// attributes

	@DataAttribute (
		required = true)
	String name;

	@DataAttribute (
		required = true)
	String baseName;

	@DataAncestor
	ConsoleModuleSpec consoleSpec;

	@DataAttribute (
		value = "extension-point",
		required = true)
	String extensionPointName;

	@DataAttribute
	String objectName;

	// children

	@DataChildren (
		direct = true)
	List<Object> children =
		new ArrayList<Object> ();

}
