package wbs.platform.api.resource;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import lombok.experimental.Accessors;
import wbs.framework.application.annotations.PrototypeComponent;
import wbs.framework.data.annotations.DataAttribute;
import wbs.framework.data.annotations.DataClass;
import wbs.platform.api.module.ApiModuleData;

@Accessors (fluent = true)
@Data
@EqualsAndHashCode (of = "name")
@ToString (of = "name")
@DataClass ("post-action")
@PrototypeComponent ("apiPostActionSpec")
@ApiModuleData
public
class ApiPostActionSpec {

	@DataAttribute (
		required = true)
	String name;

}
