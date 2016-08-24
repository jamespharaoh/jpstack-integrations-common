package wbs.framework.data.tools;

import static wbs.framework.utils.etc.LogicUtils.parseBooleanYesNo;
import static wbs.framework.utils.etc.Misc.contains;
import static wbs.framework.utils.etc.Misc.isNotEmpty;
import static wbs.framework.utils.etc.Misc.isNull;
import static wbs.framework.utils.etc.Misc.toEnumGeneric;
import static wbs.framework.utils.etc.NullUtils.ifNull;
import static wbs.framework.utils.etc.NumberUtils.moreThanZero;
import static wbs.framework.utils.etc.NumberUtils.parseIntegerRequired;
import static wbs.framework.utils.etc.OptionalUtils.optionalOrNull;
import static wbs.framework.utils.etc.StringUtils.camelToHyphen;
import static wbs.framework.utils.etc.StringUtils.hyphenToCamel;
import static wbs.framework.utils.etc.StringUtils.joinWithCommaAndSpace;
import static wbs.framework.utils.etc.StringUtils.joinWithFullStop;
import static wbs.framework.utils.etc.StringUtils.nullIfEmptyString;
import static wbs.framework.utils.etc.StringUtils.stringFormat;
import static wbs.framework.utils.etc.StringUtils.uncapitalise;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.inject.Provider;

import org.apache.commons.lang3.tuple.Pair;
import org.dom4j.Attribute;
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.Element;
import org.dom4j.io.SAXReader;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Iterables;

import lombok.Data;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import lombok.experimental.Accessors;
import lombok.extern.log4j.Log4j;
import wbs.framework.data.annotations.DataAncestor;
import wbs.framework.data.annotations.DataAttribute;
import wbs.framework.data.annotations.DataChild;
import wbs.framework.data.annotations.DataChildren;
import wbs.framework.data.annotations.DataChildrenIndex;
import wbs.framework.data.annotations.DataContent;
import wbs.framework.data.annotations.DataIgnore;
import wbs.framework.data.annotations.DataInitMethod;
import wbs.framework.data.annotations.DataParent;
import wbs.framework.logging.TaskLogger;
import wbs.framework.utils.etc.BeanLogic;

/**
 * Automatically builds data objects from XML guided by annotations.
 *
 * TODO separate registry which builds worker
 * TODO read annotations first for efficiency
 */
