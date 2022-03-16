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

for e.g. When the name of  a Model is Sample, The corresponding table would be named as SAMPLES. 

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

Succinct 2.8 supports the derby,mysql,postgres,sqlite,sqldroid and h2 for databases to be used with succinct applications.

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
Succinct uses java runtime Annotations to effective derive 
#### Field Annotations.
#### Model Annotations
#### Relationship/Children Annotations
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

