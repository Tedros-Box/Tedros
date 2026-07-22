package org.tedros.common.model;

import org.tedros.server.entity.TVersionEntity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Lob;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.OneToOne;

@MappedSuperclass
public abstract class TBasePropertieValue extends TVersionEntity {
	
	private static final long serialVersionUID = 402490778438539893L;

	@Lob
	@Column(columnDefinition = "TEXT")
	private String value;
	
	@OneToOne(cascade = CascadeType.ALL, fetch = FetchType.EAGER)
	@JoinColumn(name = "file_id")
	private TFileEntity file;
	
	public abstract TDomainPropertie getDomain();
	
	public String getValue() {
		return value;
	}
	
	public void setValue(String value) {
		this.value = value;
	}
	
	public TFileEntity getFile() {
		return file;
	}
	
	public void setFile(TFileEntity file) {
		this.file = file;
	}
	
	// Conveniência: expõe os metadados do domínio sem duplicar colunas
	public String getName() {
		return getDomain() != null ? getDomain().getName() : null;
	}
	
	public String getKey() {
		return getDomain() != null ? getDomain().getKey() : null;
	}
	
	public String getDescription() {
		return getDomain() != null ? getDomain().getDescription() : null;
	}
	
	public org.tedros.common.domain.TType getType() {
		return getDomain() != null ? getDomain().getType() : null;
	}
	
	@Override
	public String toString() {
		return getName();
	}

}
