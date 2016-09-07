package wbs.platform.object.summary;

import static wbs.framework.utils.etc.StringUtils.capitalise;
import static wbs.framework.utils.etc.StringUtils.stringFormat;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Provider;

import lombok.Getter;
import lombok.NonNull;
import lombok.experimental.Accessors;

import wbs.console.annotations.ConsoleModuleBuilderHandler;
import wbs.console.context.ConsoleContextBuilderContainer;
import wbs.console.context.ResolvedConsoleContextExtensionPoint;
import wbs.console.forms.CodeFormFieldSpec;
import wbs.console.forms.DescriptionFormFieldSpec;
import wbs.console.forms.FieldsProvider;
import wbs.console.forms.FormFieldSet;
import wbs.console.forms.IdFormFieldSpec;
import wbs.console.forms.NameFormFieldSpec;
import wbs.console.forms.ParentFormFieldSpec;
import wbs.console.helper.ConsoleHelper;
import wbs.console.module.ConsoleMetaManager;
import wbs.console.module.ConsoleMetaModuleImplementation;
import wbs.console.module.ConsoleModuleBuilder;
import wbs.console.module.ConsoleModuleImplementation;
import wbs.console.part.PagePart;
import wbs.console.part.TextPart;
import wbs.console.responder.ConsoleFile;
import wbs.console.tab.ConsoleContextTab;
import wbs.console.tab.TabContextResponder;
import wbs.framework.builder.Builder;
import wbs.framework.builder.Builder.MissingBuilderBehaviour;
import wbs.framework.builder.annotations.BuildMethod;
import wbs.framework.builder.annotations.BuilderParent;
import wbs.framework.builder.annotations.BuilderSource;
import wbs.framework.builder.annotations.BuilderTarget;
import wbs.framework.component.annotations.PrototypeComponent;
import wbs.framework.component.manager.ComponentManager;
import wbs.framework.entity.record.Record;

@Accessors (fluent = true)
@PrototypeComponent ("objectSummaryPageBuilder")
@ConsoleModuleBuilderHandler
public
class ObjectSummaryPageBuilder<
	ObjectType extends Record<ObjectType>,
	ParentType extends Record<ParentType>
