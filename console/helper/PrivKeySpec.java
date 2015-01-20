package wbs.platform.console.helper;

import lombok.Data;
import lombok.experimental.Accessors;
import wbs.framework.application.annotations.PrototypeComponent;
import wbs.framework.data.annotations.DataAttribute;
import wbs.framework.data.annotations.DataClass;
import wbs.platform.console.module.ConsoleModuleData;

@Accessors (fluent = true)
@Data
@DataClass ("priv-key")
@PrototypeComponent ("privKeySpec")
@ConsoleModuleData
public
class PrivKeySpec {

	@DataAttribute (
		required = true)
	String name;

	@DataAttribute (
		value = "priv",
		required = true)
	String privName;

	@DataAttribute ("delegate")
	String delegateName;

}
