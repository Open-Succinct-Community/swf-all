/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.venky.swf.views.model;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;

import com.venky.core.util.ObjectUtil;
import com.venky.swf.db.Database;
import com.venky.swf.db.JdbcTypeHelper.TypeConverter;
import com.venky.swf.db.model.Model;
import com.venky.swf.routing.Path;
import com.venky.swf.views.controls.page.Body;
import com.venky.swf.views.controls.page.Form;
import com.venky.swf.views.controls.page.Image;
import com.venky.swf.views.controls.page.Link;
import com.venky.swf.views.controls.page.layout.Div;
import com.venky.swf.views.controls.page.layout.LineBreak;
import com.venky.swf.views.controls.page.layout.Table;
import com.venky.swf.views.controls.page.layout.Table.Column;
import com.venky.swf.views.controls.page.layout.Table.Row;
import com.venky.swf.views.controls.page.text.Label;
import com.venky.swf.views.controls.page.text.Select;
import com.venky.swf.views.controls.page.text.Select.Option;
import com.venky.swf.views.controls.page.text.TextBox;

/**
 *
 * @author venky
 */
public class ModelListView<M extends Model> extends AbstractModelView<M> {

    private List<M> records;

    public ModelListView(Path path, Class<M> modelClass, String[] includeFields, List<M> records) {
        super(path, modelClass, includeFields);
        this.records = records;
    }

    @Override
    protected void createBody(Body b) {
        Link create = new Link();
        create.setUrl(getPath().controllerPath()+"/blank");
        create.addControl(new Image("/resources/images/blank.png"));
        
        b.addControl(create);

        Table table = new Table();
        table.setProperty("class", "tablesorter");
        Row header = table.createHeader();
        Column action = header.createColumn(3); 
        action.setText("Action");
        action.setProperty("width", "2%");
        for (String fieldName : getIncludedFields()) {
            if (isFieldVisible(fieldName)) {
                header.createColumn().setText(getFieldLiteral(fieldName));
            }
        }

        for (M record : records) {
            Row row = table.createRow();
            Link edit = new Link();
            edit.setUrl(getPath().controllerPath()+"/edit/"+record.getId());
            edit.addControl(new Image("/resources/images/edit.png"));
            
            Link show = new Link();
            show.setUrl(getPath().controllerPath()+"/show/"+record.getId());
            show.addControl(new Image("/resources/images/show.png"));
            

            Link destroy = new Link();
            destroy.setUrl(getPath().controllerPath()+"/destroy/"+record.getId());
            destroy.addControl(new Image("/resources/images/destroy.png"));
            
            row.createColumn().addControl(show);
            row.createColumn().addControl(edit);
            row.createColumn().addControl(destroy);
            
            for (String fieldName : getIncludedFields()) {
                try {
                    if (isFieldVisible(fieldName)) {
                        Column column = row.createColumn(); 

                    	Method getter = getFieldGetter(fieldName);
                        Object value = getter.invoke(record);
                        TypeConverter<?> converter = Database.getInstance().getJdbcTypeHelper().getTypeRef(getter.getReturnType()).getTypeConverter();
                        Label control = null;
                        String sValue = converter.toString(value);
                        if (isFieldPassword(fieldName)){
                        	sValue = sValue.replaceAll(".", "\\*");
                        }
                        String parentDescription = getParentDescription(getter, record) ;
                        if (!ObjectUtil.isVoid(parentDescription)){
                        	sValue = parentDescription;
                        }else {
                            column.addClass(converter.getDisplayClassName());
                        }
                        control = new Label(sValue);
                        column.addControl(control);
                    }
                } catch (IllegalAccessException ex) {
                    throw new RuntimeException(ex);
                } catch (IllegalArgumentException ex) {
                    throw new RuntimeException(ex);
                } catch (InvocationTargetException ex) {
                    throw new RuntimeException(ex);
                }
            }
        }

        b.addControl(table);
        
        Div pager = new Div();
        b.addControl(pager);

        table.setProperty("pagerid", pager.getId());
        pager.setProperty("class", "pager");

        Form form = new Form();
        pager.addControl(form);
        
        Image i = new Image("/resources/scripts/jquery.tablesorter/addons/pager/icons/first.png");
        i.setProperty("class", "first");
        form.addControl(i);
        
        i = new Image("/resources/scripts/jquery.tablesorter/addons/pager/icons/prev.png");
        i.setProperty("class", "prev");
        form.addControl(i);
        
        TextBox pageDisplay = new TextBox();
        pageDisplay.setProperty("class","pagedisplay");
        form.addControl(pageDisplay);
        
        i = new Image("/resources/scripts/jquery.tablesorter/addons/pager/icons/next.png");
        i.setProperty("class", "next");
        form.addControl(i);

        i = new Image("/resources/scripts/jquery.tablesorter/addons/pager/icons/last.png");
        i.setProperty("class", "last");
        form.addControl(i);

        Select select = new Select();
        select.setProperty("class", "pagesize");
        select.createOption("5","5");
        Option defaultOption = select.createOption("10","10");
        defaultOption.setProperty("selected", "selected");
        select.createOption("20","20");
        select.createOption("30","30");
        form.addControl(select);
        
        b.addControl(new LineBreak());
    }
}
