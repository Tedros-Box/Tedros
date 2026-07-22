/**
 * 
 */
package org.tedros.core.cdi.eao;

import java.util.Optional;

import org.tedros.common.model.TDomainPropertie;
import org.tedros.server.cdi.eao.TGenericEAO;

import jakarta.enterprise.context.RequestScoped;
import jakarta.persistence.NoResultException;
import jakarta.persistence.Query;

/**
 * @author Davis Gordon
 *
 */
@RequestScoped
public class TDomainPropertieEao extends TGenericEAO<TDomainPropertie> {
	
	public boolean exist(String key){
		Query qry = getEntityManager().createQuery("select e.id from TDomainPropertie e where e.key = :k");
		qry.setParameter("k", key);
		Long v;
		try{
			v = (Long) qry.getSingleResult();
		}catch(NoResultException e){
			v = null;
		}
		return v!=null;
	}
	
	public String getDefaultValue(String key){
		Query qry = getEntityManager().createQuery("select e.defaultValue from TDomainPropertie e where e.key = :k");
		qry.setParameter("k", key);
		String v;
		try{
			v = (String) qry.getSingleResult();
		}catch(NoResultException e){
			v = null;
		}
		return v;
	}
	
	public Optional<TDomainPropertie> getByKey(String key){
		Query qry = getEntityManager().createQuery("select e from TDomainPropertie e where e.key = :k");
		qry.setParameter("k", key);
		try{
			TDomainPropertie v = (TDomainPropertie) qry.getSingleResult();
			return Optional.of(v);
		}catch(NoResultException e){
			return Optional.empty();
		}
	}
	
	
}
