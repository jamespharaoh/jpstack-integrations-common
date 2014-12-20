package wbs.platform.application.tools;

import static wbs.framework.utils.etc.Misc.capitalise;
import static wbs.framework.utils.etc.Misc.equal;
import static wbs.framework.utils.etc.Misc.hyphenToCamel;
import static wbs.framework.utils.etc.Misc.stringFormat;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import lombok.SneakyThrows;
import lombok.experimental.Accessors;
import lombok.extern.log4j.Log4j;
import wbs.framework.application.annotations.PrototypeComponent;
import wbs.framework.application.annotations.ProxiedRequestComponent;
import wbs.framework.application.annotations.SingletonComponent;
import wbs.framework.application.context.ApplicationContext;
import wbs.framework.application.context.BeanDefinition;
import wbs.framework.application.context.MethodBeanFactory;
import wbs.framework.application.scaffold.BuildPluginSpec;
import wbs.framework.application.scaffold.BuildSpec;
import wbs.framework.application.scaffold.PluginBeanSpec;
import wbs.framework.application.scaffold.PluginConsoleModuleSpec;
import wbs.framework.application.scaffold.PluginCustomTypeSpec;
import wbs.framework.application.scaffold.PluginDependencySpec;
import wbs.framework.application.scaffold.PluginEnumTypeSpec;
import wbs.framework.application.scaffold.PluginFixtureSpec;
import wbs.framework.application.scaffold.PluginLayerSpec;
import wbs.framework.application.scaffold.PluginManager;
import wbs.framework.application.scaffold.PluginModelSpec;
import wbs.framework.application.scaffold.PluginModelsSpec;
import wbs.framework.application.scaffold.PluginSpec;
import wbs.framework.data.tools.DataFromXml;
import wbs.framework.object.AbstractObjectHooks;
import wbs.framework.object.ObjectHelperFactory;
import wbs.framework.object.ObjectHelperProvider;
import wbs.platform.console.helper.ConsoleHelperFactory;
import wbs.platform.console.helper.EnumConsoleHelper;
import wbs.platform.console.helper.EnumConsoleHelperFactory;
import wbs.platform.console.metamodule.ConsoleMetaModule;
import wbs.platform.console.metamodule.ConsoleMetaModuleFactory;
import wbs.platform.console.module.ConsoleModule;
import wbs.platform.console.module.ConsoleModuleFactory;
import wbs.platform.console.module.ConsoleModuleSpecFactory;
import wbs.platform.console.spec.ConsoleSpec;
import wbs.platform.object.core.hibernate.ObjectHelperProviderFactory;

import com.google.common.collect.ImmutableList;

@Accessors (fluent = true)
@Log4j
public
class ApplicationContextBuilder {

	@Getter @Setter
	String primaryProjectPackageName;

	@Getter @Setter
	String primaryProjectName;

	@Getter @Setter
	List<String> configNames =
		new ArrayList<String> ();

	@Getter @Setter
	List<String> layerNames =
		new ArrayList<String> ();

	@Getter @Setter
	String outputPath;

	List<PluginSpec> plugins;
	PluginManager pluginManager;

	Map<String,Object> singletonBeans =
		new LinkedHashMap<String,Object> ();

	List<BeanDefinition> beanDefinitionsToRegister =
		new ArrayList<BeanDefinition> ();

	ApplicationContext applicationContext;

	public
	ApplicationContextBuilder addSingletonBean (
			@NonNull String singletonName,
			@NonNull Object singletonBean) {

		singletonBeans.put (
			singletonName,
			singletonBean);

		return this;

	}

	@SneakyThrows (Exception.class)
	public
	ApplicationContext build () {

		loadPlugins ();

		createPluginManager ();

		initContext ();

		return applicationContext;

	}

