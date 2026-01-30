/**
 * TEDROS  
 * 
 * TODOS OS DIREITOS RESERVADOS
 * 10/01/2014
 */
package org.tedros.fx.builder;

import java.lang.annotation.Annotation;

import org.apache.commons.lang3.ArrayUtils;
import org.tedros.fx.annotation.control.TShowField.TField;
import org.tedros.fx.control.TShowField;
import org.tedros.fx.domain.TLayoutType;
import org.tedros.util.TLoggerUtil;

import javafx.beans.Observable;


/**
 * DESCRIÇÃO DA CLASSE
 *
 * @author Davis Gordon
 *
 */
public class TShowFieldBuilder 
extends TBuilder
implements ITControlBuilder<TShowField, Observable> {

	
	public TShowField build(final Annotation annotation, final Observable attrProperty) throws Exception {
		org.tedros.fx.annotation.control.TShowField tAnnotation = 
				(org.tedros.fx.annotation.control.TShowField) annotation;
		
		TLayoutType layout = tAnnotation.layout();
		TField[] tfs = tAnnotation.fields();
		org.tedros.fx.control.TField[] fields = new org.tedros.fx.control.TField[0];
		for(TField f : tfs) {
			org.tedros.fx.control.TField v = 
					new org.tedros.fx.control.TField(f.name(), f.mask(), f.format(), 
							f.dateStyle(), f.timeStyle(), f.label(), f.labelPosition(), f.converter(), f.renderHtml());
			fields = ArrayUtils.add(fields, v);
		}
		try {
			TShowField control = new TShowField(layout, attrProperty, super.getComponentDescriptor(), fields);
			
			callParser(tAnnotation, control);
			return control;
		}catch (Exception e) {
			TLoggerUtil.error(this.getClass(), "Erro no field: " + super.getComponentDescriptor().getFieldDescriptor().getFieldName(), e);
			return null;
		}
	}
}
