package org.tedros.fx.annotation.parser;

import org.tedros.fx.annotation.control.TAutoCompleteEntity;
import org.tedros.fx.annotation.parser.engine.TAnnotationParser;

public class TAutoCompleteEntityParser extends TAnnotationParser<TAutoCompleteEntity, org.tedros.fx.control.TAutoCompleteEntity> {
	
	@Override
	public void parse(TAutoCompleteEntity annotation, org.tedros.fx.control.TAutoCompleteEntity object, String... byPass)
			throws Exception {
		super.parse(annotation, object, "maxLength","required","query","service","modelViewType","startSearchAt");
	}

}
