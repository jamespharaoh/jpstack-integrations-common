package wbs.sms.message.stats.hibernate;

import java.util.List;

import lombok.NonNull;

import org.hibernate.Criteria;
import org.hibernate.criterion.ProjectionList;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.hibernate.type.CustomType;
import org.hibernate.type.Type;
import org.jadira.usertype.dateandtime.joda.PersistentLocalDate;

import com.google.common.collect.ImmutableList;

import wbs.framework.hibernate.AliasToBeanNestedResultTransformer;
import wbs.framework.hibernate.HibernateDao;
import wbs.sms.message.stats.model.MessageStatsDao;
import wbs.sms.message.stats.model.MessageStatsRec;
import wbs.sms.message.stats.model.MessageStatsSearch;

public
class MessageStatsDaoHibernate
	extends HibernateDao
	implements MessageStatsDao {

	@Override
	public
	List<MessageStatsRec> search (
			@NonNull MessageStatsSearch search) {

		Criteria criteria =
			createCriteria (
				MessageStatsRec.class,
				"_messageStats");

		if (search.dateAfter () != null) {

			criteria.add (
				Restrictions.ge (
					"messageStatsId.date",
					search.dateAfter ()));

		}

		if (search.dateBefore () != null) {

			criteria.add (
				Restrictions.lt (
					"messageStatsId.date",
					search.dateBefore ()));

		}

		if (search.routeIdIn () != null) {

			criteria.add (
				Restrictions.in (
					"messageStatsId.route.id",
					search.routeIdIn ()));

		}

		if (search.serviceIdIn () != null) {

			criteria.add (
				Restrictions.in (
					"messageStatsId.service.id",
					search.serviceIdIn ()));

		}

		if (search.affiliateIdIn () != null) {

			criteria.add (
				Restrictions.in (
					"messageStatsId.affiliate.id",
					search.affiliateIdIn ()));

		}

		if (search.batchIdIn () != null) {

			criteria.add (
				Restrictions.in (
					"messageStatsId.batch.id",
					search.batchIdIn ()));

		}

		if (search.networkIdIn () != null) {

			criteria.add (
				Restrictions.in (
					"messageStatsId.network.id",
					search.networkIdIn ()));

		}

		if (search.filter ()) {

			criteria.add (
				Restrictions.or (

				Restrictions.in (
					"messageStatsId.service.id",
					search.filterServiceIds ()),

				Restrictions.in (
					"messageStatsId.affiliate.id",
					search.filterAffiliateIds ()),

				Restrictions.in (
					"messageStatsId.route.id",
					search.filterRouteIds ())));

		}

		if (search.group ()) {

			ProjectionList projectionList =
				Projections.projectionList ();

			ImmutableList.<String>of (
				"inTotal",
				"outTotal",
				"outPending",
				"outCancelled",
				"outFailed",
				"outSent",
				"outSubmitted",
				"outDelivered",
				"outUndelivered",
				"outReportTimedOut",
				"outHeld",
				"outBlacklisted",
				"outManuallyUndelivered"
			).forEach (
				fieldName ->
					projectionList.add (
						Projections.sum (
							"_messageStats.stats." + fieldName),
						"stats." + fieldName)
			);

			if (search.groupByDate ()) {

				projectionList.add (
					Projections.groupProperty (
						"_messageStats.messageStatsId.date"),
					"messageStatsId.date");

			}

			if (search.groupByMonth ()) {

				projectionList.add (
					Projections.sqlGroupProjection (
						"date_trunc ('month', {alias}.date)::date AS date",
						"date_trunc ('month', {alias}.date)::date",
						new String [] {
							"date"
						},
						new Type [] {
							new CustomType (
								new PersistentLocalDate ()),
						}),
					"messageStatsId.date");

			}

			if (search.groupByAffiliate ()) {

				projectionList.add (
					Projections.groupProperty (
						"_messageStats.messageStatsId.affiliate"),
					"messageStatsId.affiliate");

			}

			if (search.groupByBatch ()) {

				projectionList.add (
					Projections.groupProperty (
						"_messageStats.messageStatsId.batch"),
					"messageStatsId.batch");

			}

			if (search.groupByNetwork ()) {

				projectionList.add (
					Projections.groupProperty (
						"_messageStats.messageStatsId.network"),
					"messageStatsId.network");

			}

			if (search.groupByRoute ()) {

				projectionList.add (
					Projections.groupProperty (
						"_messageStats.messageStatsId.route"),
					"messageStatsId.route");

			}

			if (search.groupByService ()) {

				projectionList.add (
					Projections.groupProperty (
						"_messageStats.messageStatsId.service"),
					"messageStatsId.service");

			}

			criteria.setProjection (
				projectionList);

			criteria.setResultTransformer (
				new AliasToBeanNestedResultTransformer (
					MessageStatsRec.class));

		}

		return findMany (
			MessageStatsRec.class,
			criteria.list ());

	}

}
