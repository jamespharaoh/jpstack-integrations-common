package wbs.utils.random;

import static wbs.utils.etc.NumberUtils.toJavaIntegerRequired;
import static wbs.utils.string.StringUtils.joinWithoutSeparator;

import java.util.List;
import java.util.Random;

import wbs.framework.component.annotations.NormalLifecycleSetup;
import wbs.framework.component.annotations.SingletonComponent;

@SingletonComponent ("randomLogic")
public
class RandomLogicImplementation
	implements RandomLogic {

	// state

	Random random;

	// life cycle

	@NormalLifecycleSetup
	public
	void setup () {

		random =
			new Random ();

	}

	// implementation

	@Override
	public
	int randomJavaInteger (
			int limit) {

		return random.nextInt (
			limit);

	}

	@Override
	public
	long randomInteger (
			long limit) {

		if (limit < 0) {

			throw new IllegalArgumentException ();

		} else if (limit == Long.MAX_VALUE) {

			return random.nextLong ();

		} else if (limit <= Integer.MAX_VALUE) {

			return random.nextInt (
				toJavaIntegerRequired (
					limit));

		} else {

			throw new RuntimeException (
				"TODO");

		}

	}

	@Override
	public
	String generateString (
			String chars,
			int length) {

		StringBuilder stringBuilder =
			new StringBuilder ();

		for (int i = 0; i < length; i ++) {

			stringBuilder.append (
				chars.charAt (
					randomJavaInteger (
						chars.length ())));

		}

		return stringBuilder.toString ();

	}

	@Override
	public
	String generateLowercase (
			int length) {

		return generateString (
			"abcdefghijklmnopqrstuvwxyz",
			length);

	}

	@Override
	public
	String generateUppercase (
			int length) {

		return generateString (
			"ABCDEFGHIJKLMNOPQRSTUVWXYZ",
			length);

	}

	@Override
	public
	String generateNumeric (
			int length) {

		return generateString (
			"0123456789",
			length);

	}

	@Override
	public
	String generateNumericNoZero (
			int length) {

		return joinWithoutSeparator (
			generateString ("123456789", 1),
			generateNumeric (length - 1));

	}

	@Override
	public <Type>
	Type sample (
			Type[] options) {

		int index =
			randomJavaInteger (
				options.length);

		return options [
			index];

	}

	@Override
	public <Type>
	Type sample (
			List<Type> options) {

		if (options.isEmpty ()) {

			throw new IllegalArgumentException (
				"Options cannot be empty");

		}

		int index =
			randomJavaInteger (
				options.size ());

		return options.get (
			index);

	}

}