	private
	void loadPlugins () {

		String buildPath =
			"/wbs-build.xml";

		DataFromXml buildDataFromXml =
			new DataFromXml ()

			.registerBuilderClasses (
				BuildSpec.class,
				BuildPluginSpec.class);

		BuildSpec build =
			(BuildSpec)
			buildDataFromXml.readClasspath (
				Collections.emptyList (),
				buildPath);

		ImmutableList.Builder<PluginSpec> pluginsBuilder =
			ImmutableList.<PluginSpec>builder ();

		DataFromXml pluginDataFromXml =
			new DataFromXml ()

			.registerBuilderClasses (
				PluginBeanSpec.class,
				PluginConsoleModuleSpec.class,
				PluginCustomTypeSpec.class,
				PluginEnumTypeSpec.class,
				PluginFixtureSpec.class,
				PluginLayerSpec.class,
				PluginModelSpec.class,
				PluginModelsSpec.class,
				PluginDependencySpec.class,
				PluginSpec.class);

		for (
			BuildPluginSpec buildPlugin
				: build.plugins ()
		) {

			String pluginPath =
				stringFormat (
					"/%s",
					buildPlugin.packageName ().replace (".", "/"),
					"/%s-plugin.xml",
					buildPlugin.name ());

			PluginSpec plugin =
				(PluginSpec)
				pluginDataFromXml.readClasspath (
					ImmutableList.<Object>of (
						build),
					pluginPath);

			pluginsBuilder.add (
				plugin);

		}

		plugins =
			pluginsBuilder.build ();

	}

	private
	void createPluginManager () {

		pluginManager =
			new PluginManager.Builder ()
				.plugins (plugins)
				.build ();

		addSingletonBean (
			"pluginManager",
			pluginManager);

	}

	private
	void initContext ()
		throws Exception {

		int errors = 0;

		createApplicationContext ();

		errors +=
			registerLayerBeans ();

		errors +=
			registerConfigBeans ();

		errors +=
			registerSingletonBeans ();

		if (errors > 0) {

			throw new RuntimeException (
				stringFormat (
					"Aborting due to %s errors",
					errors));

		}

		for (BeanDefinition beanDefinition
				: beanDefinitionsToRegister) {

			applicationContext.registerBeanDefinition (
				beanDefinition);

		}

		log.info (
			stringFormat (
				"initialising application"));

		applicationContext.init ();

		log.info (
			stringFormat (
				"application initialised"));

	}

	private
	int registerLayerBeans ()
		throws Exception {

		int errors = 0;

		for (
			String layerName
				: layerNames
		) {

			log.info (
				stringFormat (
					"Loading beans for layer %s",
					layerName));

			for (
				PluginSpec plugin
					: plugins
			) {

				errors +=
					registerLayerBeans (
						plugin,
						layerName);

			}

		}

		return errors;

	}

	int registerLayerBeans (
			@NonNull PluginSpec plugin,
			@NonNull String layerName)
		throws Exception {

		int errors = 0;

		PluginLayerSpec layer =
			plugin.layersByName ().get (layerName);

		if (layer != null) {

			for (PluginBeanSpec bean
					: layer.beans ()) {

				errors +=
					registerLayerBean (
						bean);

			}

		}

		if (equal (layerName, "console")) {

			errors +=
				registerConsoleLayerBeans (
					plugin);

			for (PluginConsoleModuleSpec consoleModule
					: plugin.consoleModules ()) {

				errors +=
					registerConsoleModule (
						consoleModule);

			}

			/*
			registerMissingConsoleHelperProviders (
				plugin,
				consoleHelperProviderObjectNames);
			*/

		}

		if (equal (
				layerName,
				"hibernate")) {

			errors +=
				registerHibernateLayerBeans (
					plugin);

		}

		if (equal (
				layerName,
				"object")) {

			errors +=
				registerObjectLayerBeans (
					plugin);

		}

		if (equal (
				layerName,
				"fixture")) {

			errors +=
				registerFixtureLayerBeans (
					plugin);

		}

		return errors;

	}

