package org.tedros.ai.cdi.bo;

import org.tedros.ai.cdi.eao.GenericEntityEAO;
import org.tedros.server.cdi.bo.TGenericBO;
import org.tedros.server.entity.ITEntity;

import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;

@Dependent
public class GenericEntityBO<E extends ITEntity> extends TGenericBO<E> {

	@Inject
	private GenericEntityEAO<E> eao;
	
	@Override
	public GenericEntityEAO<E> getEao() {
		return eao;
	}

}
