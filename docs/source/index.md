Succinct Web Framework
======================
Docs: [Home](http://www.succinct.in)


Creating webapp using SWF
-------------------------

mvn archetype:generate -DarchetypeGroupId=com.github.venkatramanm.swf-all  -DarchetypeArtifactId=swf-archetype -DarchetypeVersion=[latest](http://repo1.maven.org/maven2/com/github/venkatramanm/swf-all/swf-all/maven-metadata.xml) -DgroupId=your_application_group_id -DartifactId=your_artifact_id -Dversion=1.0-SNAPSHOT


Bringing up your application
----------------------------

Find your project in a subdirectory by the name of your artifactId.

cd your\_artifact\_id

Modify src/main/resources/config/swf.properties and set appropriate jdbc connection parameters. The Default properties would connect to an embedded derby instance that would be automatically created on application deployment.

mvn compile exec:java -Dexec.mainClass="com.venky.swf.JettyServer"

Your webapp is up on port localhost:8080. You can add or remove model interfaces and see how it effects the Application. 

Note: The default user created is root with password root. You can change the password of this user after you login to the application the first time.


Reverse Engineering
------------------
If you have a db where you have already created some tables, you can generate model interfaces for them by running the reverse engineering swf plugin. Remember to compile before generating models as the plugin depends on projects resources such as swf.properties etc to be on classpath. 

mvn compile swf:generate-model 


