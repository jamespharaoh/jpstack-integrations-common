package wbs.platform.api.module;

import lombok.Data;
import lombok.experimental.Accessors;
import wbs.framework.application.annotations.PrototypeComponent;

@Accessors (fluent = true)
@Data
@PrototypeComponent ("simpleApiBuilderContainerImplementation")
public
class SimpleApiBuilderContainerImplementation
	implements SimpleApiBuilderContainer {

	String newBeanNamePrefix;
	String existingBeanNamePrefix;

}
