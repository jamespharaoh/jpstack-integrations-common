package wbs.platform.object.browse;

import static wbs.framework.utils.etc.Misc.joinWithSpace;
import static wbs.framework.utils.etc.Misc.optionalIf;
import static wbs.framework.utils.etc.Misc.presentInstances;
import static wbs.framework.utils.etc.Misc.stringFormat;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import javax.inject.Inject;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableSet;

import wbs.console.context.ConsoleContext;
import wbs.console.context.ConsoleContextType;
import wbs.console.forms.FormFieldLogic;
import wbs.console.forms.FormFieldSet;
import wbs.console.helper.ConsoleHelper;
import wbs.console.helper.ConsoleObjectManager;
import wbs.console.html.MagicTableScriptRef;
import wbs.console.html.ScriptRef;
import wbs.console.misc.JqueryScriptRef;
import wbs.console.module.ConsoleManager;
import wbs.console.part.AbstractPagePart;
import wbs.console.priv.PrivChecker;
import wbs.framework.application.annotations.PrototypeComponent;
import wbs.framework.record.GlobalId;
import wbs.framework.record.Record;

@Accessors (fluent = true)
@PrototypeComponent ("objectBrowsePart")
public
class ObjectBrowsePart
	extends AbstractPagePart {

	// dependencies

	@Inject
	ConsoleManager consoleManager;

	@Inject
	ConsoleObjectManager objectManager;

	@Inject
	FormFieldLogic formFieldLogic;

	@Inject
	PrivChecker privChecker;

	// properties

	@Getter @Setter
	ConsoleHelper<?> consoleHelper;

	@Getter @Setter
	String typeCode;

	@Getter @Setter
	String localName;

	@Getter @Setter
	FormFieldSet formFieldSet;

	@Getter @Setter
	String targetContextTypeName;

	// state

	Record<?> currentObject;
	List<? extends Record<?>> allObjects;

	ConsoleContext targetContext;

	// details

	@Override
	public
	Set<ScriptRef> scriptRefs () {

		return ImmutableSet.<ScriptRef>builder ()

			.addAll (
				super.scriptRefs ())

			.add (
				JqueryScriptRef.instance)

			.add (
				MagicTableScriptRef.instance)

			.build ();

	}

	// implementation

	@Override
	public
	void prepare () {

		prepareCurrentObject ();
		prepareAllObjects ();
		prepareTargetContext ();

	}

	void prepareCurrentObject () {

		Integer objectId =
			(Integer)
			requestContext.stuff (
				consoleHelper.objectName () + "Id");

		if (objectId != null) {

			currentObject =
				consoleHelper.find (
					objectId);

		}

	}

	void prepareAllObjects () {

		// locate via parent

		ConsoleHelper<?> parentHelper =
			objectManager.findConsoleHelper (
				consoleHelper.parentClass ());

		Integer parentId =
			(Integer)
			requestContext.stuff (
				parentHelper.idKey ());

		if (parentId != null) {

			GlobalId parentGlobalId =
				new GlobalId (
					parentHelper.objectTypeId (),
					parentId);

			if (typeCode != null) {

				allObjects =
					consoleHelper.findByParentAndType (
						parentGlobalId,
						typeCode);

			} else {

				allObjects =
					consoleHelper.findByParent (
						parentGlobalId);

			}

			return;

		}

		// locate via grand parent

		ConsoleHelper<?> grandParentHelper =
			objectManager.findConsoleHelper (
				parentHelper.parentClass ());

		Integer grandParentId =
			(Integer)
			requestContext.stuff (
				grandParentHelper.idKey ());

		if (grandParentId != null) {

			GlobalId grandParentGlobalId =
				new GlobalId (
					grandParentHelper.objectTypeId (),
					grandParentId);

			List<? extends Record<?>> parentObjects =
				parentHelper.findByParent (
					grandParentGlobalId);

			List<Record<?>> allObjectsTemp =
				new ArrayList<Record<?>> ();

			for (Record<?> parentObject
					: parentObjects) {

				GlobalId parentGlobalId =
					new GlobalId (
						parentHelper.objectTypeId (),
						parentObject.getId ());

				if (typeCode != null) {

					allObjectsTemp.addAll (
						consoleHelper.findByParentAndType (
							parentGlobalId,
							typeCode));

				} else {

					allObjectsTemp.addAll (
						consoleHelper.findByParent (
							parentGlobalId));

				}

			}

			allObjects =
				allObjectsTemp;

			return;

		}

		// return all

		if (typeCode != null) {

			throw new RuntimeException ();

		} else {

			allObjects =
				consoleHelper.findAll ();

		}

	}

	void prepareTargetContext () {

		ConsoleContextType targetContextType =
			consoleManager.contextType (
				targetContextTypeName,
				true);

		targetContext =
			consoleManager.relatedContextRequired (
				requestContext.consoleContext (),
				targetContextType);

	}

	@Override
	public
	void renderHtmlBodyContent () {

		printFormat (
			"<table class=\"list\">\n");

		printFormat (
			"<tr>\n");

		formFieldLogic.outputTableHeadings (
			formatWriter,
			formFieldSet);

		printFormat (
			"</tr>\n");

		// render rows

		for (
			Record<?> object
				: allObjects
		) {

			printFormat (
				"<tr",

				" class=\"%h\"",
				joinWithSpace (
					presentInstances (
						Optional.of (
							"magic-table-row"),
						optionalIf (
							object == currentObject,
							"selected"))),

				" data-target-href=\"%h\"",
				requestContext.resolveContextUrl (
					stringFormat (
						"%s",
						targetContext.pathPrefix (),
						"/%s",
						consoleHelper.getPathId (
							object))),

				">\n");

			formFieldLogic.outputTableCellsList (
				formatWriter,
				formFieldSet,
				object,
				false);

			printFormat (
				"</tr>\n");

		}

		printFormat (
			"</table>\n");

	}

}
