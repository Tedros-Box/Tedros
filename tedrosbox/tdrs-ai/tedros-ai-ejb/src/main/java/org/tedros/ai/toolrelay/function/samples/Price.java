package org.tedros.ai.toolrelay.function.samples;

import java.math.BigDecimal;

import org.tedros.sample.entity.ProductPrice;

/**
 * Copia de {@code org.tedros.samples.ai.function.Price} (app-samples-fx) —
 * mesmo JSON de saida para o LLM.
 *
 * @author Davis Gordon
 */
public class Price {

	private String productCode;

	private String product;

	private String legalPerson;

	private String costCenter;

	private BigDecimal price;

	public Price(ProductPrice pp) {
		this.productCode = pp.getProduct().getCode();
		this.product = pp.getProduct().getName();
		this.legalPerson = pp.getLegalPerson().toString();
		this.costCenter = pp.getCostCenter().toString();
		this.price = pp.getUnitPrice();
	}

	public String getProductCode() {
		return productCode;
	}

	public void setProductCode(String productCode) {
		this.productCode = productCode;
	}

	public String getProduct() {
		return product;
	}

	public void setProduct(String product) {
		this.product = product;
	}

	public String getLegalPerson() {
		return legalPerson;
	}

	public void setLegalPerson(String legalPerson) {
		this.legalPerson = legalPerson;
	}

	public String getCostCenter() {
		return costCenter;
	}

	public void setCostCenter(String costCenter) {
		this.costCenter = costCenter;
	}

	public BigDecimal getPrice() {
		return price;
	}

	public void setPrice(BigDecimal price) {
		this.price = price;
	}

}
