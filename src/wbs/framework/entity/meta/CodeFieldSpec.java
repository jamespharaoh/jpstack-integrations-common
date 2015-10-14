package wbs.framework.entity.meta;

import lombok.Data;
import lombok.experimental.Accessors;
import wbs.framework.application.annotations.PrototypeComponent;
import wbs.framework.data.annotations.DataAttribute;
import wbs.framework.data.annotations.DataClass;

@Accessors (fluent = true)
@Data
@DataClass ("code-field")
@PrototypeComponent ("codeFieldSpec")
@ModelMetaData
public
class CodeFieldSpec
	implements ModelFieldSpec {

	@DataAttribute
	String name;

}