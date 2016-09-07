package wbs.platform.object.list;

import lombok.Data;
import lombok.experimental.Accessors;

import wbs.console.module.ConsoleModuleData;
import wbs.framework.component.annotations.PrototypeComponent;
import wbs.framework.data.annotations.DataAttribute;
import wbs.framework.data.annotations.DataClass;

@Accessors (fluent = true)
@Data
@DataClass ("list-browser")
@PrototypeComponent ("objectListBrowserSpec")
@ConsoleModuleData
public
class ObjectListBrowserSpec {

	// attributes

	@DataAttribute (
		name = "field",
		required = true)
	String fieldName;

	@DataAttribute
	String label;

}
