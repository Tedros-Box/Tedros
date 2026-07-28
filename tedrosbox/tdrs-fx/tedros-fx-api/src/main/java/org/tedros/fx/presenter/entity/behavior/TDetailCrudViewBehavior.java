package org.tedros.fx.presenter.entity.behavior;

import java.util.function.Consumer;

import org.apache.commons.lang3.ArrayUtils;
import org.tedros.api.form.ITModelForm;
import org.tedros.api.presenter.view.TViewMode;
import org.tedros.fx.annotation.presenter.TBehavior;
import org.tedros.fx.control.action.TPresenterAction;
import org.tedros.fx.form.TFormBuilder;
import org.tedros.fx.form.TReaderFormBuilder;
import org.tedros.fx.modal.TMessageBox;
import org.tedros.fx.model.TEntityModelView;
import org.tedros.fx.model.TModelView;
import org.tedros.fx.presenter.behavior.TActionState;
import org.tedros.fx.presenter.behavior.TActionType;
import org.tedros.fx.presenter.dynamic.TDynaPresenter;
import org.tedros.fx.presenter.dynamic.behavior.TDynaViewCrudBaseBehavior;
import org.tedros.fx.presenter.dynamic.behavior.TDynaViewSimpleBaseBehavior;
import org.tedros.fx.presenter.dynamic.decorator.TDynaViewCrudBaseDecorator;
import org.tedros.fx.presenter.entity.decorator.TDetailCrudViewDecorator;
import org.tedros.fx.util.TEntityCheckboxListViewCallback;
import org.tedros.server.entity.ITEntity;

import javafx.beans.value.ChangeListener;
import javafx.beans.value.WeakChangeListener;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.collections.WeakListChangeListener;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.SelectionMode;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.util.Callback;
/**
 * The behavior of the detail view. 
 * This behavior can be applied on detail entities.
 * @author Davis Gordon
 *
 * @param <M>
 * @param <E>
 */
