# Detailed Trails
This section is to dive into the deeper
## Creating Models
Succinct encourages model driven development, Domain models may map to database tables one-to-one or could  be simply virtual models created for the purpose  of communication with an external system via apis.

**Domain models are created as a java interface that extends the interface com.venky.swf.db.model.Model**
```java

public interface Sample extends Model { 
}
```

**Annotate with @IS_VIRTUAL if the model is not backed by a database table**
```java
@IS_VIRTUAL
public interface Sample extends Model { 
}
```
**On naming Models/Tables**
Models are usually English nouns, and are named in the singular form. Their names are also expressed in TitleCase. 

Tables are plural form of the model's names and their names in the database are expressed in SNAKE_CASE. 

for e.g. When the name of  a Model is SampleHeader, The corresponding table would be named as SAMPLE_HEADERS. 

### Simple Fields
Fields added to models are expressed as getters and setters with  appropriate datatypes.  
for e.g: 
```java
  ...
  public int getSomeThing();
  public void setSomeThing(int someThing); 

```
Based on the return value of the getter, an appropriate db type would be automatically chosen. The return type for field getters can be one of the following:

**boolean, Boolean , byte, Byte short, Short, int, Integer, long, Long, float, Float,  double, Double, BigDecimal, java.sql.Date, java.sql.Timestamp, java.sql.Time and String**

The mappings from java type to db type are defined by a JdbcTypeHelper class that is chosen based on the dbdriver class . 

Since 2.8 Succinct supports the derby,mysql,postgres,sqlite,sqldroid and h2 for databases to be used with succinct applications. 

**On Field/Column Naming conventions**
1. Field Getters and Setters are named in Camel case.
```java
    some_type getSomeThing();
    void setSomeThing(some_type someThing);
```
2. If a getter is getSomeThing, then the corresponding model field is  SOME_THING. 
3. Column Names and  Field Names are usually same but can be made different using annotation @COLUMN_NAME ("SOME_THING_ELSE"), then the field would be mapped to the column specified with @COLUMN_NAME. 

### House keeping fields
Every Model in Succinct extends Model interface that has these standard fields: 
1. ID - Primary numeric identifier of every model's instance (also the primary key of the corresponding table)
2. LOCK_ID, - Used as a version number for the database record. This is used for optimistic locking while doing database updates. 
3. CREATED_AT - Timestamp at which a model/table was  created.
4. UPDATED_AT - Timestamp at which a model/table was  updated.
5. CREATOR_USER_ID - Primary identifer of the user who created the model. 
6. UPDATER_USER_ID - Primary identifier of the user who updated the model.


### Referred/Parent Fields
Models may have a field refering to Primary identifier in other models. 
for e.g A Contact model could refer to City. 
```java
  public long getCityId();
  public void setCityId(long cityId);
```

Q. Looks like a normal numeric field. So how does this make it refer to the Model called City?

A. It doesnot. To describe that it does refer to City, an additonal method needs to be declared. 
```java
  public City getCity();
``` 
When the fieldGetters and a corresponding Model getter is defined, it becomes a reference field to another model. 

Some of the examples of Reference fields would be 
```java 
  long getCityId(); void setCityId(long cityId); City getCity();
  long getPreviousCityId(); void setPreviousCityId(long orderId); City getPreviousCity(); 
```



### Children 
Just as A model can refer to other models via Reference Getters. A model can track models that have their reference. 
```java
/* In OrderLine model Order is refered*/
public interface OrderLine extends Model { 
  long  getOrderId(); void setOrderId(long orderId); Order getOrder();
}
/* In Order Model you can define */
public interface Order extends Model {
  //This indicates that OrderLine model has references to Order model via reference fields. 

  public List<OrderLine> getOrderLines(); 
  
}

```
The methods in a model that return a List of other models are called children getter. This  also informs Succinct to either cascade delete its children on delete of a model or to nullify bad references in the children. 

NOTE: _If the reference field is nullable (Annotated with @IS_NULLABLE), then on delete of the referred model, this field would be nullified. However, if it is marked as @IS_NULLABLE(false), then this model would  be deleted when the referred model is deleted_

SIDE NOTE: _If there are multiple reference fields in a model refering to the same parent/referred model, (e.g PREVIOUS_CITY_ID and CITY_ID say in a Contact  model) then the List method to list children of the City model can be restricted to use only one of the reference field using annotation @CONNECTED_VIA("CITY_ID" )_
``` java 
public interface City extends Model  {
  ...
  @CONNECTED_VIA("CITY_ID")
  public List<Contact> getCurrentContacts(); 
  ...
}

public interface Contact extends Model { 
  /* Primitive types are not null fields by default */
  long getCityId(); void setCityId(long cityId); City getCity();
  
  @IS_NULLABLE // Previous city may not be always available. 
  Long getPreviousCityId(); void setPreviousCityId(Long orderId); City getPreviousCity(); 
}
```