	int registerConsoleModule (
			@NonNull PluginConsoleModuleSpec pluginConsoleModuleSpec) {

		String xmlResourceName =
			stringFormat (
				"/%s/console/%s-console.xml",
				pluginConsoleModuleSpec
					.plugin ()
					.packageName ()
					.replace (".", "/"),
				pluginConsoleModuleSpec
					.name ());

		String consoleSpecBeanName =
			stringFormat (
				"%sSpec",
				hyphenToCamel (
					pluginConsoleModuleSpec.name ()));

		String consoleModuleBeanName =
			stringFormat (
				"%sConsoleModule",
				hyphenToCamel (
					pluginConsoleModuleSpec.name ()));

		String consoleMetaModuleBeanName =
			stringFormat (
				"%sConsoleMetaModule",
				hyphenToCamel (
					pluginConsoleModuleSpec.name ()));

		/*
		for (ConsoleHelperProviderSpec consoleHelperProviderSpec
				: consoleModuleMiniSpec.consoleHelperProviders ()) {

			if (consoleHelperProviderObjectNames.contains (
					consoleHelperProviderSpec.objectName ()))
				throw new RuntimeException ();

			consoleHelperProviderObjectNames.add (
				consoleHelperProviderSpec.objectName ());

			String objectHelperBeanName =
				sf ("%sObjectHelper",
					consoleHelperProviderSpec.objectName ());

			String consoleHelperProviderBeanName =
				sf ("%sConsoleHelperProvider",
					consoleHelperProviderSpec.objectName ());

			String consoleHelperClassName =
				sf ("%s.%s.console.%sConsoleHelper",
					consoleModule.project ().packageName (),
					consoleModule.plugin ().packageName (),
					capitalise (consoleHelperProviderSpec.objectName ()));

			Class<?> consoleHelperClass;

			try {

				consoleHelperClass =
					Class.forName (
						consoleHelperClassName);

			} catch (ClassNotFoundException exception) {

				log.error (sf (
					"Console helper class %s not found",
					consoleHelperClassName));

				return 1;

			}

			applicationContext.registerBeanDefinition (
				new BeanDefinition ()
					.name (consoleHelperProviderBeanName)
					.beanClass (GenericConsoleHelperProvider.class)
					.scope ("singleton")

					.addValueProperty (
						"consoleHelperProviderSpec",
						consoleHelperProviderSpec)

					.addReferenceProperty (
						"objectHelper",
						objectHelperBeanName)

					.addValueProperty (
						"consoleHelperClass",
						consoleHelperClass));

		}
		*/

		applicationContext.registerBeanDefinition (
			new BeanDefinition ()

			.name (
				consoleSpecBeanName)

			.beanClass (
				ConsoleSpec.class)

			.scope (
				"singleton")

			.factoryClass (
				ConsoleModuleSpecFactory.class)

			.addValueProperty (
				"xmlResourceName",
				xmlResourceName)

		);

		applicationContext.registerBeanDefinition (
			new BeanDefinition ()

			.name (
				consoleModuleBeanName)

			.beanClass (
				ConsoleModule.class)

			.scope (
				"singleton")

			.factoryClass (
				ConsoleModuleFactory.class)

			.addReferenceProperty (
				"consoleSpec",
				consoleSpecBeanName)

		);

		applicationContext.registerBeanDefinition (
			new BeanDefinition ()

			.name (
				consoleMetaModuleBeanName)

			.beanClass (
				ConsoleMetaModule.class)

			.scope (
				"singleton")

			.factoryClass (
				ConsoleMetaModuleFactory.class)

			.addReferenceProperty (
				"consoleSpec",
				consoleSpecBeanName)

		);

		return 0;

	}

	int registerFixtureLayerBeans (
			@NonNull PluginSpec plugin)
		throws Exception {

		int errors = 0;

		for (
			PluginFixtureSpec fixture
				: plugin.fixtures ()
		) {

			String fixtureProviderBeanName =
				stringFormat (
					"%sFixtureProvider",
					fixture.name ());

			String fixtureProviderClassName =
				stringFormat (
					"%s.fixture.%sFixtureProvider",
					plugin.packageName (),
					capitalise (
						fixture.name ()));

			Class<?> fixtureProviderClass;

			try {

				fixtureProviderClass =
					Class.forName (
						fixtureProviderClassName);

			} catch (ClassNotFoundException exception) {

				log.error (
					stringFormat (
						"Can't find fixture provider of type %s ",
						fixtureProviderClassName,
						"for fixture %s ",
						fixture.name (),
						"from %s",
						plugin.name ()));

				errors ++;

				continue;

			}

			applicationContext.registerBeanDefinition (
				new BeanDefinition ()

				.name (
					fixtureProviderBeanName)

				.beanClass (
					fixtureProviderClass)

				.scope (
					"prototype"));

		}

		return errors;

	}

