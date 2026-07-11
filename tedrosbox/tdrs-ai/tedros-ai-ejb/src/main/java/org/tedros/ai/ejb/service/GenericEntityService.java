package org.tedros.ai.ejb.service;

import org.tedros.ai.cdi.bo.GenericEntityBO;
import org.tedros.server.ejb.service.TEjbService;
import org.tedros.server.entity.ITEntity;

import jakarta.ejb.LocalBean;
import jakarta.ejb.Stateless;
import jakarta.ejb.TransactionAttribute;
import jakarta.ejb.TransactionAttributeType;
import jakarta.inject.Inject;

@LocalBean
@Stateless(name="GenericEntityService")
@TransactionAttribute(value = TransactionAttributeType.NOT_SUPPORTED)
public class GenericEntityService<E extends ITEntity> extends TEjbService<E>  {
	
	@Inject
	private GenericEntityBO<E> bo;
	
	@Override
	public GenericEntityBO<E> getBussinesObject() {
		return bo;
	}
	

}
