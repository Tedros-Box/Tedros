package org.tedros.common.model;

import org.tedros.common.domain.DomainSchema;
import org.tedros.common.domain.DomainTables;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(name = DomainTables.user_propertie, schema = DomainSchema.tedros_common,
uniqueConstraints = @UniqueConstraint(
	columnNames = { "domain_propertie_id", "created_by_user_id" }))
public class TUserPropertie extends TBasePropertieValue {

	private static final long serialVersionUID = 5123458792406122411L;
	
	// Vários TUserPropertie -> 1 TDomainPropertie
	@ManyToOne(fetch = FetchType.EAGER, optional = false)
	@JoinColumn(name = "domain_propertie_id", nullable = false)
	private TDomainPropertie domain;
	
	public TUserPropertie() {
	}
	
	@Override
	public TDomainPropertie getDomain() {
		return domain;
	}
	
	public void setDomain(TDomainPropertie domain) {
		this.domain = domain;
	}

}