@Accessors (fluent = true)
@Log4j
public
class DataFromXmlImplementation
	implements DataFromXml {

	// properties

	@Getter @Setter
	Map <String, List <DataClassInfo>> dataClassesMap;

	@Getter @Setter
	Map <String, Map <String, ?>> namedObjectCollections;

	// implementation

	@Override
	public
	Object readInputStream (
			@NonNull InputStream inputStream,
			@NonNull String filename,
			@NonNull List <Object> parents) {

		TaskLogger taskLogger =
			new TaskLogger (
				log);

		taskLogger.firstErrorFormat (
			"Error reading %s from filesystem",
			filename);

		SAXReader saxReader =
			new SAXReader ();

		Document document;

		try {

			document =
				saxReader.read (
					inputStream,
					filename);

		} catch (DocumentException exception) {

			taskLogger.errorFormatException (
				exception,
				"Error parsing XML");

			throw taskLogger.makeException ();

		}

		Object result;

		try {

			result =
				new ElementBuilder ()

				.taskLogger (
					taskLogger)

				.element (
					document.getRootElement ())

				.parents (
					parents)

				.context (
					ImmutableList.of (
						document.getRootElement ().getName ()))

				.build ();

		} catch (Exception exception) {

			taskLogger.errorFormatException (
				exception,
				"Error building object tree from XML");

			throw taskLogger.makeException ();

		}

		taskLogger.makeException ();

		return result;

	}

	@Override
	public
	Object readClasspath (
			@NonNull String filename,
			@NonNull List <Object> parents) {

		InputStream inputStream =
			getClass ().getResourceAsStream (
				filename);

		if (inputStream == null) {

			throw new RuntimeException (
				stringFormat (
					"Classpath resource %s not found",
					filename));

		}

		return readInputStream (
			inputStream,
			filename,
			parents);

	}

	@Override
	public
	Object readFilename (
			@NonNull String filename,
			@NonNull List <Object> parents) {

		InputStream inputStream;

		try {

			inputStream =
				new FileInputStream (
					filename);

		} catch (FileNotFoundException fileNotFoundException) {

			throw new RuntimeException (
				stringFormat (
					"File %s not found",
					filename));

		}

		return readInputStream (
			inputStream,
			filename,
			parents);

	}

	@Accessors (fluent = true)
	class ElementBuilder {

		@Getter @Setter
		TaskLogger taskLogger;

		@Getter @Setter
		Element element;

		@Getter @Setter
		Iterable <Object> parents;

		@Getter @Setter
		Iterable <String> context;

		Object object;

		Set <String> matchedAttributes =
			new HashSet<> ();

		Set <String> matchedElementNames =
			new HashSet<> ();

		Object build () {

			Iterator <Object> parentsIterator =
				parents.iterator ();

			Class <?> parentClass =
				parentsIterator.hasNext ()
					? parentsIterator.next ().getClass ()
					: Object.class;

			// find the appropriate builder

			List <DataClassInfo> dataClassInfosForElementName =
				dataClassesMap.get (
					element.getName ());

			if (dataClassInfosForElementName == null) {

				taskLogger.errorFormat (
					"%s: ",
					joinWithFullStop (
						context),
					"Don't know how to map <%s>",
					element.getName ());

				return null;

			}

			List<DataClassInfo> matchingDataClassInfos =
				new ArrayList<DataClassInfo> ();

			for (
				DataClassInfo dataClassInfo :
					dataClassInfosForElementName
			) {

				if (! dataClassInfo.parentClass.isAssignableFrom (
						parentClass))
					continue;

				matchingDataClassInfos.add (
					dataClassInfo);

			}

			if (matchingDataClassInfos.isEmpty ()) {

				taskLogger.errorFormat (
					"%s: ",
					joinWithFullStop (
						context),
					"Don't know how to map <%s> with parent %s",
					element.getName (),
					parentClass.getName ());

				return null;

			}

			if (matchingDataClassInfos.size () > 1) {

				List <String> matchingDataClassNames =
					new ArrayList<> ();

				for (DataClassInfo matchingDataClassInfo
						: matchingDataClassInfos) {

					matchingDataClassNames.add (
						matchingDataClassInfo.dataClass ().getName ());

				}

				throw new RuntimeException (
					stringFormat (
						"%s: ",
						joinWithFullStop (
							context),
						"Multiple mappings for <%s> with parent %s: %s",
						element.getName (),
						parentClass.getName (),
						joinWithCommaAndSpace (
							matchingDataClassNames)));

			}

			Provider <?> builder =
				matchingDataClassInfos.get (0).provider ();

			// build it

			object =
				builder.get ();

			for (
				Field field
					: object.getClass ().getDeclaredFields ()
			) {

				buildField (
					field);

			}

			// check for unmatched attributes

			for (
				Object attributeObject
					: element.attributes ()
			) {

				Attribute attribute =
					(Attribute)
					attributeObject;

				String attributeName =
					attribute.getName ();

				if (matchedAttributes.contains (
						attributeName))
					continue;

				throw new RuntimeException (
					stringFormat (
						"%s: ",
						joinWithFullStop (
							context),
						"Don't understand attribute %s on <%s>, using %s",
						attributeName,
						element.getName (),
						object.getClass ().getSimpleName ()));

			}

			// check for unmatched elements

			long unmatchedElementCount = 0;

			for (
				Object childElementObject
					: element.elements ()
			) {

				Element childElement =
					(Element) childElementObject;

				if (matchedElementNames.contains (
						childElement.getName ()))
					continue;

				taskLogger.errorFormat (
					"%s: ",
					joinWithFullStop (
						context),
					"Don't understand element <%s> in <%s>, using %s",
					childElement.getName (),
					element.getName (),
					object.getClass ().getSimpleName ());

				unmatchedElementCount ++;

			}

			if (
				moreThanZero (
					unmatchedElementCount)
			) {
				return null;
			}

			// run init method

			for (
				Method method
					: object.getClass ().getMethods ()
			) {

				DataInitMethod dataInitMethodAnnotation =
					method.getAnnotation (DataInitMethod.class);

				if (dataInitMethodAnnotation == null)
					continue;

				if (method.getParameterTypes ().length > 0)
					throw new RuntimeException ();

				try {

					method.invoke (
						object);

				} catch (Exception exception) {

					taskLogger.errorFormatException (
						exception,
						"%s: ",
						joinWithFullStop (
							context),
						"Error invoking data init method %s.%s",
						object.getClass ().getName (),
						method.getName ());

					return null;

				}

			}

			return object;

		}

		void buildField (
				Field field) {

			for (
				Annotation annotation
					: field.getAnnotations ()
			) {

				if (annotation instanceof DataAttribute) {

					buildAttributeField (
						field,
						(DataAttribute) annotation);

				}

				if (annotation instanceof DataContent) {

					buildContentField (
						field,
						(DataContent) annotation);

				}

				if (annotation instanceof DataChild) {

					buildChildField (
						field,
						(DataChild) annotation);

				}

				if (annotation instanceof DataChildren) {

					buildChildrenField (
						field,
						(DataChildren) annotation);

				}

				if (annotation instanceof DataChildrenIndex) {

					buildChildrenIndexField (
						field,
						(DataChildrenIndex) annotation);

				}

				if (annotation instanceof DataParent) {

					if (! parents.iterator ().hasNext ())
						throw new RuntimeException ();

					BeanLogic.set (
						object,
						field.getName (),
						parents.iterator ().next ());

				}

				if (annotation instanceof DataAncestor) {

					for (Object ancestor : parents) {

						if (! field.getType ().isInstance (ancestor))
							continue;

						BeanLogic.set (
							object,
							field.getName (),
							ancestor);

						break;

					}

				}

				if (annotation instanceof DataIgnore) {

					matchedElementNames.add (
						camelToHyphen (
							field.getName ()));

				}

			}

		}

		void buildAttributeField (
				@NonNull Field field,
				@NonNull DataAttribute dataAttributeAnnotation) {

			String attributeName =
				ifNull (
					nullIfEmptyString (
						dataAttributeAnnotation.name ()),
					camelToHyphen (
						field.getName ()));

			String attributeValue =
				element.attributeValue (
					attributeName);

			if (attributeValue == null) {

				if (dataAttributeAnnotation.required ()) {

					taskLogger.errorFormat (
						"%s: ",
						joinWithFullStop (
							context),
						"Missing required attribute '%s' of <%s>",
						attributeName,
						element.getName ());

				}

				return;

			}

			matchedAttributes.add (
				attributeName);

			if (! dataAttributeAnnotation.collection ().isEmpty ()) {

				Map <String, ?> collection =
					namedObjectCollections.get (
						dataAttributeAnnotation.collection ());

				if (collection == null) {

					taskLogger.errorFormat (
						"%s: ",
						joinWithFullStop (
							context),
						"Named collection %s doesn't exist for %s.%s",
						dataAttributeAnnotation.collection (),
						object.getClass ().getSimpleName (),
						field.getName ());

				}

				Object namedObject =
					collection.get (attributeValue);

				if (namedObject == null) {

					taskLogger.errorFormat (
						"%s: ",
						joinWithFullStop (
							context),
						"Named object %s not found in collection %s",
						attributeValue,
						dataAttributeAnnotation.collection ());

					return;

				}

				BeanLogic.set (
					object,
					field.getName (),
					namedObject);

			} else {

				setScalarFieldRequired (
					object,
					field,
					attributeValue);

			}

		}

		boolean tryToSetScalarField (
				@NonNull Object object,
				@NonNull Field field,
				@NonNull String stringValue) {

			if (field.getType () == String.class) {

				BeanLogic.set (
					object,
					field.getName (),
					stringValue);

				return true;

			} else if (field.getType () == Integer.class) {

				BeanLogic.set (
					object,
					field.getName (),
					Integer.parseInt (
						stringValue));

				return true;

			} else if (field.getType () == Long.class) {

				BeanLogic.set (
					object,
					field.getName (),
					Long.parseLong (
						stringValue));

				return true;

			} else if (field.getType () == Boolean.class) {

				BeanLogic.set (
					object,
					field.getName (),
					optionalOrNull (
						parseBooleanYesNo (
							stringValue)));

				return true;

			} else if (field.getType ().isEnum ()) {

				Enum<?> enumValue =
					toEnumGeneric (
						field.getType (),
						hyphenToCamel (
							stringValue));

				BeanLogic.set (
					object,
					field.getName (),
					enumValue);

				return true;

			} else {

				return false;

			}

		}

		void setScalarFieldRequired (
				@NonNull Object object,
				@NonNull Field field,
				@NonNull String stringValue) {

			if (
				! tryToSetScalarField (
					object,
					field,
					stringValue)
			) {

				taskLogger.errorFormat (
					"%s: ",
					joinWithFullStop (
						context),
					"Don't know how to map attribute to %s ",
					field.getType ().getName (),
					"at %s.%s",
					object.getClass ().getSimpleName (),
					field.getName ());

			}

		}

		void buildContentField (
				@NonNull Field field,
				@NonNull DataContent dataContentAnnotation) {

			String stringValue =
				element.getTextTrim ();

			if (field.getType () == String.class) {

				BeanLogic.set (
					object,
					field.getName (),
					stringValue);

			} else {

				taskLogger.errorFormat (
					"%s: ",
					joinWithFullStop (
						context),
					"Don't know how to map content to %s ",
					field.getType ().getName (),
					"at %s.%s",
					object.getClass ().getSimpleName (),
					field.getName ());

			}

		}

		void buildChildField (
				@NonNull Field field,
				@NonNull DataChild dataChildAnnotation) {

			String childElementName =
				ifNull (
					nullIfEmptyString (
						dataChildAnnotation.name ()),
					camelToHyphen (
						field.getName ()));

			Element childElement =
				element.element (
					childElementName);

			if (
				isNull (
					childElement)
			) {
				return;
			}

			matchedElementNames.add (
				childElementName);

			if (

				isNotEmpty (
					childElement.attributes ())

				|| isNotEmpty (
					childElement.elements ())

				|| ! tryToSetScalarField (
					object,
					field,
					childElement.getText ())

			) {

				Object nextParent =
					object;

				Object child =
					new ElementBuilder ()

					.taskLogger (
						taskLogger)

					.element (
						childElement)

					.parents (
						Iterables.concat (
							Collections.singletonList (
								nextParent),
							parents))

					.context (
						Iterables.concat (
							context,
							Collections.singletonList (
								childElement.getName ())))

					.build ();

				if (child != null) {

					BeanLogic.set (
						object,
						field.getName (),
						child);

				}

			}

		}

		void buildChildrenField (
				Field field,
				DataChildren dataChildrenAnnotation) {

			if (

				! dataChildrenAnnotation.direct ()

				&& ! dataChildrenAnnotation.childElement ().isEmpty ()

				&& (

					field.getType () != Map.class

					&& field.getType () != List.class

				)

			) {

				taskLogger.errorFormat (
					"%s: ",
					joinWithFullStop (
						context),
					"Don't specify childElement for indirect children, at ",
					"%s.%s",
					field.getDeclaringClass ().getSimpleName (),
					field.getName ());

				return;

			}

			// find the element which contains the children

			Element childrenElement;

			if (dataChildrenAnnotation.direct ()) {

				childrenElement =
					element;

				if (childrenElement == null)
					return;

			} else {

				String childrenElementName =
					! dataChildrenAnnotation.childrenElement ().isEmpty ()
						? dataChildrenAnnotation.childrenElement ()
						: camelToHyphen (
							field.getName ());

				List <?> childrenElementObjects =
					element.elements (
						childrenElementName);

				if (childrenElementObjects.isEmpty ())
					return;

				if (childrenElementObjects.size () > 1)
					throw new RuntimeException ();

				matchedElementNames.add (
					childrenElementName);

				childrenElement =
					(Element)
					childrenElementObjects.get (0);

			}

			// work out parents for recursive call

			Iterable <Object> nextParents;

			if (! dataChildrenAnnotation.surrogateParent ().isEmpty ()) {

				nextParents =
					Iterables.concat (
						ImmutableList.<Object> of (
							BeanLogic.get (
								object,
								dataChildrenAnnotation.surrogateParent ()),
							object),
						parents);

			} else {

				nextParents =
					Iterables.concat (
						Collections.singletonList (
							object),
						parents);

			}

			// collect children

			List <Object> children =
				new ArrayList<> ();

			List <?> childElementObjects =
				! dataChildrenAnnotation.childElement ().isEmpty ()
					? childrenElement.elements (
						dataChildrenAnnotation.childElement ())
					: childrenElement.elements ();

			Set <String> newlyMatchedElementNames =
				new HashSet<> ();

			for (
				Object childElementObject
					: childElementObjects
			) {

				Element childElement =
					(Element)
					childElementObject;

				if (dataChildrenAnnotation.direct ()) {

					if (
						contains (
							matchedElementNames,
							childElement.getName ())
					) {
						continue;
					}

					newlyMatchedElementNames.add (
						childElement.getName ());

				}

				if (field.getType () == Map.class) {

					String entryKey =
						childElement.attributeValue (
							dataChildrenAnnotation.keyAttribute ());

					String entryValue =
						childElement.attributeValue (
							dataChildrenAnnotation.valueAttribute ());

					if (entryKey == null) {

						taskLogger.errorFormat (
							"%s: ",
							joinWithFullStop (
								context),
							"Must specify 'keyAttribute' on @DataChildren ",
							"when field type is Map, at %s.%s",
							object.getClass ().getSimpleName (),
							field.getName ());

					}

					if (entryValue == null) {

						taskLogger.errorFormat (
							"%s: ",
							joinWithFullStop (
								context),
							"Must specify 'entryValue' on @DataChildren ",
							"when field type is Map, at %s.%s",
							object.getClass ().getSimpleName (),
							field.getName ());

					}

					if (

						isNull (							

							entryKey)

						|| isNull (
							entryValue)

					) {
						return;
					}

					children.add (
						Pair.of (
							entryKey,
							entryValue));

				} else if (
					field.getType () == List.class
					&& ! dataChildrenAnnotation.valueAttribute ().isEmpty ()
				) {

					Type genericType =
						field.getGenericType ();

					Class <?> itemClass;

					if (genericType instanceof ParameterizedType) {

						ParameterizedType parameterizedType =
							(ParameterizedType)
							genericType;

						itemClass =
							(Class <?>)
							parameterizedType.getActualTypeArguments () [0];

					} else {

						itemClass = null;

					}

					String stringValue =
						childElement.attributeValue (
							dataChildrenAnnotation.valueAttribute ());

					if (stringValue == null) {

						taskLogger.errorFormat (
							"%s: ",
							joinWithFullStop (
								context),
							"No attribute '%s' on <%s> ",
							dataChildrenAnnotation.valueAttribute (),
							childElement.getName (),
							"at %s.%s",
							object.getClass ().getSimpleName (),
							field.getName ());

						return;

					}

					Object value;

					if (itemClass == Integer.class) {

						value =
							Integer.parseInt (
								stringValue);

					} else if (itemClass == Long.class) {

						value =
							parseIntegerRequired (
								stringValue);

					} else if (itemClass == String.class) {

						value =
							stringValue;

					} else {

						taskLogger.errorFormat (
							"%s: ",
							joinWithFullStop (
								context),
							"Unable to map attribute type %s ",
							itemClass.getName (),
							"at %s.%s",
							object.getClass ().getSimpleName (),
							field.getName ());

						return;

					}

					children.add (
						value);

				} else {

					children.add (
						new ElementBuilder ()

						.taskLogger (
							taskLogger)

						.element (
							childElement)

						.parents (
							nextParents)

						.context (
							Iterables.concat (
								context,
								Collections.singletonList (
									childElement.getName ())))

						.build ());

				}

			}

			matchedElementNames.addAll (
				newlyMatchedElementNames);

			// set them

			if (field.getType () == Map.class) {

				ImmutableMap.Builder<Object,Object> mapBuilder =
					ImmutableMap.builder ();

				for (
					Object pairObject
						: children
				) {

					Pair<?,?> pair =
						(Pair<?,?>) pairObject;

					mapBuilder.put (
						pair.getLeft (),
						pair.getRight());

				}

				BeanLogic.set (
					object,
					field.getName (),
					mapBuilder.build ());

			} else {

				BeanLogic.set (
					object,
					field.getName (),
					children);

			}

		}

		void buildChildrenIndexField (
				Field field,
				DataChildrenIndex dataChildrenIndexAnnotation) {

			Matcher matcher =
				childrenIndexPattern.matcher (field.getName ());

			if (! matcher.matches ())
				throw new RuntimeException ();

			String childrenFieldName =
				matcher.group (1);

			String indexFieldName =
				uncapitalise (
					matcher.group (2));

			Map<Object,Object> childrenIndex =
				new LinkedHashMap<Object,Object> ();

			List<?> children =
				(List<?>)
				BeanLogic.get (
					object,
					childrenFieldName);

			for (Object child : children) {

				Object index =
					BeanLogic.get (
						child,
						indexFieldName);

				if (index == null)
					continue;

				childrenIndex.put (
					index,
					child);

			}

			BeanLogic.set (
				object,
				field.getName (),
				childrenIndex);

		}

	}

	static
	Pattern childrenIndexPattern =
		Pattern.compile ("(.+)By(.+)");

	@Accessors (fluent = true)
	@Data
	static
	class DataClassInfo {

		Class<?> parentClass;
		Class<?> dataClass;
		Provider<?> provider;

	}

}