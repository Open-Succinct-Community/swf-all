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

|Category| Description | Bundled|
|-|-|-|
|Authentication |open id based user authentication| bundled
|	 |phone/otp based authentication| plugin
|Authorization|user-role and data-role based authorization.| bundled
|Customizing default Views|  - |bundled
|Asyncronous task processing.| Running background jobs| bundled 
| Communication | Mails (smtp and sendgrid adaptors) | plugin
| Communitation | In App Notification | plugin 
| Message Queue Adaptors | nats | plugin 
| |hivemq|plugin
|Collaboration|Company based Collaboration|plugin
| |	Resource Calendars|plugin
|	| Ecommerce - Products and Services |plugin
| | Beckn protocol support|plugin
| | Wiki | plugin
|Support | Issue  tracker |plugin
| | Database Audits| plugin
|Presentations | Slide show.| plugin
| |Attachments | plugin
|ETL |Datamart	| plugin

