package org.tedros.fx.presenter.modal.behavior;

import java.util.Arrays;
import java.util.function.Consumer;

import org.tedros.core.ITModule;
import org.tedros.core.annotation.security.TAuthorizationType;
import org.tedros.core.context.TedrosAppManager;
import org.tedros.core.context.TedrosContext;
import org.tedros.fx.annotation.presenter.TBehavior;
import org.tedros.fx.control.action.TPresenterAction;
import org.tedros.fx.exception.TValidatorException;
import org.tedros.fx.modal.TMessageBox;
import org.tedros.fx.model.TEntityModelView;
import org.tedros.fx.presenter.behavior.TActionState;
import org.tedros.fx.presenter.behavior.TActionType;
import org.tedros.fx.presenter.behavior.TProcessResult;
import org.tedros.fx.presenter.dynamic.behavior.TDynaViewCrudBaseBehavior;
import org.tedros.fx.presenter.dynamic.decorator.TDynaViewCrudBaseDecorator;
import org.tedros.fx.presenter.modal.decorator.TEditModalDecorator;
import org.tedros.fx.util.TEntityCheckboxListViewCallback;
import org.tedros.server.entity.ITEntity;

import javafx.beans.value.ChangeListener;
import javafx.beans.value.WeakChangeListener;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.collections.WeakListChangeListener;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
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
 * The behavior of the open modal view to edit entity. 
 * This behavior can be applied on detail entities.
 * @author Davis Gordon
 *
 * @param <M>
 * @param <E>
 */
public class TEditModalBehavior<M extends TEntityModelView<E>, E extends ITEntity>
extends TDynaViewCrudBaseBehavior<M, E> {
	
	protected TEditModalDecorator<M> decorator;
	private boolean singleMode = false;
		
	@Override
	public void load() {
		super.load();
		this.decorator = (TEditModalDecorator<M>) getPresenter().getDecorator();
		initialize();
	}
	/**
	 * Initialize the behavior.
	 */
	public void initialize() {
		
		configCancelAction();
		configColapseButton();
		configNewButton();
		configSaveButton();
		configDeleteButton();
		configImportButton();
		configPrintButton();
		
		configCancelButton();
		configCloseButton();
		
		configListViewChangeListener();
		configListViewCallBack();
		
		if(!isUserNotAuthorized(TAuthorizationType.VIEW_ACCESS))
			this.loadListView();
		else
			super.setViewStateAsReady();
	}
	
	/**
	 * Enable the single mode.
	 */
	public void setSingleMode() {
		this.singleMode = true;
		super.runNewActionAfterSave = false;
		this.colapseAction();
		if(!getModels().isEmpty() && super.getModelView()==null) {
			super.setModelView(getModels().get(0));
		}else {
			super.newAction();
		}
		if(decorator.gettDeleteButton()!=null)
			super.addAction(new TPresenterAction(TActionType.DELETE) {
	
				@Override
				public boolean runBefore() {
					return true;
				}
	
				@Override
				public void runAfter() {
					newAction();
				}
			});
		if(decorator.gettCancelButton()!=null)
			super.addAction(new TPresenterAction(TActionType.CANCEL) {
	
				@Override
				public boolean runBefore() {
					return true;
				}
	
				@Override
				public void runAfter() {
					if(!getModels().isEmpty())
						setModelView(getModels().get(0));
				}
				
			});
	}
	
	/**
	 * Config the close button 
	 * */
	public void configCloseButton() {
		final Button cleanButton = this.decorator.gettCloseButton();
			cleanButton.setOnAction(new EventHandler<ActionEvent>() {
				@Override
				public void handle(ActionEvent event) {
					closeAction();
				}
			});
	}
	
	/**
	 * Perform this action when close button onAction is triggered.
	 * */
	public void closeAction() {
		if(actionHelper.runBefore(TActionType.CLOSE)){
			try{
				
				//recupera a lista de models views
				final ObservableList<M> modelsViewsList = (ObservableList<M>) ((getModels()!=null) 
						? getModels() 
								: getModelView()!=null 
								? FXCollections.observableList(Arrays.asList(getModelView())) 
										: null) ;
								
				if(modelsViewsList != null)
					validateModels(modelsViewsList);
				
				closeModal();
				
			}catch(TValidatorException e){
				getView().tShowModal(new TMessageBox(e), true);
				setActionState(new TActionState<>(TActionType.CLOSE, TProcessResult.FINISHED));
			} catch (Throwable e) {
				LOGGER.error(e.getMessage(), e);
				getView().tShowModal(new TMessageBox(e), true);
				setActionState(new TActionState<>(TActionType.CLOSE, TProcessResult.ERROR));
			}
		}
		actionHelper.runAfter(TActionType.CLOSE);
	}
	
	private void closeModal() {
		super.invalidate();
		
		ITModule module = TedrosContext.getCurrentModule();
		
		TedrosAppManager.getInstance()
		.getModuleContext(module).getCurrentViewContext()
		.getPresenter().getView().tHideModal();
	}
	/**
	 * Config the cancel action
	 */
	protected void configCancelAction() {
		addAction(new TPresenterAction(TActionType.CANCEL) {

			@Override
			public boolean runBefore() {
				return true;
			}

			@Override
			public void runAfter() {
				final ListView<M> listView = decorator.gettListView();
				listView.getSelectionModel().clearSelection();
				setDisableModelActionButtons(true);
				showListView();
			}
		});
	}
	
	
	@SuppressWarnings("unchecked")
	private void loadListView() {
		final ObservableList<M> models = getModels();
		final ListView<M> listView = this.decorator.gettListView();
		listView.setItems((ObservableList<M>) (models==null ? FXCollections.observableArrayList() : models));
		listView.setEditable(true);
		listView.getSelectionModel().setSelectionMode(SelectionMode.SINGLE);
		super.getListenerRepository().remove("processloadlistviewCL");
		final M mv = getPresenter().getModelView();
		processModelView(mv);
		
	}
	
	
	@SuppressWarnings({ "unchecked", "rawtypes" })
	private void configListViewCallBack() {
		TBehavior tBehavior = getPresenter().getPresenterAnnotation().behavior();
		try {
			Callback<ListView<M>, ListCell<M>> callBack = (Callback<ListView<M>, ListCell<M>>) 
					((tBehavior!=null) 
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
		
		ChangeListener<M> chl = (a0, old_, new_) -> {
			processListViewSelectedItem(new_);
		};
		
		super.getListenerRepository().add("listviewselecteditemviewCL", chl);
		
		this.decorator.gettListView()
		.getSelectionModel()
		.selectedItemProperty()
		.addListener(new WeakChangeListener<>(chl));
		
	}
	/**
	 * Process the selected item on the ListView
	 * @param new_
	 */
	protected void processListViewSelectedItem(M new_) {
		if(new_==null) {
			setModelView(null);
			showListView();
		}else{
			selectedItemAction(new_);
			hideListView();
		}
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
		if(!this.singleMode)
			this.decorator.showListContent();
	}
	
	@Override
	public boolean processNewEntityBeforeBuildForm(M model) {
		final ListView<M> list = this.decorator.gettListView();
		list.getItems().add(model);
		list.selectionModelProperty().get().select(list.getItems().size()-1);
		return false;
	}
		
}
