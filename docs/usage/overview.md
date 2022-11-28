# Overview
## Introducing Succinct
<pre>
Why the name "Succinct"
Succinct: Compact precise expression without wasted words
</pre>
Succinct is a web framework that attempts to give back developers the joy of creating solutions quickly without having to sweat too much.

Succinct aims to address common application concerns (such as user managment, login,	 user-role permissions, data-role permissions, async  processing, crud....) in a way that follows proven solution patterns so that application developers can leverage these solutions in their applications with confidence. 

Allowing Framework to handle these common concerns makes application code Less buggy as it is Small,Brief and Precise. Hence the name of the framework "Succinct".

## Why another framework? 
Web Applications in most domains have similar requirements and developers often keep reinventing the wheel for each project. Succinct aims to provide standardized solutions (as plugins) to standard concerns so that writing applications on Succinct becomes easy and fun by just composing these well designed solutions for standard concerns. 

## Features 
1. Model driven developement
1. Pluggable modules.
2. All Models including framework models are extensible by applications and plugins.
1. Sophisticated data access layer that makes database access simple while being secure.
1. Auto migration of database based on Model definition(s).
1. Zero Code for crud operations, full text search, and import/export via xls
	1. A Single controller with in the framework responds restfully to crud operation end-points for all models with json/xml/html.
	3. Generic (List & Detail) Views rendered by the framework for crud operations on models.

1. Low code for non-crud business domain actions. 
	1. Application behaviour customizable through model methods  and extension points/hooks.
	2. Domain actions can be added to controllers.
	3. Views customizable via Vuejs/Reactjs and similar frameworks.
1. Favours Convention over configuration inculcating good programing practices. 
1. Embedded a Jetty Server .


## System Requirements
* java - 8+ 
* maven - 3.5+	




## Some popular plugins.

|Category| Name | Description | Bundled|
|-|-|-|-|
|Authentication |swf|open id based user authentication| bundled
|	 |swf-plugin-mobilesignup|phone/otp based authentication| plugin
|Authorization|swf(security)|user-role and data-role based authorization.| bundled
|Customizing default Views|swf| - |bundled
|Asyncronous task processing.|swf(background)| Running background jobs| bundled 
| Communication | (swf-plugin-mail)|Mails (smtp and sendgrid adaptors) | plugin
| Communication | (swf-plugin-templates)|In App Notification | plugin 
| Message Queue Adaptors |(swf-plugin-nats)| nats | plugin 
| |(swf-plugin-hivemq)|hivemq|plugin
|Collaboration|swf-plugin-collab|Company based Collaboration|plugin
| |	swf-plugin-calendar|Resource Calendars|plugin
|	| swf-plugin-ecommerce|Ecommerce - Products and Services |plugin
| | swf-plugin-beckn|Beckn protocol support|plugin
| | swf-plugin-wiki |Wiki | plugin
|Support |swf-plugin-bugs| Issue  tracker |plugin
| | [swf-plugin-audit](https://swf-plugin-audit.readthedocs.io/en/latest/usage/audit.html)|Database Audits| plugin
|Presentations | swf-plugin-slideshow|Slide show.| plugin
| | swf(attachment)|Attachments |bundled
|ETL |swf-plugin-datamart|Datamart	| plugin

## Installing Succinct
Succinct framework gets auto installed via maven when you try to create an application. However, if you  wish to be on the development version of Succinct, you can clone the relevant github repositories manually and build them locally. 

|Repository Name | Instructions|
|-|-
|common |git clone https://github.com/venkatramanm/common.git <br> cd common <br>mvn install <br>|
|reflection |git clone https://github.com/venkatramanm/reflection.git <br> cd reflection <br>mvn install <br>|
|swf-all|git clone https://github.com/venkatramanm/swf-all.git <br> cd swf-all <br>mvn install|


