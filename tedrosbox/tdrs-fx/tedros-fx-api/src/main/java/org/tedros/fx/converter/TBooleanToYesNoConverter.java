package org.tedros.fx.converter;

import org.tedros.core.TLanguage;
import org.tedros.fx.TUsualKey;

public class TBooleanToYesNoConverter extends TConverter<Boolean, String>{
	
	@Override
	public String getOut() {
		Boolean value = getIn();
		if(value!=null && value) {
			return TLanguage.getInstance().getString(TUsualKey.YES);
		}
		return TLanguage.getInstance().getString(TUsualKey.NO);
	}

}
