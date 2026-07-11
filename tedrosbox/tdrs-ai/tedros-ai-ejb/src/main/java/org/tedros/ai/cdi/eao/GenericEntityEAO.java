package org.tedros.ai.cdi.eao;

import org.tedros.server.cdi.eao.TGenericEAO;
import org.tedros.server.entity.ITEntity;

import jakarta.enterprise.context.Dependent;

/**
 * The generic entity access object.
 * 
 * This use a Dependent context. 
 * 
 * @author Davis Dun
 *
 */
@Dependent
public class GenericEntityEAO<E extends ITEntity> extends TGenericEAO<E> {

}
