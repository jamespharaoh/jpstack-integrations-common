package wbs.platform.console.helper;

import static wbs.framework.utils.etc.Misc.stringFormat;

import java.util.ArrayList;
import java.util.List;

import lombok.Data;
import lombok.experimental.Accessors;
import wbs.framework.application.annotations.PrototypeComponent;
import wbs.framework.data.annotations.DataAttribute;
import wbs.framework.data.annotations.DataChildren;
import wbs.framework.data.annotations.DataClass;
import wbs.framework.data.annotations.DataInitMethod;
import wbs.platform.console.context.ConsoleContextStuffSpec;
import wbs.platform.console.module.ConsoleModuleData;

@Accessors (fluent = true)
@Data
@DataClass ("console-helper-provider")
@PrototypeComponent ("consoleHelperProviderSpec")
@ConsoleModuleData
public
class ConsoleHelperProviderSpec {

	// attributes

	@DataAttribute
	String objectName;

	@DataAttribute
	String idKey;

	@DataAttribute ("default-list-context")
	String defaultListContextName;

	@DataAttribute ("default-object-context")
	String defaultObjectContextName;

	@DataAttribute
	String viewDelegateField;

	@DataAttribute
	String viewDelegatePrivCode;

	@DataAttribute ("cryptor")
	String cryptorBeanName;

	// children

	@DataChildren (
		direct = true,
		childElement = "priv-key")
	List<PrivKeySpec> privKeys =
		new ArrayList<PrivKeySpec> ();

	@DataChildren (
		direct = true,
		childElement = "context-stuff")
	List<ConsoleContextStuffSpec> contextStuffs =
		new ArrayList<ConsoleContextStuffSpec> ();

	@DataChildren (
		direct = true,
		childElement = "run-post-processor")
	List<RunPostProcessorSpec> runPostProcessors =
		new ArrayList<RunPostProcessorSpec> ();

	// defaults

	@DataInitMethod
	public
	void init () {

		if (idKey () == null) {

			idKey (
				stringFormat (
					"%sId",
					objectName ()));

		}

	}

}
