# Quick Start 
## Developing your first Succinct Application 
This section we will see how to write an application to store personal contacts. We will use this example to learn various aspects of developing applications on Succinct Framework. We have intentionally not  taken the conventional Hello World as it is not a representative application of any real world application. 

|Prerequisites|
|-|
|Java >= 1.8 (We recommend use  of jdk LTS 17)|
|maven >= 3.8.3|

## The contacts Application

### Iteration 0
#### Requirements
I want to store my contacts in an easily searchable fashion so that When I plan to visit a city, I can know my friends who are staying in that city. 

#### Preparing the application scaffold. 
1. Create the application scaffold, using the maven archetype, swf-archetype.

	mvn archetype:generate -DarchetypeGroupId=com.github.venkatramanm.swf-all 
		-DgroupId=my.group -DartifactId=contacts -Dversion=1.0-SNAPSHOT  -DarchetypeArtifactId=swf-archetype -DarchetypeVersion=[latest](https://repo1.maven.org/maven2/com/github/venkatramanm/swf-all/swf-archetype/maven-metadata.xml)

2. Accept the default options when prompted
3. Folder called contacts is created in the directory where you ran the last command.

#### Creating Models
1. Under "src/main/java", Navigate to my.group.contacts.db.model package and refer to Sample.java to see how a model is created. You can use this file as reference and delete it later. 
2. Lets Create the model Contact.java  (Notice the use of singular for naming java class files)
``` java
package my.group.contacts.db.model;

import com.venky.swf.db.annotations.column.indexing.Index;
import com.venky.swf.db.annotations.column.validations.RegEx;
import com.venky.swf.db.model.Model;

public interface Contact extends Model {
    @Index // Make this field searchable.
    public String getName();
    public void setName(String name);

    @RegEx("\\+91[0-9]{10}") //allow only india numbers.starting with +91, then 10 digit phone number.
    public String getPhoneNumber();
    public void setPhoneNumber(String phoneNumber);

    @RegEx("[A-z|0-9]+@[A-z|0-9]+[.][A-z]+")// Very simplistic email pattern.
    public String getEmail();
    public void setEmail(String email);

    @Index //Make this field searchable.
    public String getCityName();
    public void setCityName(String cityName);
}
```
#### Configuring Ports
```shell
vi overrideProperties/config/envvars 

```
Change the port numbers to where you would like the embedding jetty server to start listening for incoming http requests. I am changing to 30000 & 30030. (dport is for attaching debugger from an ide and wport is for web port)

#### Start the Application.

``` shell
chmod +x bin/* 
bin/swfstart 
```


#### Open Application in the [Browser](http://localhost:30030/)
1. Login as user root and password root. (Yes root is a default admin user for your application)
2. You will see some models on the application menu but you will not see your Contacts model. 
3. Lets add Contacts to an existing menu item (like Sample) by adding the annotation @MENU to the Contact class.
``` java
...
import com.venky.swf.db.annotations.model.MENU;
...
@MENU
public interface Contact extends Model {
...
}
```
7. Bring up the application and open in the browser as you have seen in Step 4 and 5.
8. Now you will see the Contacts Menu item under Manage. 
	* If you wist to open the model under any other menu or a new menu in its own right, Change @MENU to @MENU("Some Menu Label")*
9. Navigate via menu to Manage->Contacts.
10. By clicking the + icon add a new Contact. 
11. Then click on [Save & More] Button and add another contact in a different city and then Click on [Done] 
12. See that you can enter the city name and search by city on the list screen.
13. Oh oh I am able to create duplicate records!!
	* I need to make some of the columns as unique key to avoid duplicates. 
``` java
package my.group.contacts.db.model;
import com.venky.swf.db.annotations.column.UNIQUE_KEY;
...
public interface Contact extends Model {
    ...
    @UNIQUE_KEY("PHONE")
    public String getPhoneNumber();
    ...
    
    @UNIQUE_KEY("EMAIL")
    public String getEmail();
}
/* So phone number is one unique key. and Email is another. 
No two contacts can have the same Email. 
No two contacts can have the same Phone number. 

Note that void values of these fields are allowed for multiple contacts. 

*/
```	

 

### Iteration 1
#### Requirements 
I am making mistakes in tagging a contact's city correctly, So I would like the city's name to come from a master list. 


#### Creating Model City.java 
``` java
package my.group.contacts.db.model;

import com.venky.swf.db.annotations.column.UNIQUE_KEY;
import com.venky.swf.db.annotations.model.MENU;
import com.venky.swf.db.model.Model;

@MENU
public interface City extends Model {
    @UNIQUE_KEY
    public String getName();
    public void setName(String name);
}

```
#### Change Contacts to refer to City model.
``` java
... 
public interface Contact extends Model {
...
    /* Comment out the City name field.
    @Index //Make this field searchable.
    public String getCityName();
    public void setCityName(String cityName);
		*/
		/* add a relation ship to City model as given below */
    @Index
    public Long getCityId(); // This is a regular getter.
    public void setCityId(Long id); // Parent id , regular setter.
    public City getCity(); // Tells the framework that this id  is a reference to the City Model.
...
}
```

#### Restart the application and open in [Browser](http://localhost:30030/)
1. You will see that the cityName  field is automatically removed from the table and the new city field shown is blank!  (this is auto migration of db schema)
	* Removing field removes it from the database. 
	* Adding field add to the table 
3. Go to Manage-> Cities and add 2 new cities
4. Now go to Manage contacts and edit a contact and start editing the city field.
	* Notice auto complete showing from cities entity. 
	* Select city from the auto complete and save the contacts. 
	

### Iteration 2
#### Requirements 
I would like to search my contacts by city name or the contact's name, number or email. 

#### Index all relevant fields.
``` java
package my.group.contacts.db.model;

import com.venky.swf.db.annotations.column.UNIQUE_KEY;
import com.venky.swf.db.annotations.column.indexing.Index;
import com.venky.swf.db.annotations.column.validations.RegEx;
import com.venky.swf.db.annotations.model.MENU;
import com.venky.swf.db.model.Model;

@MENU
public interface Contact extends Model {
    @Index // Make this field searchable.
    public String getName();
    public void setName(String name);

    @RegEx("\\+91[0-9]{10}") //allow only india numbers.starting with +91, then 10 digit phone number.
    @UNIQUE_KEY("PHONE")
    @Index
    public String getPhoneNumber();
    public void setPhoneNumber(String phoneNumber);

    @RegEx("[A-z|0-9]+@[A-z|0-9]+[.][A-z]+")// Very simplistic email pattern.
    @UNIQUE_KEY("EMAIL")
    @Index
    public String getEmail();
    public void setEmail(String email);

/*    @Index //Make this field searchable.
    public String getCityName();
    public void setCityName(String cityName);
*/
    @Index
    public Long getCityId();
    public void setCityId(Long id);
    public City getCity();

}

```
#### You need to rebuild indexes after adding new indexed columns.
1.	delete .index/CONTACTS directory under the contacts application folder


#### Restart the application and open in [Browser](http://localhost:30030/)
1. login and goto /contacts
1. Search by any value in the indexed column

_Notice that you need to ignore special characters that are reserved for [luncene engine's](https://lucene.apache.org) query syntax_


### Iteration 3
Can I put this up as a service for other friends to store their contacts? May be from privacy standpoint, I may want the data to be encrypted and viewable only by the person who entered the contact information. 

#### Allowing new users to register
```shell
vi overrideProperties/config/swf.properties
```
Uncomment line, save and exit
#swf.application.requires.registration=true

#####  Restart the application and open in [Browser](http://localhost:30030/)
You will see a link "I'm a new user", which can be used to self register new users. 

#### Marking ownership of contacts created. 
````java
...
@MENU
public interface Contact extends Model {
...
    @IS_NULLABLE
    @UNIQUE_KEY("PHONE,EMAIL") // Same phone or email may be added by multiple users.
    public Long getOwnerId();
    public void setOwnerId(Long id);
    public User getOwner();
...
}
````
#### Marking Contact as a child of User . 
````java
package my.group.contacts.db.model;

import com.venky.swf.db.annotations.column.relationship.CONNECTED_VIA;
import com.venky.swf.db.model.Model;

import java.util.List;

public interface User extends Model {
    @CONNECTED_VIA("OWNER_ID")
    public List<Contact> getContacts();
}

````
#### BeforeValidateContact Extension
We need to autofill the OwnerId based on current user. 
````java
package my.group.contacts.extensions;

import com.venky.swf.db.Database;
import com.venky.swf.db.extensions.BeforeModelValidateExtension;
import my.group.contacts.db.model.Contact;

public class BeforeValidateContact extends BeforeModelValidateExtension<Contact> {
    static {
        /*Framework calls Class.forName for all classes in extensions package
        See src/main/resources/config/swf.properties
        swf.extn.package.root property points to the java package where extensions reside.
        Across all modules, succinct consolidates the extension packages and calls Class.forName on all the classes. ,
        */

        registerExtension(new BeforeValidateContact());
    }

    @Override
    public void beforeValidate(Contact model) {
        if (model.getRawRecord().isNewRecord()){
            model.setOwnerId(Database.getInstance().getCurrentUser().getId());
        }
    }
}

````
1. Extension is a pattern used in Succinct Quite extensively. It is a way the framework calls out to domain code to do additional processing. 
1. BeforeValidateExtension is called by the framework before performing any validations like not-null, regex pattern, etc for sanitity checks before doing db updates. 
1. We are checking that when Contact being saved is a new record, we should stamp the current user as the owner of the contact. 
#### Marking fields for encryption.
Fields may be marked with @ENCRYPTED annotation to auto encrypt them before persisting to database. 

````java
...

public interface Contact extends Model {
    ....
    @ENCRYPTED
    public String getPhoneNumber();
    ...
    @ENCRYPTED
    public String getEmail(); 
    ...
}
````
_Note , succinct encrypts using a server level AES Key stored in  a java key store_
```shell
vi overrideProperties/config/swf.properties
```
* Update the following encryption related properties.

swf.encryption.support=true
swf.key.store.directory=./.keystore
swf.key.store.password=mypassword
swf.key.entry.succinct.password=myentrypassword

**Most important. If you use encryption and you lose the keys stored in your keystore directory, your data cannot be decrypted. Please take backup of .keystore folder and store it carefully. Cannot stress more on the importance of this**

#### Contact Privacy
Now to restrict viewability of contacts only to users who have created them 
##### Annotate OwnerId field with @PARTICIPANT
``` java
...
public interface Contact extends Model {
...
  @IS_NULLABLE
  @UNIQUE_KEY("PHONE,EMAIL")
  @PARTICIPANT
  public Long getOwnerId();
...
}
```

##### Create ContactParticipantExtension 
````java
package my.group.contacts.extensions;

import com.venky.core.util.ObjectUtil;
import com.venky.swf.db.extensions.ParticipantExtension;
import com.venky.swf.db.model.User;
import my.group.contacts.db.model.Contact;

import java.util.Arrays;
import java.util.List;

public class ContactParticipantExtension extends ParticipantExtension<Contact> {
    static {
        registerExtension(new ContactParticipantExtension());
    }
    @Override
    protected List<Long> getAllowedFieldValues(User user, Contact partiallyFilledModel, String fieldName) {
        if (ObjectUtil.equals(fieldName,"OWNER_ID")){
            return Arrays.asList(user.getId());
            //Return  Valid values of owner_id field that this user can see. i.e. only self's id
        }
        return null;// No logic for any other field . Actually there are none in Contact model other than OwnerId So nothing to do.
    }
}

````
#####  Restart the application and open in [Browser](http://localhost:30030/)
You will now see that only contacts created by a user can be viewed by that user. 


### Iteration 4 
Is it possible to have a True caller type application which given a number,I can check who the caller is. (May be some limited information)

#### Add a controller action /whois
1. Lets create a custom controller for contact model. 
2. This custom controller extends the default ModelController. and provides additional functionality. 
3. Notice that the name of the controller and the url are in plural while model name is in singular. 
````java
package my.group.contacts.controller;

import com.venky.core.util.Bucket;
import com.venky.swf.controller.ModelController;
import com.venky.swf.db.Database;
import com.venky.swf.path.Path;
import com.venky.swf.sql.Expression;
import com.venky.swf.sql.Operator;
import com.venky.swf.sql.Select;
import com.venky.swf.views.View;
import my.group.contacts.db.model.Contact;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeSet;

public class ContactsController extends ModelController<Contact> {
    public ContactsController(Path path) {
        super(path);
    }
    public View whois(String phoneNumber){
        if (phoneNumber.length()==10){
            phoneNumber = "+91" + phoneNumber;
        }else  if (phoneNumber.length() == 12){
            phoneNumber = "+" + phoneNumber;
        }
        Expression expression = new Expression(getReflector().getPool(),"PHONE_NUMBER", Operator.EQ,phoneNumber);

        List<Contact> records= new Select().from(getModelClass()).where(expression).execute(MAX_LIST_RECORDS);


        Map<String, Bucket> names = new HashMap<>();

        for (Contact record : records) {
            Bucket numberOfTimes = names.get(record.getName());
            if (numberOfTimes ==  null){
                numberOfTimes = new Bucket();
                names.put(record.getName(),numberOfTimes);
            }
            numberOfTimes.increment();
        }

        SortedSet<String> mostProbableNames = new TreeSet<>(new Comparator<String>() {
            @Override
            public int compare(String o1, String o2) {
                return names.get(o2).compareTo(names.get(o1));
            }
        });
        mostProbableNames.addAll(names.keySet());

        Contact contact = Database.getTable(Contact.class).newRecord();
        contact.setName(mostProbableNames.isEmpty()? "Unknown" : mostProbableNames.first());
        contact.getRawRecord().setNewRecord(false);
        return show(contact);

    }

}


````
####  Restart the application and open in [Browser](http://localhost:30030/)
1. Login as any user. 
2. You can now type in url   http://localhost:30030/contacts/whois/SomePhoneNumber. 
3. You will see the contact name in the output based  on the phonenumber of the contact. 


### Iteration 5 
Can this be exposed as a json api? 

#### Response By Content-Type. 
1. By passing content-type=application/json, you can get responses as json 
2. To call an Api, you need to pass an ApiKey associated with a user in the http header.
3. To know the api key, you can use the login api with user/password to know the api key. This key changes based on a security policy. so, you are advised to handle Request Status of 401 (Unauthorized) to try login again. 

```shell 
$ curl -H 'content-type:application/json' "http://localhost:30030/login" -d '{ "User" : {"Name" : "venky" , "Password" :"venky12" }}' 

{
  "User" : {
    "ApiKey" : "9927ae0bcd45f32ad0af1205c11bd6ee30e940e1"
    ,"Id" : "33"
    ,"Name" : "venky"
  }
}

$ curl -H 'content-type:application/json' -H 'ApiKey:9927ae0bcd45f32ad0af1205c11bd6ee30e940e1' "http://localhost:30030/contacts/whois/9845114558" 
{
  "Contact" : {
    "Id" : "0"
    ,"LockId" : "0"
    ,"Name" : "Venky"
  }
}

$ curl -H 'content-type:application/xml' -H 'ApiKey:9927ae0bcd45f32ad0af1205c11bd6ee30e940e1' "http://localhost:30030/contacts/whois/9845114558"

<?xml version="1.0" encoding="UTF-8"?>
<Contact Id="0" LockId="0" Name="Venky"/>
````

### Iteration 6
How can I restrict the fields I want to see in a model while calling the apis. 

#### IncludedModelFields

1. An http header "IncludedModelFields" may be passed to restrict fields needed in response to an api for each model that the api returns.

2. The value of IncludedModelFields is a Base64 encoded json whose structure is as follows. 
{ 
	"Model1" : ["FieldName1","FieldName2"]
	"Model2" : [ "FieldName3"]
 }

e.g If you wanted to see only "Name", "PhoneNumber"  from the call to /contacts 

Convert the json string  '{ "Contact" : ["Name","PhoneNumber"] }' to base64 encoding ( eyAiQ29udGFjdCIgOiBbIk5hbWUiLCJQaG9uZU51bWJlciJdIH0K ) and pass this as value of the header field "IncludedModelFields' 

```` shell

$ curl -L -H 'content-type:application/json' -H 'IncludedModelFields:eyAiQ29udGFjdCIgOiBbIk5hbWUiLCJQaG9uZU51bWJlciJdIH0K' -H "ApiKey:2fb0340cc8cce98fe75a4c38c0d7846d4cf731b4" http://localhost:3030/contacts  

{
  "Contacts" : [{
    "Name" : "Venky"
    ,"PhoneNumber" : "+919845114558"
  }]
}


````	


**Note, If a model appears in multiple xml/json paths for an api, the attributes in them would be the same at all such paths.**


