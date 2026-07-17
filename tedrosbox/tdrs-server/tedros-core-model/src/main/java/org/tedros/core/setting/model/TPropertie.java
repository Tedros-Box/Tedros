package org.tedros.core.setting.model;

import org.tedros.core.domain.DomainSchema;
import org.tedros.core.domain.DomainTables;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(name = DomainTables.propertie, schema = DomainSchema.tedros_core,
uniqueConstraints = @UniqueConstraint(columnNames = { "domain_propertie_id" }))
public class TPropertie extends TBasePropertieValue {
	
	private static final long serialVersionUID = -3294433359406122382L;

	// 1 TPropertie -> 1 TDomainPropertie (unique garante o 1..1 no banco)
	@OneToOne(fetch = FetchType.EAGER, optional = false)
	@JoinColumn(name = "domain_propertie_id", nullable = false, unique = true)
	private TDomainPropertie domain;
	
	public TPropertie() {
	}
	
	@Override
	public TDomainPropertie getDomain() {
		return domain;
	}
	
	public void setDomain(TDomainPropertie domain) {
		this.domain = domain;
	}

}
