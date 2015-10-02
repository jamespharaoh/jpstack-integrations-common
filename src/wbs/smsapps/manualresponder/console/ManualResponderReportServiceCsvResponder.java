package wbs.smsapps.manualresponder.console;

import static wbs.framework.utils.etc.Misc.stringFormat;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;

import org.joda.time.Interval;
import org.joda.time.LocalDate;

import wbs.framework.application.annotations.PrototypeComponent;
import wbs.platform.console.forms.FormFieldLogic;
import wbs.platform.console.forms.FormFieldSet;
import wbs.platform.console.module.ConsoleModule;
import wbs.platform.console.request.ConsoleRequestContext;
import wbs.platform.console.responder.ConsoleResponder;
import wbs.platform.user.model.UserObjectHelper;
import wbs.smsapps.manualresponder.console.ManualResponderReportSimplePart.SearchForm;
import wbs.smsapps.manualresponder.model.ManualResponderObjectHelper;
import wbs.smsapps.manualresponder.model.ManualResponderReportObjectHelper;
import wbs.smsapps.manualresponder.model.ManualResponderReportRec;

@PrototypeComponent ("manualResponderReportServiceCsvResponder")
public
class ManualResponderReportServiceCsvResponder
	extends ConsoleResponder {

	// dependencies

	@Inject
	FormFieldLogic formFieldLogic;

	@Inject
	ManualResponderObjectHelper manualResponderHelper;

	@Inject @Named
	ConsoleModule manualResponderReportConsoleModule;

	@Inject
	ManualResponderReportObjectHelper manualResponderReportHelper;

	@Inject
	ConsoleRequestContext requestContext;

	@Inject
	UserObjectHelper userHelper;

	// state

	PrintWriter out;

	List<ManualResponderReportRec> reports;
	FormFieldSet searchFormFieldSet;
	FormFieldSet resultsFormFieldSet;
	SearchForm searchForm;

	// implementation

	@Override
	protected
	void setup ()
		throws IOException {

		out =
			requestContext.writer ();

	}

	@Override
	public
	void prepare () {

		searchFormFieldSet =
			manualResponderReportConsoleModule.formFieldSets ().get (
				"simpleReportSearch");

		resultsFormFieldSet =
			manualResponderReportConsoleModule.formFieldSets ().get (
				"simpleReportCsv");

		LocalDate today =
			LocalDate.now ();

		Interval todayInterval =
			today.toInterval ();

		searchForm =
			new SearchForm ()

			.start (
				todayInterval.getStart ().toInstant ())

			.end (
				todayInterval.getEnd ().toInstant ());

		formFieldLogic.update (
			searchFormFieldSet,
			searchForm);

		reports =
			manualResponderReportHelper.findByProcessedTime (
				new Interval (
					searchForm.start (),
					searchForm.end ()));

	}

	@Override
	public
	void goHeaders () {

		requestContext.setHeader (
			"Content-Type",
			"text/csv");

		requestContext.setHeader (
			"Content-Disposition",
			"attachment;filename=report.csv");

	}

	@Override
	public
	void goContent ()
		throws IOException {

		List<Integer> users =
			new ArrayList<Integer> ();

		List<Long> numCount =
			new ArrayList<Long> ();

		for (
			ManualResponderReportRec report
				: reports
		) {

			Integer userId =
				report.getUser ().getId ();

			Integer num =
				report.getNum ();

			if (users.contains (userId)){

				Integer index =
					users.indexOf (userId);

				numCount.set (
					index,
					num + numCount.get (index));

			} else {

				users.add (
					userId);

				numCount.add (
					new Long (num));

			}

		}

		out.write (
			stringFormat (
				"\"Operator\",",
				"\"Count\"\n"));

		for (
			Integer user
				: users
		) {

			out.write (
				stringFormat (
					"\"%s\",",
					userHelper.find (user).getFullname ()));

			out.write (
				stringFormat (
					"\"%s\"\n",
					numCount.get (
						users.indexOf (user)
					).toString ()));

		}

		out.flush ();

	}

}