> {

	// dependencies

	@Inject
	ComponentManager applicationContext;

	@Inject
	ConsoleMetaManager consoleMetaManager;

	@Inject
	ConsoleModuleBuilder consoleModuleBuilder;

	// prototype dependencies

	@Inject
	Provider<ConsoleFile> consoleFile;

	@Inject
	Provider<ConsoleContextTab> contextTab;

	@Inject
	Provider<ObjectSummaryPart> objectSummaryPart;

	@Inject
	Provider<ParentFormFieldSpec> parentField;

	@Inject
	Provider<ObjectSummaryFieldsPart<ObjectType,ParentType>> summaryFieldsPart;

	@Inject
	Provider<TabContextResponder> tabContextResponder;

	@Inject
	Provider<TextPart> textPart;

	// builder

	@BuilderParent
	ConsoleContextBuilderContainer<ObjectType> container;

	@BuilderSource
	ObjectSummaryPageSpec spec;

	@BuilderTarget
	ConsoleModuleImplementation consoleModule;

	// state

	@Getter
	ConsoleHelper<ObjectType> consoleHelper;

	FormFieldSet formFieldSet;

	FieldsProvider<ObjectType,ParentType> fieldsProvider;

	String privKey;

	List<Provider<PagePart>> pagePartFactories =
		new ArrayList<Provider<PagePart>> ();

	// build meta

	public
	void buildMeta (
			@NonNull ConsoleMetaModuleImplementation consoleMetaModule) {

	}

	// build

	@BuildMethod
	public
	void build (
			@NonNull Builder builder) {

		setDefaults ();

		buildResponder ();

		for (
			ResolvedConsoleContextExtensionPoint resolvedExtensionPoint
				: consoleMetaManager.resolveExtensionPoint (
					container.extensionPointName ())
		) {

			buildContextTabs (
				resolvedExtensionPoint);

			buildContextFile (
				resolvedExtensionPoint);

		}

		builder.descend (
			spec,
			spec.builders (),
			this,
			MissingBuilderBehaviour.error);

	}

	void buildContextTabs (
			@NonNull ResolvedConsoleContextExtensionPoint resolvedExtensionPoint) {

		consoleModule.addContextTab (

			"end",

			contextTab.get ()

				.name (
					stringFormat (
						"%s.summary",
						container.pathPrefix ()))

				.defaultLabel (
					"Summary")

				.localFile (
					stringFormat (
						"%s.summary",
						container.pathPrefix ()))

				.privKeys (
					privKey),

			resolvedExtensionPoint.contextTypeNames ());

	}

	void buildContextFile (
			@NonNull ResolvedConsoleContextExtensionPoint resolvedExtensionPoint) {

		consoleModule.addContextFile (

			stringFormat (
				"%s.summary",
				container.pathPrefix ()),

			consoleFile.get ()

				.getResponderName (
					stringFormat (
						"%sSummaryResponder",
						container.newBeanNamePrefix ()))

				.privKeys (
					privKey != null
						? Collections.singletonList (privKey)
						: Collections.<String>emptyList ()),

			resolvedExtensionPoint.contextTypeNames ());

	}

	void buildResponder () {

		Provider<PagePart> partFactory =
			new Provider<PagePart> () {

			@Override
			public
			PagePart get () {

				return objectSummaryPart.get ()
					.partFactories (pagePartFactories);

			}

		};

		consoleModule.addResponder (

			stringFormat (
				"%sSummaryResponder",
				container.newBeanNamePrefix ()),

			tabContextResponder.get ()

				.tab (
					stringFormat (
						"%s.summary",
						container.pathPrefix ()))

				.title (
					capitalise (
						stringFormat (
							"%s summary",
							consoleHelper.friendlyName ())))

				.pagePartFactory (
					partFactory));

	}

	public
	ObjectSummaryPageBuilder<ObjectType,ParentType> addFieldsPart (
			@NonNull final FormFieldSet formFieldSet) {

		Provider<PagePart> partFactory =
			new Provider<PagePart> () {

			@Override
			public
			PagePart get () {

				return summaryFieldsPart.get ()

					.consoleHelper (
						consoleHelper)

					.formFieldSet (
						formFieldSet)

					.formFieldsProvider (
						fieldsProvider);

			}

		};

		pagePartFactories.add (
			partFactory);

		return this;

	}

	public
	ObjectSummaryPageBuilder<ObjectType,ParentType> addHeading (
			@NonNull String heading) {

		final
		String html =
			stringFormat (
				"<h2>%h</h2>\n",
				heading);

		Provider<PagePart> pagePartFactory =
			new Provider<PagePart> () {

			@Override
			public
			PagePart get () {

				return textPart.get ()

					.text (
						html);

			}

		};

		pagePartFactories.add (
			pagePartFactory);


		return this;

	}

	public
	ObjectSummaryPageBuilder<ObjectType,ParentType> addPart (
			@NonNull final String beanName) {

		Provider<PagePart> partFactory =
			new Provider<PagePart> () {

			@Override
			public
			PagePart get () {

				Object object =
					applicationContext.getComponentRequired (
						beanName,
						Object.class);

				if (object instanceof PagePart)
					return (PagePart) object;

				if (object instanceof Provider) {

					Provider<?> provider =
						(Provider<?>) object;

					return (PagePart)
						provider.get ();

				}

				throw new ClassCastException (
					object.getClass ().getName ());

			}

		};

		pagePartFactories.add (
			partFactory);

		return this;

	}

	void setDefaults () {

		consoleHelper =
			container.consoleHelper ();

		formFieldSet =
			spec.fieldsName () != null
				? consoleModule.formFieldSets ().get (
					spec.fieldsName ())
				: defaultFields ();

		privKey =
			spec.privKey ();

		if (spec.builders ().isEmpty ()) {

			addFieldsPart (
				formFieldSet);

		}

		// if a provider name is provided

		if (spec.fieldsProviderName () != null) {

			@SuppressWarnings ("unchecked")
			FieldsProvider<ObjectType,ParentType> fieldsProviderTemp =
				(FieldsProvider<ObjectType,ParentType>)
				applicationContext.getComponentRequired (
					spec.fieldsProviderName (),
					FieldsProvider.class);

			fieldsProvider =
				fieldsProviderTemp;

		}

		else {

			fieldsProvider =
				null;

		}

	}

	FormFieldSet defaultFields () {

		List<Object> formFieldSpecs =
			new ArrayList<Object> ();

		formFieldSpecs.add (
			new IdFormFieldSpec ());

		if (consoleHelper.parentTypeIsFixed ()) {

			// TODO this should not be disabled!

			/*
			ConsoleObjectHelper parentHelper =
				objectManager.getConsoleObjectHelper (
					maintSchedConsoleHelper.parentClass ());

			if (! parentHelper.isRoot ())
				fields.add (parentField.get ());
			*/

		} else {

			formFieldSpecs.add (
				new ParentFormFieldSpec ());

		}

		if (consoleHelper.codeExists ()) {

			formFieldSpecs.add (
				new CodeFormFieldSpec ());

		}

		if (consoleHelper.nameExists ()
				&& ! consoleHelper.nameIsCode ()) {

			formFieldSpecs.add (
				new NameFormFieldSpec ());

		}

		if (consoleHelper.descriptionExists ()) {

			formFieldSpecs.add (
				new DescriptionFormFieldSpec ());

		}

		String fieldSetName =
			stringFormat (
				"%s.summary",
				consoleHelper.objectName ());

		return consoleModuleBuilder.buildFormFieldSet (
			consoleHelper,
			fieldSetName,
			formFieldSpecs);

	}

}
