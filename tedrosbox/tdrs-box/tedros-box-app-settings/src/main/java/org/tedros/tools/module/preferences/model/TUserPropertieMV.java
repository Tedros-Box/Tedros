/**
 * 
 */
package org.tedros.tools.module.preferences.model;

import org.tedros.common.model.TFileEntity;
import org.tedros.core.annotation.security.TAuthorizationType;
import org.tedros.core.annotation.security.TSecurity;
import org.tedros.core.controller.TUserPropertieController;
import org.tedros.core.domain.DomainApp;
import org.tedros.core.setting.model.TDomainPropertie;
import org.tedros.core.setting.model.TUserPropertie;
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

import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;

/**
 * @author Davis Gordon
 *
 */
@TFormReaderHtml
@TForm(header="",  editCssId="")
@TEjbService(serviceName = TUserPropertieController.JNDI_NAME, model=TUserPropertie.class,
filterByLoggedUser=true)
@TListViewPresenter(
		page=@TPage(serviceName = TUserPropertieController.JNDI_NAME, filterByLoggedUser=true,
			query = @TQuery(entity=TUserPropertie.class, condition= {
					@TCondition(field = "domain.name", operator=TCompareOp.LIKE, label=TUsualKey.NAME)},
				orderBy= {@TOrder(label = TUsualKey.NAME, field = "domain.name")}
			),showSearch=true, showOrderBy=true),
		presenter=@TPresenter(decorator = @TDecorator(viewTitle=ToolsKey.VIEW_USER_PROPERTIES,
			buildModesRadioButton=false, buildNewButton = false, buildDeleteButton = false),
		behavior=@TBehavior(runNewActionAfterSave=false, saveAllModels=false, 
		saveOnlyChangedModels=true, type=TMasterCrudViewBehavior.class)))
@TSecurity(	id=DomainApp.USER_PROPERTIE_FORM_ID, 
			appName=ToolsKey.APP_TOOLS, 
			moduleName=ToolsKey.MODULE_PREFERENCES, 
			viewName=ToolsKey.VIEW_USER_PROPERTIES,
			allowedAccesses={	TAuthorizationType.VIEW_ACCESS, TAuthorizationType.EDIT, 
					TAuthorizationType.READ, TAuthorizationType.SAVE})
public class TUserPropertieMV extends TEntityModelView<TUserPropertie> {

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
	
	
	public TUserPropertieMV(TUserPropertie entity) {
		super(entity);
		super.formatToString("%s", domain);
	}

	

}
