/**
 * 
 */
package org.tedros.tools.module.preferences.model;

import org.tedros.common.model.TDomainPropertie;
import org.tedros.common.model.TFileEntity;
import org.tedros.common.model.TPropertie;
import org.tedros.core.annotation.security.TAuthorizationType;
import org.tedros.core.annotation.security.TSecurity;
import org.tedros.core.controller.TPropertieController;
import org.tedros.core.domain.DomainApp;
import org.tedros.fx.TUsualKey;
import org.tedros.fx.annotation.control.TFileField;
import org.tedros.fx.annotation.control.TGenericType;
import org.tedros.fx.annotation.control.TLabel;
import org.tedros.fx.annotation.control.TShowField;
import org.tedros.fx.annotation.control.TShowField.TField;
import org.tedros.fx.annotation.control.TTextAreaField;
import org.tedros.fx.annotation.form.TForm;
import org.tedros.fx.annotation.layout.TFieldSet;
import org.tedros.fx.annotation.page.TPage;
import org.tedros.fx.annotation.presenter.TBehavior;
import org.tedros.fx.annotation.presenter.TDecorator;
import org.tedros.fx.annotation.presenter.TListViewPresenter;
import org.tedros.fx.annotation.presenter.TPresenter;
import org.tedros.fx.annotation.process.TEjbService;
import org.tedros.fx.annotation.query.TCondition;
import org.tedros.fx.annotation.query.TOrder;
import org.tedros.fx.annotation.query.TQuery;
import org.tedros.fx.annotation.reader.TFormReaderHtml;
import org.tedros.fx.annotation.reader.TReaderHtml;
import org.tedros.fx.domain.TFileExtension;
import org.tedros.fx.domain.TFileModelType;
import org.tedros.fx.domain.TLayoutType;
import org.tedros.fx.model.TEntityModelView;
import org.tedros.fx.presenter.entity.behavior.TMasterCrudViewBehavior;
import org.tedros.fx.property.TSimpleFileProperty;
import org.tedros.server.query.TCompareOp;
import org.tedros.tools.ToolsKey;
import org.tedros.tools.module.preferences.action.ReloadPropertiesAction;

import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;

/**
 * @author Davis Gordon
 *
 */
@TFormReaderHtml
@TForm(header="",  editCssId="")
@TEjbService(serviceName = TPropertieController.JNDI_NAME, model=TPropertie.class)
@TListViewPresenter(
		page=@TPage(serviceName = TPropertieController.JNDI_NAME,
			query = @TQuery(entity=TPropertie.class, condition= {
					@TCondition(field = "domain.name", operator=TCompareOp.LIKE, label=TUsualKey.NAME)},
				orderBy= {@TOrder(label = TUsualKey.NAME, field = "domain.name")}
			),showSearch=true, showOrderBy=true),
		presenter=@TPresenter(decorator = @TDecorator(viewTitle=ToolsKey.VIEW_SYSTEM_PROPERTIES,
			buildModesRadioButton=false, buildNewButton = false, buildDeleteButton = false),
		behavior=@TBehavior(runNewActionAfterSave=false, saveAllModels=false, 
		saveOnlyChangedModels=true, type=TMasterCrudViewBehavior.class, 
		action=ReloadPropertiesAction.class)))
@TSecurity(	id=DomainApp.PROPERTIE_FORM_ID, 
			appName=ToolsKey.APP_TOOLS, 
			moduleName=ToolsKey.MODULE_PREFERENCES, 
			viewName=ToolsKey.VIEW_SYSTEM_PROPERTIES,
			allowedAccesses={	TAuthorizationType.VIEW_ACCESS, TAuthorizationType.EDIT, 
					TAuthorizationType.READ, TAuthorizationType.SAVE})
public class TPropertieMV extends TEntityModelView<TPropertie> {
	
	@TFieldSet(fields = { "domain" }, 
			legend = "Propriedade")
		@TShowField(layout=TLayoutType.VBOX,
			fields= {
				@TField(name="name", label=TUsualKey.NAME),
				@TField(name="key", label=TUsualKey.KEY),
				@TField(name="description", label=TUsualKey.DESCRIPTION)
		})
	private SimpleObjectProperty<TDomainPropertie> domain;
	
	@TReaderHtml
	@TLabel(text=TUsualKey.VALUE)
	@TTextAreaField(wrapText=true, prefRowCount=8)
	private SimpleStringProperty value;
	
	@TLabel(text=TUsualKey.FILE)
	@TFileField(propertyValueType=TFileModelType.ENTITY, preLoadFileBytes=true,
	extensions= {TFileExtension.ALL_FILES}, showFilePath=true, showImage=true)
	@TGenericType(model=TFileEntity.class)
	private TSimpleFileProperty<TFileEntity> file;
	
	
	public TPropertieMV(TPropertie entity) {
		super(entity);
		super.formatToString("%s", domain);
	}

	

}