	/*
	int registerMissingConsoleHelperProviders (
			ProjectPluginSpec plugin,
			Set<String> consoleHelperProviderObjectNames) {

		int errorCount = 0;

		for (ProjectModelSpec model
				: plugin.models ().models ()) {

			if (consoleHelperProviderObjectNames.contains (
					model.name ()))
				continue;

			String objectHelperBeanName =
				sf ("%sObjectHelper",
					model.name ());

			String consoleHelperProviderBeanName =
				sf ("%sConsoleHelperProvider",
					model.name ());

			String consoleHelperClassName =
				sf ("%s.%s.console.%sConsoleHelper",
					plugin.project ().packageName (),
					plugin.packageName (),
					capitalise (model.name ()));

			Class<?> consoleHelperClass;

			try {

				consoleHelperClass =
					Class.forName (
						consoleHelperClassName);

			} catch (ClassNotFoundException exception) {

				log.error (sf (
					"Console helper class %s not found",
					consoleHelperClassName));

				return 1;

			}

			applicationContext.registerBeanDefinition (
				new BeanDefinition ()
					.name (consoleHelperProviderBeanName)
					.beanClass (GenericConsoleHelperProvider.class)
					.scope ("singleton")

					.addValueProperty (
						"consoleHelperProviderSpec",
						new ConsoleHelperProviderSpec ())

					.addReferenceProperty (
						"objectHelper",
						objectHelperBeanName)

					.addValueProperty (
						"consoleHelperClass",
						consoleHelperClass));

		}

		return errorCount;

	}
	*/

	int registerHibernateLayerBeans (
			@NonNull PluginSpec plugin)
		throws Exception {

		int errorCount = 0;

		for (PluginModelSpec projectModelSpec
				: plugin.models ().models ()) {

			errorCount +=
				registerDaoHibernate (
					projectModelSpec);

		}

		return errorCount;

	}

	int registerObjectLayerBeans (
			@NonNull PluginSpec plugin)
		throws Exception {

		int errorCount = 0;

		for (PluginModelSpec projectModelSpec
				: plugin.models ().models ()) {

			errorCount +=
				registerObjectHooks (
					projectModelSpec);

			errorCount +=
				registerObjectHelperProvider (
					projectModelSpec);

			errorCount +=
				registerObjectHelper (
					projectModelSpec);

			errorCount +=
				registerObjectHelperImplementation (
					projectModelSpec);

		}

		return errorCount;

	}

	int registerConsoleLayerBeans (
			@NonNull PluginSpec plugin)
		throws Exception {

		int errors = 0;

		for (PluginModelSpec model
				: plugin.models ().models ()) {

			/*
			errors +=
				registerConsoleHelperProvider (
					model);
			*/

			errors +=
				registerConsoleHelper (
					model);

		}

		for (
			PluginEnumTypeSpec enumType
				: plugin.models ().enumTypes ()
		) {

			errors +=
				registerEnumConsoleHelper (
					enumType);

		}

		return errors;

	}

