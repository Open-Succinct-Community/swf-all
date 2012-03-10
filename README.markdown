swf-all
=======

Complete SWF including various modules.

Requirements:
-------------
git, mvn, mysql/derby  

Creating webapp using Released SWF
----------------------------------

mvn archetype:generate -DarchetypeGroupId=com.github.venkatramanm.swf-all  -DarchetypeArtifactId=swf-archetype -DarchetypeVersion=1.1.3 \
-DgroupId=[your application group id] -DartifactId=[your artifact id] -Dversion=1.0-SNAPSHOT

* You may want to skip subsequent sections and go directly to "Bringing up your application". 

Building SWF From sources
-------------------------

git clone git://github.com/venkatramanm/swf-all.git 

cd swf-all

git submodule init

git submodule update

mvn install 

Keeping SWF Sources updated
---------------------------
cd swf-all

git pull

git submodule update

mvn install 

Using SWF Built locally
-----------------------

mvn archetype:generate -DarchetypeCatalog=local 

In interactive mode Choose number corresponding to swf-archetype and answer the questions asked.  


Bringing up your application
----------------------------

Find your project in a subdirectory by the name of your artifactId.

cd "your artifact id"

Modify src/main/resources/config/swf.properties and set appropriate jdbc connection parameters. 

mvn compile exec:java -Dexec.mainClass="com.venky.swf.JettyServer"

Your webapp is up on port localhost:8080. You can add or remove model interfaces and see how it effects the Application.  

Note: The default user created is root with password root. You can change the password of this user from your app. 


Reverse Engineering
------------------
If you have a db where you have already created some tables, you can generate model interfaces for them by running the reverse engineering swf plugin. Remember to compile before generating models as the plugin depends on projects resources such as swf.properties etc to be on classpath. 

mvn compile swf:generate-model 


