package wbs.platform.console.metamodule;

import java.util.List;

import lombok.Data;
import lombok.ToString;
import lombok.experimental.Accessors;
import wbs.framework.application.annotations.PrototypeComponent;

@Accessors (fluent = true)
@Data
@PrototypeComponent ("resolvedConsoleContextLink")
@ToString
public
class ResolvedConsoleContextLink {

	String name;
	String localName;

	String tabName;
	String tabLocation;
	String tabLabel;
	String tabPrivKey;
	List<String> tabContextTypeNames;

	List<String> parentContextNames;

	List<String> contextNamePrefixes;

}
