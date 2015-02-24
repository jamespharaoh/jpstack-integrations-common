package wbs.platform.reporting.console;

import static wbs.framework.utils.etc.Misc.stringFormat;

import java.io.IOException;
import java.io.Writer;
import java.util.List;
import java.util.Map;
import java.util.Set;

import lombok.NonNull;
import lombok.SneakyThrows;

import org.apache.commons.lang3.tuple.ImmutablePair;
import org.joda.time.DateTime;
import org.joda.time.Instant;

import wbs.framework.application.annotations.SingletonComponent;

@SingletonComponent ("statsConsoleLogic")
public
class StatsConsoleLogic {

	@SneakyThrows (IOException.class)
	public
	void outputGroup (
			@NonNull Writer out,
			@NonNull Map<String,StatsDataSet> dataSetsByName,
			@NonNull StatsPeriod period,
			@NonNull StatsGrouper grouper,
			@NonNull StatsResolver resolver,
			@NonNull StatsFormatter formatter) {

		// aggregate stats via resolver

		Set<Object> groups =
			resolver.getGroups (
				dataSetsByName,
				grouper);

		ResolvedStats resolved =
			resolver.resolve (
				dataSetsByName,
				period,
				groups);

		List<Object> sortedGroups =
			grouper.sortGroups (groups);

		// output

		for (
			Object group
				: sortedGroups
		) {

			out.write (
				stringFormat (
					"<tr>\n"));

			out.write (
				stringFormat (
					"%s\n",
					grouper.tdForGroup (group)));

			for (
				int step = 0;
				step < period.size ();
				step ++
			) {

				Object combinedValue =
					resolved.steps ().get (
						new ImmutablePair<Object,Instant> (
							group,
							period.step (step)));

				out.write (
					formatter.format (
						group,
						Integer.toString (
							step +
							period.offset ()),
						combinedValue));

			}

			Object totalValue =
				resolved.totals ().get (
					group);

			out.write (
				formatter.formatTotal (
					group,
					totalValue));

			out.write (
				stringFormat (
					"</tr>\n"));

		}

	}

	public
	StatsPeriod createStatsPeriod (
			StatsGranularity granularity,
			Instant startTime,
			Instant endTime,
			Integer offset) {

		StatsPeriod ret =
			new StatsPeriod ()
				.granularity (granularity)
				.startTime (startTime)
				.endTime (endTime)
				.offset (offset);

		for (
			DateTime hour = startTime.toDateTime ();
			hour.isBefore (endTime);
			hour = hour.plusHours (1)
		) {

			ret.steps ().add (
				hour.toInstant ());

		}

		return ret;

	}

}
