package org.kinalllaves.controller;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.Label;
import java.util.*;
/** Utilidad visual: preserva columnas incluso cuando un filtro devuelve cero filas. */
public final class Tablas {
 private Tablas(){}
 public static void mostrar(TableView<Map<String,Object>> tabla,List<Map<String,Object>> items,String filtro){
  mostrar(tabla,items,filtro,new String[0]);
 }
 public static void mostrar(TableView<Map<String,Object>> tabla,List<Map<String,Object>> items,String filtro,String... columnasPorDefecto){
  // El ancho de cada columna se establece con setPrefWidth al construir las columnas.
  Set<String> cols=new LinkedHashSet<>();
  if(items!=null&&!items.isEmpty())cols.addAll(items.get(0).keySet());
  else if(columnasPorDefecto!=null && columnasPorDefecto.length>0)
   cols.addAll(Arrays.asList(columnasPorDefecto));
  if(!cols.isEmpty()){
   tabla.getColumns().clear();
   for(String key:cols){
    TableColumn<Map<String,Object>,String> c=new TableColumn<>(key.replace('_',' '));
    c.setPrefWidth(key.equalsIgnoreCase("descripcion")?230:155);
    c.setCellValueFactory(v->new SimpleStringProperty(Objects.toString(v.getValue().get(key),"")));
    tabla.getColumns().add(c);
   }
  }
  String texto=filtro==null?"":filtro.toLowerCase(Locale.ROOT).trim();
  List<Map<String,Object>> visibles=new ArrayList<>();
  if(items!=null)for(Map<String,Object> r:items)
   if(texto.isEmpty()||r.values().toString().toLowerCase(Locale.ROOT).contains(texto))visibles.add(r);

  tabla.setItems(FXCollections.observableArrayList(visibles));
  tabla.setPlaceholder(new Label(texto.isEmpty()
      ? "Todavía no hay registros. Realiza una operación para que aparezcan aquí."
      : "No se encontraron resultados. Prueba con otro dato de búsqueda."));
 }
}
