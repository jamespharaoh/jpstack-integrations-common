package wbs.console.context;

import static wbs.framework.utils.etc.Misc.equal;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import wbs.console.helper.PrivKeySpec;
import wbs.console.priv.PrivChecker;
import wbs.console.tab.ConsoleContextTab;
import wbs.framework.application.annotations.PrototypeComponent;
import wbs.framework.data.annotations.DataAttribute;
import wbs.framework.data.annotations.DataChildren;
import wbs.framework.data.annotations.DataClass;
import wbs.framework.record.GlobalId;
import wbs.framework.web.WebFile;

@Accessors (fluent = true)
@DataClass ("simple-context")
@PrototypeComponent ("simpleConsoleContext")
public
class SimpleConsoleContext
	extends ConsoleContext {

	// dependencies

	@Inject
	PrivChecker privChecker;

	// properties

	@DataAttribute
	@Getter @Setter
	String name;

	@DataAttribute
	@Getter @Setter
	String typeName;

	@DataAttribute
	@Getter @Setter
	String pathPrefix;

	@DataAttribute
	@Getter @Setter
	Boolean global;

	@DataAttribute
	@Getter @Setter
	String parentContextName;

	@DataAttribute
	@Getter @Setter
	String parentContextTabName;

	@DataAttribute
	@Getter @Setter
	String title;

	@DataAttribute
	@Getter @Setter
	String postProcessorName;

	@DataChildren
	@Getter @Setter
	Map<String,Object> stuff =
		new LinkedHashMap<String,Object> ();

	// state

	@Getter @Setter
	Map<String,ConsoleContextTab> contextTabs =
		new LinkedHashMap<String,ConsoleContextTab> ();

	@Getter @Setter
	Map<String,WebFile> files =
		new LinkedHashMap<String,WebFile> ();

	@Getter @Setter
	List<PrivKeySpec> privKeySpecs =
		new ArrayList<PrivKeySpec> ();

	// implementation

	@Override
	public
	String titleForStuff (
			ConsoleContextStuff stuff) {

		return title ();

	}

	@Override
	public
	String localPathForStuff (
			ConsoleContextStuff stuff) {

		if (parentContext () != null) {

			return parentContext ().localPathForStuff (
				stuff);

		} else {

			return super.localPathForStuff (
				stuff);

		}

	}

	@Override
	public
	void initContext (
			PathSupply pathParts,
			ConsoleContextStuff contextStuff) {

		// set privs

		for (PrivKeySpec privKeySpec
				: privKeySpecs) {

			if (! equal (
					privKeySpec.delegateName (),
					"root"))
				throw new RuntimeException ();

			if (privChecker.can (
					GlobalId.root,
					privKeySpec.privName ())) {

				contextStuff.grant (
					privKeySpec.name ());

			}

		}

		// set stuff

		if (stuff () != null) {

			for (Map.Entry<String,? extends Object> ent
					: stuff ().entrySet ()) {

				contextStuff.set (
					ent.getKey (),
					ent.getValue ());

			}

		}

		// run hook

		postInitHook (
			contextStuff);

		// initialise parent

		ConsoleContext parentContext =
			parentContext ();

		if (parentContext != null) {

			parentContext.initContext (
				pathParts,
				contextStuff);

		}

		// run post processors

		if (postProcessorName () != null) {

			consoleManager.get ().runPostProcessors (
				postProcessorName (),
				contextStuff);

		}

	}

	protected
	void postInitHook (
			ConsoleContextStuff contextStuff) {

	}

}