	int registerLayerBean (
			@NonNull PluginBeanSpec beanSpec)
		throws Exception {

		log.debug (
			stringFormat (
				"Loading %s from %s",
				beanSpec.className (),
				beanSpec.plugin ().name ()));

		String beanClassName =
			stringFormat (
				"%s.%s",
				beanSpec.plugin ().packageName (),
				beanSpec.className ());

		Class<?> beanClass;

		try {

			beanClass =
				Class.forName (
					beanClassName);

		} catch (ClassNotFoundException exception) {

			log.error (
				stringFormat (
					"No such class %s in %s.%s.%s",
					beanClassName,
					beanSpec.plugin ().name (),
					beanSpec.layer ().name (),
					beanSpec.className ()));

			return 1;

		}

		String beanName = null;

		SingletonComponent singletonComponent =
			beanClass.getAnnotation (
				SingletonComponent.class);

		if (singletonComponent != null) {

			beanName =
				singletonComponent.value ();

			applicationContext.registerBeanDefinition (
				new BeanDefinition ()
					.name (beanName)
					.beanClass (beanClass)
					.scope ("singleton"));

		}

		PrototypeComponent prototypeComponent =
			beanClass.getAnnotation (PrototypeComponent.class);

		if (prototypeComponent != null) {

			beanName =
				prototypeComponent.value ();

			applicationContext.registerBeanDefinition (
				new BeanDefinition ()
					.name (beanName)
					.beanClass (beanClass)
					.scope ("prototype"));

		}

		ProxiedRequestComponent proxiedRequestComponent =
			beanClass.getAnnotation (
				ProxiedRequestComponent.class);

		if (proxiedRequestComponent != null) {

			beanName =
				proxiedRequestComponent.value ();

			String targetBeanName =
				stringFormat (
					"%sTarget",
					beanName);

			applicationContext.registerBeanDefinition (
				new BeanDefinition ()
					.name (targetBeanName)
					.beanClass (beanClass)
					.scope ("prototype")
					.hide (true));

			applicationContext.registerBeanDefinition (
				new BeanDefinition ()
					.name (beanName)
					.beanClass (proxiedRequestComponent.proxyInterface ())
					.factoryClass (ThreadLocalProxyBeanFactory.class)
					.scope ("singleton")

					.addValueProperty (
						"beanName",
						beanName)

					.addValueProperty (
						"beanClass",
						proxiedRequestComponent.proxyInterface ()));

			applicationContext.requestBeanNames ().add (
				beanName);

		}

		if (beanName == null) {

			log.error (
				stringFormat (
					"Could not find component annotation on %s",
					beanClass.getName ()));

			return 1;

		}

		for (Method method
				: beanClass.getDeclaredMethods ()) {

			SingletonComponent singletonComponentAnnotation =
				method.getAnnotation (
					SingletonComponent.class);

			if (singletonComponentAnnotation != null) {

				applicationContext.registerBeanDefinition (
					new BeanDefinition ()

					.name (
						method.getName ())

					.beanClass (
						method.getReturnType ())

					.scope (
						"singleton")

					.factoryClass (
						MethodBeanFactory.class)

					.addReferenceProperty (
						"factoryBean",
						beanName)

					.addValueProperty (
						"factoryMethodName",
						method.getName ())

				);

			}

			PrototypeComponent prototypeComponentAnnotation =
				method.getAnnotation (
					PrototypeComponent.class);

			if (prototypeComponentAnnotation != null) {

				applicationContext.registerBeanDefinition (
					new BeanDefinition ()

					.name (
						method.getName ())

					.beanClass (
						method.getReturnType ())

					.scope (
						"prototype")

					.factoryClass (
						MethodBeanFactory.class)

					.addReferenceProperty (
						"factoryBean",
						beanName)

					.addValueProperty (
						"factoryMethodName",
						method.getName ()));

			}

		}

		return 0;

	}

	int registerObjectHooks (
			@NonNull PluginModelSpec model)
		throws Exception {

		String objectHooksBeanName =
			stringFormat (
				"%sHooks",
				model.name ());

		String objectHooksClassName =
			stringFormat (
				"%s.model.%sRec$%sHooks",
				model.plugin ().packageName (),
				capitalise (model.name ()),
				capitalise (model.name ()));

		Class<?> objectHooksClass;

		try {

			objectHooksClass =
				Class.forName (objectHooksClassName);

			applicationContext.registerBeanDefinition (
				new BeanDefinition ()
					.name (objectHooksBeanName)
					.beanClass (objectHooksClass)
					.scope ("singleton"));

			return 0;

		} catch (ClassNotFoundException exception) {

			applicationContext.registerBeanDefinition (
				new BeanDefinition ()
					.name (objectHooksBeanName)
					.beanClass (AbstractObjectHooks.class)
					.scope ("singleton"));

			return 0;

		}

	}

