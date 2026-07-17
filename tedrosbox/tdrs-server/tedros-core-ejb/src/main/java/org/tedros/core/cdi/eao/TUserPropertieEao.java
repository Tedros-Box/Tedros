/**
 * 
 */
package org.tedros.core.cdi.eao;

import jakarta.enterprise.context.RequestScoped;
import jakarta.persistence.NoResultException;
import jakarta.persistence.Query;

import org.tedros.common.model.TFileEntity;
import org.tedros.core.setting.model.TUserPropertie;
import org.tedros.server.cdi.eao.TGenericEAO;

/**
 * EAO for user-specific properties.
 */
@RequestScoped
public class TUserPropertieEao extends TGenericEAO<TUserPropertie> {

	public boolean exist(String key, Long userId){
		Query qry = getEntityManager().createQuery("select e.id from TUserPropertie e "
				+ "where e.domain.key = :k and e.createdByUserId = :u");
		qry.setParameter("k", key);
		qry.setParameter("u", userId);
		Long v;
		try{
			v = (Long) qry.getSingleResult();
		}catch(NoResultException e){
			v = null;
		}
		return v!=null;
	}

	public String getValue(String key, Long userId){
		Query qry = getEntityManager().createQuery("select e.value from TUserPropertie e "
				+ "where e.domain.key = :k and e.createdByUserId = :u");
		qry.setParameter("k", key);
		qry.setParameter("u", userId);
		String v;
		try{
			v = (String) qry.getSingleResult();
		}catch(NoResultException e){
			v = null;
		}
		return v;
	}

	/**
	 * Retorna o valor da propriedade do usuario ou, se nulo, o defaultValue do dominio.
	 */
	public String getValueOrDefault(String key, Long userId){
		Query qry = getEntityManager().createQuery("select coalesce(e.value, d.defaultValue) "
				+ "from TUserPropertie e join e.domain d "
				+ "where d.key = :k and e.createdByUserId = :u");
		qry.setParameter("k", key);
		qry.setParameter("u", userId);
		String v;
		try{
			v = (String) qry.getSingleResult();
		}catch(NoResultException e){
			v = null;
		}
		return v;
	}

	public TFileEntity getFile(String key, Long userId){
		Query qry = getEntityManager().createQuery("select e.file from TUserPropertie e "
				+ "join e.file f join f.byteEntity b "
				+ "where e.domain.key = :k and e.createdByUserId = :u");
		qry.setParameter("k", key);
		qry.setParameter("u", userId);
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