/**
 * 
 */
package org.tedros.core.cdi.eao;

import jakarta.enterprise.context.RequestScoped;
import jakarta.persistence.NoResultException;
import jakarta.persistence.Query;

import org.tedros.common.model.TFileEntity;
import org.tedros.core.setting.model.TPropertie;
import org.tedros.server.cdi.eao.TGenericEAO;

/**
 * @author Davis Gordon
 *
 */
@RequestScoped
public class TPropertieEao extends TGenericEAO<TPropertie> {

	public boolean exist(String key){
		Query qry = getEntityManager().createQuery("select e.id from TPropertie e "
				+ "where e.domain.key = :k");
		qry.setParameter("k", key);
		Long v;
		try{
			v = (Long) qry.getSingleResult();
		}catch(NoResultException e){
			v = null;
		}
		return v!=null;
	}

	public String getValue(String key){
		Query qry = getEntityManager().createQuery("select e.value from TPropertie e "
				+ "where e.domain.key = :k");
		qry.setParameter("k", key);
		String v;
		try{
			v = (String) qry.getSingleResult();
		}catch(NoResultException e){
			v = null;
		}
		return v;
	}

	/**
	 * Retorna o valor da propriedade ou, se nulo, o defaultValue do dominio.
	 */
	public String getValueOrDefault(String key){
		Query qry = getEntityManager().createQuery("select coalesce(e.value, d.defaultValue) "
				+ "from TPropertie e join e.domain d "
				+ "where d.key = :k");
		qry.setParameter("k", key);
		String v;
		try{
			v = (String) qry.getSingleResult();
		}catch(NoResultException e){
			v = null;
		}
		return v;
	}

	public TFileEntity getFile(String key){
		Query qry = getEntityManager().createQuery("select e.file from TPropertie e "
				+ "join e.file f join f.byteEntity b "
				+ "where e.domain.key = :k");
		qry.setParameter("k", key);
		TFileEntity v;
		try{
			v = (TFileEntity) qry.getSingleResult();
			getEntityManager().detach(v);
		}catch(NoResultException e){
			v = null;
		}
		return v;
	}

}