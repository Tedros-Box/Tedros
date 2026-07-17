package org.tedros.core.setting.model;

import java.util.Objects;

import org.tedros.common.domain.TType;
import org.tedros.core.domain.DomainSchema;
import org.tedros.core.domain.DomainTables;
import org.tedros.server.entity.TVersionEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(name = DomainTables.domain_propertie, schema = DomainSchema.tedros_core,
uniqueConstraints=@UniqueConstraint(columnNames = { "key"}))
public class TDomainPropertie extends TVersionEntity {
	
	private static final long serialVersionUID = -3294433359406122382L;

	@Column(length=40, nullable = false)
	private String name;
	
	@Column(length=120, nullable = false)
	private String key;
	
	@Lob
	@Column(name = "default_value", columnDefinition = "TEXT")
	private String defaultValue;
	
	@Column(length=500)
	private String description;
	
	@Column(length=8)
	@Enumerated(EnumType.STRING)
	private TType type;
	
	public TDomainPropertie() {
		
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getKey() {
		return key;
	}

	public void setKey(String key) {
		this.key = key;
	}

	public String getDefaultValue() {
		return defaultValue;
	}

	public void setDefaultValue(String defaultValue) {
		this.defaultValue = defaultValue;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public TType getType() {
		return type;
	}

	public void setType(TType type) {
		this.type = type;
	}

	@Override
	public String toString() {
		return name;
	}
	
	public boolean isSameValues(String name, String key, String defaultValue, String description, TType type) {
		return Objects.equals(this.defaultValue, defaultValue) && Objects.equals(this.description, description)
				&& Objects.equals(this.key, key) && Objects.equals(this.name, name) && this.type == type;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = super.hashCode();
		result = prime * result + Objects.hash(defaultValue, description, key, name, type);
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (!super.equals(obj))
			return false;
		if (getClass() != obj.getClass())
			return false;
		TDomainPropertie other = (TDomainPropertie) obj;
		return Objects.equals(defaultValue, other.defaultValue) && Objects.equals(description, other.description)
				&& Objects.equals(key, other.key) && Objects.equals(name, other.name) && type == other.type;
	}

}