	int registerObjectHelper (
			@NonNull PluginModelSpec model)
		throws Exception {

		String objectHelperBeanName =
			stringFormat (
				"%sObjectHelper",
				model.name ());

		String objectHelperClassName =
			stringFormat (
				"%s.model.%sObjectHelper",
				model.plugin ().packageName (),
				capitalise (model.name ()));

		Class<?> objectHelperClass =
			Class.forName (objectHelperClassName);

		applicationContext.registerBeanDefinition (
			new BeanDefinition ()
				.name (objectHelperBeanName)
				.beanClass (objectHelperClass)
				.scope ("singleton")
				.factoryClass (ObjectHelperFactory.class)

				.addValueProperty (
					"objectName",
					model.name ())

				.addValueProperty (
					"objectHelperClass",
					objectHelperClass));

		return 0;

	}

	int registerObjectHelperImplementation (
			@NonNull PluginModelSpec model)
		throws Exception {

		String objectHelperImplementationBeanName =
			stringFormat (
				"%sObjectHelperImplementation",
				model.name ());

		String objectHelperImplementationClassName =
			stringFormat (
				"%s.model.%sRec$%sObjectHelperImplementation",
				model.plugin ().packageName (),
				capitalise (model.name ()),
				capitalise (model.name ()));

		Class<?> objectHelperImplementationClass;

		try {

			objectHelperImplementationClass =
				Class.forName (objectHelperImplementationClassName);

		} catch (ClassNotFoundException exception) {

			/*
			log.warn (sf (
				"No object helper implementation for %s.%s.%s",
				model.project ().packageName (),
				model.plugin ().packageName (),
				model.name ()));
			*/

			return 0;

		}

		applicationContext.registerBeanDefinition (
			new BeanDefinition ()
				.name (objectHelperImplementationBeanName)
				.beanClass (objectHelperImplementationClass)
				.scope ("singleton"));

		return 0;

	}

	int registerDaoHibernate (
			@NonNull PluginModelSpec pluginModelSpec)
		throws Exception {

		String daoBeanName =
			stringFormat (
				"%sDao",
				pluginModelSpec.name ());

		String daoClassName =
			stringFormat (
				"%s.model.%sDao",
				pluginModelSpec.plugin ().packageName (),
				capitalise (pluginModelSpec.name ()));

		try {

			Class.forName (
				daoClassName);

		} catch (ClassNotFoundException exception) {

			return 0;

		}

		String daoHibernateClassName =
			stringFormat (
				"%s.hibernate.%sDaoHibernate",
				pluginModelSpec.plugin ().packageName (),
				capitalise (pluginModelSpec.name ()));

		Class<?> daoHibernateClass;

		try {

			daoHibernateClass =
				Class.forName (
					daoHibernateClassName);

		} catch (ClassNotFoundException exception) {

			return 0;

		}

		applicationContext.registerBeanDefinition (
			new BeanDefinition ()

			.name (
				daoBeanName)

			.beanClass (
				daoHibernateClass)

			.scope (
				"singleton")

		);

		return 0;

	}

	int registerObjectHelperProvider (
			@NonNull PluginModelSpec model)
		throws Exception {

		String objectHooksBeanName =
			stringFormat (
				"%sHooks",
				model.name ());

		String objectHelperProviderBeanName =
			stringFormat (
				"%sObjectHelperProvider",
				model.name ());

		applicationContext.registerBeanDefinition (
			new BeanDefinition ()

			.name (
				objectHelperProviderBeanName)

			.beanClass (
				ObjectHelperProvider.class)

			.scope (
				"singleton")

			.factoryClass (
				ObjectHelperProviderFactory.class)

			.addValueProperty (
				"objectName",
				model.name ())

			.addReferenceProperty (
				"objectHooks",
				objectHooksBeanName)

		);

		return 0;

	}

