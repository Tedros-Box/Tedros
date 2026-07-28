package org.tedros.fx.util;

import org.apache.commons.lang3.StringUtils;
import org.tedros.fx.exception.TException;
import org.tedros.fx.model.TEntityModelView;
import org.tedros.fx.model.TModelView;
import org.tedros.server.entity.ITEntity;

import javafx.beans.property.StringProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.util.Callback;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

/**
 *<pre>
 *A {@link Callback} to {@link ListView} for {@link TEntityModelView}.
 *
 *This call back use the getDisplayProperty() method to get the {@link StringProperty} to bind with the
 *{@link ListCell} textProperty.
 *
 *The selected item in the {@link ListView} will  
 *</pre>  
 * */

@SuppressWarnings("rawtypes")
public class TEntityCheckboxListViewCallback<M extends TModelView> implements Callback<ListView<M>, ListCell<M>> {

	public static String ITEM_CSS_ID = "t-listcell-item";
	public static String BOLD_ITEM_CSS_ID = "t-listcell-bold-item";
	public static String NEW_ITEM_EMPTY_CSS_ID = "t-listcell-new-empty-item"; 
	public static String NEW_ITEM_CSS_ID = "t-listcell-new-item"; 
	public static String CHANGED_ITEM_CSS_ID = "t-listcell-changed-item";
	
	private ObservableList<M> selectedItems = FXCollections.observableArrayList();

	public ObservableList<M> getSelectedItems() {
		return selectedItems;
	}
	
	@Override
	public ListCell<M> call(final ListView<M> param) {
		
        final ListCell<M> cell = new ListCell<M>() {
            
            private CheckBox checkBox;
            
            {
                checkBox = new CheckBox();
                checkBox.setOnAction(e -> {
                    M item = getItem();
                    if (item != null) {
                        if (checkBox.isSelected()) {
                            if (!selectedItems.contains(item)) {
                                selectedItems.add(item);
                            }
                        } else {
                            selectedItems.remove(item);
                        }
                    }
                });
            }
            
        	@Override
            public void updateItem(final M item, boolean empty) {
        		super.updateItem(item, empty);
            	
        		if (item == null) {
        			setId(ITEM_CSS_ID);
                	textProperty().unbind();
        			setText(null);
        			setGraphic(null);
        		}else {
        			//if(empty) {
	        			if(item.toStringProperty()==null)
	        				throw new RuntimeException(new TException("The method toStringProperty must return a not null value!"));
	        			
	                	textProperty().bind(item.toStringProperty());
	                	
	                	checkBox.setSelected(selectedItems.contains(item));
	                	setGraphic(checkBox);
	                	
	                	validateNew(item);
	                	
	                	item.lastHashCodeProperty().addListener(new ChangeListener<Number>() {
							@Override
							public void changed(ObservableValue<? extends Number> arg0, Number arg1, Number arg2) {
								if(!validateNew(item))
									setId(CHANGED_ITEM_CSS_ID);
							}
						});
	                	
	                	item.loadedProperty().addListener(new ChangeListener<Number>() {
							@Override
							public void changed(ObservableValue<? extends Number> arg0, Number arg1, Number arg2) {
								setId(ITEM_CSS_ID);
							}
						});
        			
                }/**/
                	
            }

			private boolean validateNew(final M item) {
				if(item.getModel() instanceof ITEntity && ((ITEntity)item.getModel()).isNew()){
					if(StringUtils.isBlank(item.toStringProperty().getValue()))
						setId(NEW_ITEM_EMPTY_CSS_ID);
					else
						setId(NEW_ITEM_CSS_ID);
					return true;
				}
				return false;
			}
        };
        return cell;
    }

}
