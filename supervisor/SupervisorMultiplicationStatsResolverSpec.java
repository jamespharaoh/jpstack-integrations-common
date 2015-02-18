package wbs.platform.supervisor;

import java.util.ArrayList;
import java.util.List;

import lombok.Data;
import lombok.experimental.Accessors;
import wbs.framework.application.annotations.PrototypeComponent;
import wbs.framework.data.annotations.DataAttribute;
import wbs.framework.data.annotations.DataChildren;
import wbs.framework.data.annotations.DataClass;
import wbs.framework.data.annotations.DataParent;
import wbs.platform.console.module.ConsoleModuleData;

@Accessors (fluent = true)
@Data
@DataClass ("multiplication-stats-resolver")
@PrototypeComponent ("supervisorMultiplicationStatsResponderSpec")
@ConsoleModuleData
public
class SupervisorMultiplicationStatsResolverSpec {

	@DataParent
	SupervisorConfigSpec supervisorConfig;

	@DataAttribute
	String name;

	@DataChildren (direct = true)
	List<SupervisorMultiplicationOperandSpec> operandSpecs =
		new ArrayList<SupervisorMultiplicationOperandSpec> ();


}