	/*
	int registerConsoleHelperProvider (
			ProjectModelSpec model)
		throws Exception {

		String consoleHelperProviderBeanName =
			sf ("%sConsoleHelperProvider",
				model.name ());

		String consoleHelperProviderClassName =
			sf ("%s.%s.console.%sConsoleHelper$%sConsoleHelperProvider",
				model.project ().packageName (),
				model.plugin ().packageName (),
				capitalise (model.name ()),
				capitalise (model.name ()));

		Class<?> consoleHelperProviderClass;

		try {

			consoleHelperProviderClass =
				Class.forName (consoleHelperProviderClassName);

		} catch (ClassNotFoundException exception) {

			return 0;

		}

		applicationContext.registerBeanDefinition (
			new BeanDefinition ()
				.name (consoleHelperProviderBeanName)
				.beanClass (consoleHelperProviderClass)
				.scope ("singleton"));

		return 0;

	}
	*/

	int registerConsoleHelper (
			@NonNull PluginModelSpec model)
		throws Exception {

		String objectHelperBeanName =
			stringFormat (
				"%sObjectHelper",
				model.name ());

		/*
		String consoleHelperProviderBeanName =
			sf ("%sConsoleHelperProvider",
				model.name ());
		*/

		String consoleHelperBeanName =
			stringFormat (
				"%sConsoleHelper",
				model.name ());

		String consoleHelperClassName =
			stringFormat (
				"%s.console.%sConsoleHelper",
				model.plugin ().packageName (),
				capitalise (model.name ()));

		Class<?> consoleHelperClass;

		try {

			consoleHelperClass =
				Class.forName (consoleHelperClassName);

		} catch (ClassNotFoundException exception) {

			log.error (
				stringFormat (
					"No such class %s",
					consoleHelperClassName));

			return 1;

		}

		applicationContext.registerBeanDefinition (
			new BeanDefinition ()

			.name (
				consoleHelperBeanName)

			.beanClass (
				consoleHelperClass)

			.factoryClass (
				ConsoleHelperFactory.class)

			.scope (
				"singleton")

			.addReferenceProperty (
				"objectHelper",
				objectHelperBeanName)

			/*
			.addReferenceProperty (
				"consoleHelperProvider",
				consoleHelperProviderBeanName)
			*/

			.addValueProperty (
				"consoleHelperClass",
				consoleHelperClass)

		);

		return 0;

	}

	int registerEnumConsoleHelper (
			@NonNull PluginEnumTypeSpec enumType)
		throws Exception {

		String enumClassName =
			stringFormat (
				"%s.model.%s",
				enumType.plugin ().packageName (),
				capitalise (enumType.name ()));

		Class<?> enumClass;

		try {

			enumClass =
				Class.forName (
					enumClassName);

		} catch (ClassNotFoundException exception) {

			log.error (
				stringFormat (
					"No such class %s",
					enumClassName));

			return 1;

		}

		String enumConsoleHelperBeanName =
			stringFormat (
				"%sConsoleHelper",
				enumType.name ());

		applicationContext.registerBeanDefinition (
			new BeanDefinition ()

			.name (
				enumConsoleHelperBeanName)

			.beanClass (
				EnumConsoleHelper.class)

			.factoryClass (
				EnumConsoleHelperFactory.class)

			.scope (
				"singleton")

			.addValueProperty (
				"enumClass",
				enumClass)

		);

		return 0;

	}

	int registerConfigBeans () {

		for (String configName
				: configNames) {

			log.info (
				stringFormat (
					"Loading configuration %s",
					configName));

			String configPath =
				stringFormat (
					"conf/%s-config-beans.xml",
					configName);

			applicationContext.registerXmlFilename (
				configPath);

		}

		return 0;

	}

	int registerSingletonBeans () {

		for (Map.Entry<String,Object> entry
				: singletonBeans.entrySet ()) {

			applicationContext.registerSingleton (
					entry.getKey (),
					entry.getValue ());

		}

		return 0;

	}

	void createApplicationContext () {

		applicationContext =
			new ApplicationContext ()
				.outputPath (outputPath);

	}

	public
	ApplicationContextBuilder registerBeanDefinition (
			@NonNull BeanDefinition beanDefinition) {

		beanDefinitionsToRegister.add (
			beanDefinition);

		return this;

	}

}
