package wbs.web.responder;

import java.io.IOException;
import java.io.PrintWriter;

import javax.inject.Provider;

import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import lombok.experimental.Accessors;

import org.json.simple.JSONValue;

import wbs.framework.component.annotations.PrototypeComponent;
import wbs.framework.component.annotations.SingletonDependency;
import wbs.framework.data.tools.DataToJson;
import wbs.framework.logging.TaskLogger;

import wbs.web.context.RequestContext;

@Accessors (fluent = true)
@PrototypeComponent ("jsonResponder")
public
class JsonResponder
	implements
		Provider <Responder>,
		Responder {

	// singleton dependencies

	@SingletonDependency
	RequestContext requestContext;

	// properties

	@Getter @Setter
	Object value;

	// implementation

	@Override
	public
	void execute (
			@NonNull TaskLogger parentTaskLogger)
		throws IOException {

		requestContext.setHeader (
			"Content-Type",
			"application/json");

		PrintWriter out =
			requestContext.printWriter ();

		DataToJson dataToJson =
			new DataToJson ();

		Object jsonValue =
			dataToJson.toJson (
				value);

		JSONValue.writeJSONString (
			jsonValue,
			out);

		out.println ();

	}

	@Override
	public
	Responder get () {

		return this;

	}

}