@SuppressWarnings({ "rawtypes" })
public class TDetailCrudViewBehavior<M extends TEntityModelView<E>, E extends ITEntity>
extends TDynaViewCrudBaseBehavior<M, E> {
	
	private TDetailCrudViewDecorator<M> decorator;

	@Override
	public void load() {
		super.setSkipConfigBreadcrumb(true);
		super.load();
		this.decorator = (TDetailCrudViewDecorator<M>) getPresenter().getDecorator();
		initialize();
	}
	/**
	 * Initialize the behavior.
	 */
	public void initialize() {
		try{
			
			configColapseButton();
			configNewButton();
			configDeleteButton();
			configPrintButton();
			if(this.decorator.gettEditButton()!=null)
				configEditButton();
			
			configModesRadio();
			
			configListViewChangeListener();
			configListViewCallBack();
			loadListView();
			
		}catch(Throwable e){
			getView().tShowModal(new TMessageBox(e), true);
			LOGGER.error(e.getMessage(), e);
		}
	}
	
	@Override
	public void addAction(TPresenterAction action) {
		boolean flag = false;
		if(action!=null && action.getTypes()!=null) { 
			for(TActionType a : new TActionType[] {TActionType.NEW, TActionType.DELETE, TActionType.PRINT, 
					TActionType.EDIT, TActionType.CHANGE_MODE, TActionType.SELECTED_ITEM})
				if(ArrayUtils.contains(action.getTypes(), a)) {
					flag = true;
					break;
				}	
		}else
			flag = true;
		if(flag) {
			super.addAction(action);
		}else {
			final TDynaPresenter presenter = getModulePresenter();
			final TDynaViewSimpleBaseBehavior behavior = (TDynaViewSimpleBaseBehavior) presenter.getBehavior(); 
			behavior.addAction(action);
		}
	}
		
	@Override
	public void startRemoveProcess(boolean removeFromDataBase, ObservableList<M> selectedEntitiesToRemove) {
		super.startRemoveProcess(false, selectedEntitiesToRemove);
	}
	
	@SuppressWarnings("unchecked")
	private void loadListView() {
		final ObservableList<M> models = getModels();
		final ListView<M> listView = this.decorator.gettListView();
		listView.setItems((ObservableList<M>) (models==null ? FXCollections.observableArrayList() : models));
		listView.setEditable(true);
		listView.getSelectionModel().setSelectionMode(SelectionMode.SINGLE);
		processModelView(null);
	}
	
	@SuppressWarnings("unchecked")
	private void configListViewCallBack() {
		TBehavior tBehavior = getPresenter().getPresenterAnnotation().behavior();
		try {
			Callback<ListView<M>, ListCell<M>> callBack = (Callback<ListView<M>, ListCell<M>>) ((tBehavior!=null) 
					? tBehavior.listViewCallBack().getDeclaredConstructor().newInstance() 
							: new TEntityCheckboxListViewCallback<M>());
			
			final ListView<M> listView = this.decorator.gettListView();			
			listView.setCellFactory(callBack);
			
			if(callBack instanceof TEntityCheckboxListViewCallback withCheckBox) {
				VBox layout = this.decorator.gettListViewLayout();
				
				Label title = (Label) layout.getChildren().remove(0);
				CheckBox checkBox = new CheckBox();
				checkBox.setOnAction(e -> {
                    if (checkBox.isSelected()) {
                    	withCheckBox.getSelectedItems().clear();
                    	withCheckBox.getSelectedItems().addAll(listView.getItems());
                    } else {
                    	withCheckBox.getSelectedItems().clear();
                    }
                    listView.refresh();
                });
				
				HBox hbox = new HBox();
				HBox.setMargin(checkBox, new Insets(5));
				hbox.setAlignment(Pos.CENTER_LEFT);				
				layout.getChildren().add(0, new HBox(checkBox, title));
				
				ListChangeListener<M> selectedToRemoveChl = l -> {
					if(l.next()) {
						Button removeBtn = ((TDynaViewCrudBaseDecorator<M>)this.decorator).gettDeleteButton(); 
						if(getModelView()!=null)
							return;
						removeBtn.setDisable(l.getList().size()==0);
					}
				};
				super.getListenerRepository().add("selectedToRemoveChl", selectedToRemoveChl);
				withCheckBox.getSelectedItems().addListener(new WeakListChangeListener<>(selectedToRemoveChl));
				
				ChangeListener<TActionState> chl00 = (a,o,n) -> {
					switch(n.getType()) {
					case CANCEL:
					case CLOSE:
					case DELETE:
						Button removeBtn = ((TDynaViewCrudBaseDecorator<M>)this.decorator).gettDeleteButton(); 
						removeBtn.setDisable(withCheckBox.getSelectedItems().size()==0);
						break;
					default:
						break;
					
					}
				};
				super.getListenerRepository().add("checkRemoveButtonDisableStatusChl", chl00);
				actionStateProperty().addListener(new WeakChangeListener<>(chl00));	
			}
			
			
		} catch (Exception e) {
			LOGGER.error(e.getMessage(), e);
		}
	}	
	
	@Override
	@SuppressWarnings("unchecked")
	public ObservableList<M> getModelViewListToDelete(){
		
		M selectedModelView = getModelView();
		
		final ListView<M> listView = this.decorator.gettListView();
		Callback<ListView<M>, ListCell<M>> callback = listView.getCellFactory();
		
		if(callback instanceof TEntityCheckboxListViewCallback withCheckBox) {
			ObservableList<M> entitiesToDelete = withCheckBox.getSelectedItems();
			if(selectedModelView!=null && entitiesToDelete.stream().filter(p->p==selectedModelView).count()==0) {
				entitiesToDelete.add(selectedModelView);
			}
			return entitiesToDelete;
		}
		
		return super.getModelViewListToDelete();
	}
		
	/**
	 * Config the ListView listener.
	 */
	protected void configListViewChangeListener() {
		
		ChangeListener<M> chl = (a, o, n) -> {
			this.processListViewSelectedItem(n);
		};
		
		super.getListenerRepository().add("listviewselecteditemviewCL", chl);
		
		this.decorator.gettListView()
		.getSelectionModel()
		.selectedItemProperty()
		.addListener(new WeakChangeListener<>(chl));
	}
	
	/**
	 * Remove the selected model from the ListView
	 */
	@Override
	public void remove(Consumer<Boolean> callback) {
		super.remove(result->{
			if(result) {
				final ListView<M> listView = this.decorator.gettListView();
				listView.getSelectionModel().clearSelection();
				callback.accept(true);
			}else {
				callback.accept(false);
			}
		});
	}

	/**
	 * Remove the selected item from the ListView
	 *
	public void remove() {
		final ListView<M> listView = this.decorator.gettListView();
		int index = getModels().indexOf(getModelView());
		listView.getSelectionModel().clearSelection();
		super.remove(index);
	}
	*/
	@Override
	public void colapseAction() {
		if(!this.decorator.isListContentVisible())
			showListView();
		else
			hideListView();
	}
	
	/**
	 * Hide the ListView panel
	 */
	public void hideListView() {
		this.decorator.hideListContent();
	}

	/**
	 * Show the ListView panel
	 */
	public void showListView() {
		this.decorator.showListContent();
	}

	@Override
	public boolean processNewEntityBeforeBuildForm(M model) {
		addInListView(model);
		return false;
	}

	/**
	 * @param model
	 */
	private void addInListView(M model) {
		final ListView<M> list = this.decorator.gettListView();
		list.getItems().add(model);
		list.selectionModelProperty().get().select(list.getItems().size()-1);
	}
	
	@Override
	public void setForm(ITModelForm form) {
		final TDynaPresenter presenter = (TDynaPresenter) getModulePresenter();
		if( presenter.getDecorator() instanceof TDynaViewCrudBaseDecorator 
				&& ((TDynaViewCrudBaseDecorator) presenter.getDecorator()).gettBreadcrumbForm()!=null){
			removeBreadcrumbFormChangeListener();
			super.setForm(form);
			addBreadcrumbFormChangeListener();
		}else
			super.setForm(form);
	}
	
	/**
	 * Called by the editAction()
	 * */
	@Override
	public void editEntity(TModelView model) {
		if(this.decorator.isShowBreadcrumBar())
			showFormInMainPresenter(model);
		else
			showForm(TViewMode.EDIT);
	}
	
	private void showFormInMainPresenter(TModelView model) {
		
		final TDynaPresenter presenter = getModulePresenter();
		final TDynaViewCrudBaseBehavior behavior = (TDynaViewCrudBaseBehavior)presenter.getBehavior(); 
		
		ITModelForm form =  behavior.isReaderModeSelected() // check in the main behavior  
				? TReaderFormBuilder.create(model).build() 
						: TFormBuilder.create(model).presenter(getPresenter()).build();
		
		form.settPresenter(presenter);
		presenter.getBehavior().setForm(form);
	}

	@Override
	public void setDisableModelActionButtons(boolean flag) {
		if(decorator.gettCancelButton()!=null)
			decorator.gettCancelButton().setDisable(flag);
		if(decorator.gettEditButton()!=null)
			decorator.gettEditButton().setDisable(flag);
		if(decorator.gettDeleteButton()!=null)
			decorator.gettDeleteButton().setDisable(flag);
		if(decorator.gettPrintButton()!=null)
			decorator.gettPrintButton().setDisable(flag);
	}
	
	@Override
	public void removeBreadcrumbFormChangeListener() {
		final TDynaPresenter presenter = getModulePresenter();
		if(presenter.getBehavior() instanceof TDynaViewCrudBaseBehavior) {
			final TDynaViewCrudBaseBehavior behavior = (TDynaViewCrudBaseBehavior)presenter.getBehavior();
			behavior.removeBreadcrumbFormChangeListener();
		}
	}
	
	@Override
	public void addBreadcrumbFormChangeListener() {
		final TDynaPresenter presenter = getModulePresenter();
		if(presenter.getBehavior() instanceof TDynaViewCrudBaseBehavior) {
			final TDynaViewCrudBaseBehavior behavior = (TDynaViewCrudBaseBehavior)presenter.getBehavior();
			behavior.addBreadcrumbFormChangeListener();
		}
	}
	
	/**
	 * Process the selected item on the ListView
	 * @param m
	 */
	protected void processListViewSelectedItem(M m) {
		if(m==null) {
			setModelView(null);
			showListView();
		}else{
			selectedItemAction(m);
			hideListView();
		}
	}


	@Override
	public void loadModelView(M m) {
		this.addInListView(m);
		this.processListViewSelectedItem(m);
	}
	
}
