swf-all
=======

This is a place holder project to pull all relevant modules needed for web development using  SWF. 

Requirements:
-------------
git, mvn, mysql/derby  

Installing
------------

git clone git://github.com/venkatramanm/swf-all.git 

git submodule init

git submodule update

cd swf-all

mvn install 

Using SWF
-----------


mvn archetype:generate -DarchetypeCatalog=local 

In interactive mode Choose number corresponding to swf-archetype and answer the questions asked. 

Find your project in a subdirectory by the name of your artifactId, (answer you gave to one of the questions asked above). 

cd "your artifact id"

Modify src/main/resources/config/swf.properties and set appropriate jdbc connection parameters. 

mvn clean compile 

mvn -e exec:java -Dexec.mainClass="com.venky.swf.JettyServer"

Your webapp is up on port localhost:8080. You can add or remove model interfaces and see how it effects the Application.  

Note: The default user created is root with password root. You can change the password of this user from you app. 
