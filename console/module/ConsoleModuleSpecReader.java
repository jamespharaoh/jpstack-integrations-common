package wbs.platform.console.module;

import java.util.Map;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import javax.inject.Provider;

import lombok.NonNull;
import lombok.experimental.Accessors;
import wbs.framework.application.annotations.SingletonComponent;
import wbs.framework.data.tools.DataFromXml;

import com.google.common.collect.ImmutableList;

@Accessors (fluent = true)
@SingletonComponent ("consoleModuleSpecReader")
public
class ConsoleModuleSpecReader {

	@Inject
	@ConsoleModuleData
	Map<Class<?>,Provider<ConsoleModuleSpec>> consoleModuleSpecProviders;

	DataFromXml dataFromXml;

	@PostConstruct
	public
	void init () {

		dataFromXml =
			new DataFromXml ();

		for (Map.Entry<Class<?>,Provider<ConsoleModuleSpec>> entry
				: consoleModuleSpecProviders.entrySet ()) {

			dataFromXml.registerBuilder (
				entry.getKey (),
				entry.getValue ());

		}

	}

	public
	ConsoleModuleSpec readClasspath (
			@NonNull String xmlResourceName) {

		ConsoleModuleSpec consoleSpec =
			(ConsoleModuleSpec)
			dataFromXml.readClasspath (
				ImmutableList.of (),
				xmlResourceName);

		return consoleSpec;

	}

}