### Annotations.
Succinct uses java runtime Annotations to effectively derive meta information  about models and fields. 
#### Model Annotations
|Annotation|Usage|
|-|-
|@IS_VIRTUAL|Indicates if a model is backed by a database table or not.
|@CONFIGURATION|If the data in this table/model does not change frequently, You may mark it with this annotation. Succinct would cache this information as it would change frequently.
|@DBPOOL("some_pool") | Entities in an application may be backed by tables in diffent databases. In such situations, A DBPool may point to the appropriate jdbc connection configuration in swf.properties file.<br/>swf.jdbc.some_pool.driver=org.h2.Driver<br/>swf.jdbc.some_pool.url=jdbc:h2:./database/some_db;AUTO_SERVER=TRUE;NON_KEYWORDS=VALUE;<br/>swf.jdbc.some_pool.userid=some_user<br/>swf.jdbc.some_pool.password=some_password<br/>swf.jdbc.some_pool.validationQuery=values(1)<br/>swf.jdbc.some_pool.dbschema=PUBLIC<br/>swf.jdbc.some_pool.dbschema.setonconnection=true<br/>swf.jdbc.some_pool.set.dbschema.command=set schema public<br/>swf.jdbc.some_pool.readOnly=false
|@EXPORTABLE(true\|false)|Default is @EXPORTABLE(true).<br/>Models that are not annotated are considered EXPORTABLE(true)
|@HAS_DESCRIPTION_FIELD("SOME_FIELD")| Indicates that  the field with name SOME_FIELD, corresponding to getSomeField() is treated as description column for records in this entity. It is typically used in lookups.   
|@MENU("MAIN_MENU")| Indicates the Main menu in which this entity will be  shown for management of  this entity.
|@TABLE_NAME("SOME_NAME")| Model names and table names follow a naming convention but if you want to make them different, you can use this to do so.
|@ORDER_BY("F1,F2,F3")| Indicates the default ordering by field names while listing records of this entity.


#### Field Annotations.
Annotation|Usage
-|-
@IS_VIRTUAL| Indicates if a field is backed by a table column or not. The default meaning in absence of the annotation is the same as @IS_VIRTUAL(false)
@COLUMN_NAME("SOME_COLUMN_NAME")| Name of the table column this field is backed by
@COLUMN_SIZE(some_integer)|Size of the table column 
@DATA_TYPE(some_integer) | Type of the table column as defined in java.sql.Types class
@DECIMAL_DIGITS(some_integer) | Indicate the precision of a numeric field in the database.
@COLUMN_DEF(StandardDefault[,String])|Used to assign a default value to the column if not specified during an insertion of a record in the table.
@IS_AUTOINCREMENT|To mark an auto incremented primary key
@IS_NULLABLE| If the field can be null. 
@PASSWORD|To display **** and not show the data on the screen.
@CLONING_PROTECT|When you duplicate a database record, field marked as CLONING_PROTECT is not copied over. 
@HOUSE_KEEPING|Are fields managed by the framework. 
@ENCRYPTED|Field marked thus will be encrypted at rest. These Properties have to be set accordingly if you need this feature for sensitive fields stored in the database.<br/>swf.encryption.support=true\|false <br/>swf.key.store.directory=a_key_store_directory_name<br/>swf.key.store.password=a_key_store_password<br/>swf.key.entry.succinct.password=some_other_password_for_key_entry
@UNIQUE_KEY("comma separated key names")<br/> e.g @UNIQUE_KEY("K1,K2,K3")| All fields marked with same key names are considered to be composite unique keys of the table that backs the entity.
@Index|Lucene Index for the model would include this field.
@PARTICIPANT|The value stored in the columns  marked this are used to indentify if the logged in user has access to a table record. How it is determined is by application programmer implementing a ParticipantExtension for this Entity where the programmer can describe the values that are allowed for the column for the logged in user.


UI Annotation|Description
-|-
@CONTENT_TYPE|Fields that are binary streams can be annotated with @CONTENT_TYPE(some_mime_type)
@HIDDEN|Fields marked as @HIDDEN are not returned by default in default UI/API for the entity unless specifically asked for.
@PROTECTION |Used to denote if a field is enabled/disabled/editable/non-editable on the default ui.
@WATER_MARK(Some text) |This text is shown as a water mark in the input control for the field on the default ui.
@TOOLTIP(Some Tool tip text)| This text will show as a tool tip on the ui control on the default ui

Validation | Description
-|-
@MaxLength(number)|ensures the data in the field is limited to this length.
@MinLength(number)|ensures the data in this field is of atleast this length.
@NumericRange(min,max)|ensures that data value is between this numeric range.
@IntegerRange(min,max)|ensures that data value is between this integer range.
@ExactLength(number)|ensures that the data in the field is exactly of this length.
@Regex(a regexp)|Ensures that the data in the field satisfies the mentioned regular expression.




#### Relationship/Children Annotations
Annotation|Description
-|-
@CONNECTED_VIA|child getters marked with @CONNECTED_VIA("PARENT_ID") would return child models whose column, "PARENT_ID" is treated as the reference column to the current entity as parent. These are only needed when child models have multiple reference fields to the current entity as parent.


### Dynamic Proxy and ModelInvocationHandler 


### Model Implementation(s)

#### Method resolution 

## Persistence 
### Creating Tables
### How to CRUD 
### Model persistence extensions.
### ModelController and extending its functionality.
#### Record level actions and icons.
#### Types of View 
#### Externalizing Views Templates. 
#### Externalizing Views Vue/React
#### Content-Type  and Api Integration Adaptors
#### XLS Export and import

## Routing
### Controller identification
### Action identification

## Roles and Permissions.
### Data based Roles and Permissions(@PARTICIPANT)


## Core Plugins 


## Contributed/External Plugins

