package wbs.platform.object.summary;

import javax.inject.Inject;
import javax.inject.Provider;

import wbs.console.annotations.ConsoleModuleBuilderHandler;
import wbs.console.module.ConsoleModuleBuilder;
import wbs.framework.builder.Builder;
import wbs.framework.builder.annotations.BuildMethod;
import wbs.framework.builder.annotations.BuilderParent;
import wbs.framework.builder.annotations.BuilderSource;
import wbs.framework.builder.annotations.BuilderTarget;
import wbs.framework.component.annotations.PrototypeComponent;
import wbs.framework.entity.record.Record;

@PrototypeComponent ("objectSummaryFieldsBuilder")
@ConsoleModuleBuilderHandler
public
class ObjectSummaryFieldsBuilder<
	ObjectType extends Record<ObjectType>,
	ParentType extends Record<ParentType>
> {

	// dependencies

	@Inject
	ConsoleModuleBuilder consoleModuleBuilder;

	// prototype dependencies

	@Inject
	Provider<ObjectSummaryFieldsPart<ObjectType,ParentType>> summaryFieldsPart;

	// builder

	@BuilderParent
	ObjectSummaryPageSpec objectSummaryPageSpec;

	@BuilderSource
	ObjectSummaryFieldsSpec objectSummaryFieldsSpec;

	@BuilderTarget
	ObjectSummaryPageBuilder<ObjectType,ParentType> objectSummaryPageBuilder;

	// build

	@BuildMethod
	public
	void build (
			Builder builder) {

		objectSummaryPageBuilder.addFieldsPart (
			objectSummaryPageBuilder.consoleModule.formFieldSets ().get (
				objectSummaryFieldsSpec.fieldsName ()));

	}

}
