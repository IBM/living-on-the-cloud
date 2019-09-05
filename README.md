# Connecting Spring Boot to a Cloud Hosted Database

## Prerequisites

You will have wanted to completed the [previous article](https://developer.ibm.com/tutorials/living-on-the-cloud-1/) in this series as this article directly builds off the lessons learned and operations completed in the previous. You can find the starting code for this article [here](https://github.com/wkorando/living-on-the-cloud/tree/2-connecting-to-a-database). 

## Cloud Native Data Persistence

IBM Cloud provides a number of options for [persisting data](https://cloud.ibm.com/catalog?search=database). In this article we will setup a PostgreSQL database for handling our data persistance needs. IBM Cloud provides a couple of options for PostgreSQL databases as well, [a first-party option](https://cloud.ibm.com/catalog/services/databases-for-postgresql) hosted directly on IBM Cloud, and a [third-party option](https://cloud.ibm.com/catalog/services/elephantsql), ElephantSQL, which is hosted off premises. In this series we will be using the PostgreSQL option provided by IBM, however note there is only a pay-as-you-go tier. If you'd still like to experiment with IBM Cloud for free, ElephantSQL offers a free-tier "Lite". There are a few minor differences when it comes to configuration, but you can still complete the steps in this aritcle using ElephantSQL. 

If you have not already, login into to [https://cloud.ibm.com/](https://cloud.ibm.com/). Once logged in, in the search box in the top center of the page search for `PostgreSQL` and select the **Databases for PostgreSQL** option: 

![](postgres-lookup.png)

On the screen for configuring the database, change the name to `living-on-the-cloud-db` if you want to follow along with the application being built in this series. The rest of the default values are fine for the purposes of the applications we will be building in this series. Be sure to scroll to the bottom of page to see the cost of running a PostgreSQL instance, you can also use the cost estimator to view the cost of running a PostgreSQL  will be per month. 

Later in this article we will be doing some configuration work on the database from the command line, so you will need to downloand and install the the `psql` command line tool. Steps on how to do that can be found [here](https://www.postgresql.org/download/). 

It will take a few minutes before the PostgreSQL database is ready as well as for the download to complete, so we while that happens we will continue on by updating the `storm-tracker` application to handle database persistence. 

## Handling the CRUD with Spring Data 

Persisting to and reading from a database is a common requirement. Cloud platforms, as we see above, can handle the hosting of a database, but how do we handle the communication to that database within the application itself? 

A lot of database communication is relatively simple; add a new record, look up records, update an existing record, remove a record, in otherwords; create, read, update, delete, or CRUD. Despite the relative simplicity of these tasks, to do it right, can require a substanstial amount of boilerplate code; configure a datasource, handling connections, map fields, and so on. In short, even a relatively simple requirement of looking up a record might require a decent investment in developer time. 

Luckily a lot of this behavior can be encapsulated with Spring Data and JPA, greatly reducing the amount of work surrounding database communication. Let's walk through the steps of updating the **storm-tracker** application we built in the previous article to use Spring Data. 

The first step will the update the **pom.xml** to adding these dependencies:

```xml
<dependency>
	<groupId>org.springframework.boot</groupId>
	<artifactId>spring-boot-starter-data-jpa</artifactId>
</dependency>
<dependency>
	<groupId>org.postgresql</groupId>
	<artifactId>postgresql</artifactId>
</dependency>
```

**spring-boot-starter-data-jpa** will bring in the Spring Data libraries and underlying JPA and hibernate libraries for handling the object relational mapping (ORM) and database transaction behavior. 

**postgresql** is needed as we are connecting to a PostgreSQL database. 

Also be sure to update the **version** tag to **0.0.2-SNAPSHOT**. This will be important later when we upload the new version of our code to the container registry:

```
<version>0.0.2-SNAPSHOT</version>
```

With **pom.xml** updated, the next step is to create the entity model. As the name of the service is **storm-tracker**, let's create a class called `Storm` that we will use for storing records of storms we want to track. Under `com.ibm.developer.stormtracker` you will create the class `Storm`, we will walk through all the elements in this class below: 

```java
@Entity
@Table(name = "storms")
public class Storm {

	@Id
	@GeneratedValue(generator = "storms_id_generator")
	@SequenceGenerator(name = "storms_id_generator", allocationSize = 1, initialValue = 10)
	private long id;
	private String startDate;
	private String endDate;
	private String startLocation;
	private String endLocation;
	private String type;
	private int intensity;

	Storm() {
	}

	public Storm(String startDate, String endDate, String startLocation, String endLocation, String type,
			int intensity) {
		this.startDate = startDate;
		this.endDate = endDate;
		this.startLocation = startLocation;
		this.endLocation = endLocation;
		this.type = type;
		this.intensity = intensity;
	}

	public long getId() {
		return id;
	}

	public String getStartDate() {
		return startDate;
	}

	public String getEndDate() {
		return endDate;
	}

	public String getStartLocation() {
		return startLocation;
	}

	public String getEndLocation() {
		return endLocation;
	}

	public String getType() {
		return type;
	}

	public int getIntensity() {
		return intensity;
	}

}
```

* `@Entity` marks a class as an entity to be tracked by JPA. 

* `@Table` maps the entity to the table the entity will be persisted to, the `name` field defines the name of the table. This annotation is optional, if not defined `Storm` would be mapped to the table `Storm`.

* `@Id` marks this field as a primary key. 

* `@GeneratedValue` marks this field as having its value automatically generated. The `generator` field is an optional field for defining the generator to be used for creating the generated value.

* `@SequenceGenerator` defines a generator that can be used by `@GeneratedValue` for generating a value. Which type a generator a underlying database supports will vary(provide link).

* By default JPA will map all the fields in a class to columns and convert the camelCase notation used in Java to snake\_case typically used in databases, for example `startLocation` translates to a column name of `start_location`. 

Spring Data uses the [repositories](ahttps://docs.spring.io/spring-data/jpa/docs/current/reference/html/#repositories) concept for handling database interaction. Spring Data provides several [pre-defined interfaces](https://docs.spring.io/spring-data/commons/docs/current/api/org/springframework/data/repository/Repository.html). We will be using `CrudRepository<T, ID>` ([javadoc](https://docs.spring.io/spring-data/commons/docs/current/api/org/springframework/data/repository/CrudRepository.html)) which itself has several pre-dfined methods including looking up by id, persisting an entity to the database, retreiving all entities, and others. 

Generally the predefined queries won't be enough to handle your needs, so Spring Data provides a couple of mechanisms for defining custom queries. For more complex queries there is `@Query` which allows a query to be defined using [JPQL](https://docs.oracle.com/html/E13946_04/ejb3_langref.html). For simple queries however, you can use Spring Data's semantic query engine. Spring Data is able to derive a query from the name of a [method signature](https://docs.spring.io/spring-data/jpa/docs/current/reference/html/#repositories.query-methods.query-creation) defined in a repository interface. For example if we wanted to lookup all storms by where they started, we can do that by writing the method signature `findByStartLocation(String startLocation)`. 

Using the above we have a repository interface that looks like this: 

```java
public interface StormRepo extends CrudRepository<Storm, Long> {
	public Iterable<Storm> findByStartLocation(String type);
}
```

We will want to be able to interact with the repository, so let's update `StormTrackerController`. Generally there would be a service class(es) between a controller and a repository class that would contain business logic, input validation, merging results, amending values, etc.. We don't have those requirements yet, so adding a service class would at this point be an unnecessary abstraction. 

Our updated controller class should look like this:

```java
@RestController
@RequestMapping("/api/v1/storms")
public class StormTrackerController {

	private StormRepo repo;

	public StormTrackerController(StormRepo repo) {
		this.repo = repo;
	}

	@GetMapping
	public ResponseEntity<Iterable<Storm>> findAllStorms() {
		return ResponseEntity.ok(repo.findAll());
	}
	
	@GetMapping("/{stormId}")
	public ResponseEntity<Storm> findById(@PathVariable long stormId) {
		return ResponseEntity.ok(repo.findById(stormId).get());
	}

	@PostMapping
	public ResponseEntity<?> addNewStorm(@RequestBody Storm storm) {
		storm = repo.save(storm);
		return ResponseEntity.created(URI.create("/api/v1/storms/" + storm.getId())).build();
	}
}
```

The last step with our application will be to update the `application.properties` file to define our datasource. However because many of the values of those properties will depend on some configuration work we will be doing in the next steps, we will hold of on this step for now.  

For more information on how to use Spring Data be sure to checkout it's [project page](https://spring.io/projects/spring-data). 

## Configuring the Database and Kubernetes Cluster

By now the PostgreSQL database should have finished initializing and **psql** should be installed on your system. 

The first step we will need to do is create credentials which we will use to configure the the database. To do that head back to [https://cloud.ibm.com/](https://cloud.ibm.com/). 

In the top center of the page search for **living-on-the-cloud-db** and select the result that comes up. 

This brings up the dashboard for the PostgreSQL database service.

**Note:** If you are unable to open the page, the database might not yet be provisioned. In which case wait a few more minutes before continuing. 

Scroll down toward the bottom of the page you should see a field labeled **TLS certificate**. This contains the self-signed certificate for the database. We can use this to have secure communication between our application and the database.

![](certificate.png)

You will want to create a file called **root.crt** in `~/.postgresql` (or `%APPDATA%\postgresql` if you are using Windows). Copy everything in the **contents** text field into the **root.crt** file just created and save it. **psql** will use this certificate when we connect to the PostgreSQL database in just a moment.

On the left side of the screen select the option that says **Service credentials**.

![](service-credentials-highlight.png)

From thie service credentials page we can create new credentials. To do so, on the right side of the page click the button blue button that says **New credential +**

![](create-credentials.png)

The defaults are fine, though if you want a more memorable name than "Service credentials-1" feel free to change it. 

Once the credentials are created click **View credentials** which should drop down a text box with JSON in it. In that text box look for the value **composed** which should look like the below:

![](command-line-values.png)

Open a command line terminal and copy/paste the above values into your terminal, **but don't hit enter**. You will want to remove the **PGSSLROOTCERT** value, as we have already added the cert to the **.postgresql** folder earlier.

With **PGSSLROOTCERT** removed hit enter and you should be signed into your database. From here we can configure the database. Let's update the database to match the **Storm** JPA entity we just created and add a few records. The below sql statements should accomplish this:

```sql
create sequence storms_id_generator start 10 increment 1;

create table storms (id int8 not null, end_date varchar(255), end_location varchar(255), intensity int4 not null, start_date varchar(255), start_location varchar(255), type varchar(255), primary key (id));

insert into storms (id, start_date, end_date, start_location, end_location, type, intensity) values (nextval('storms_id_generator'), '10-10-2018', '10-13-2018', 'Denver, Colorado', 'Kansas City, Missouri', 'Thunderstorm', 2);

insert into storms (id, start_date, end_date, start_location, end_location, type, intensity) values (nextval('storms_id_generator'), '01-15-2019', '01-17-2019', 'Atlantic Ocean', 'New York City, New York', 'Blizzard', 4);
```

Once all the above statements have been run, you can log out of the database with the following:

```
\q
``` 

Next step will be to sign into IBM Cloud with the cli tool and point **kubectl** to your kubernetes cluster. I cover how to do these steps in the [previous article](https://developer.ibm.com/tutorials/living-on-the-cloud-1/#deploy-to-kubernetes).

Once signed in we will create a binding between the kubernetes cluster and the PostgreSQL database. To do that you will want to run the following: 

```
ibmcloud ks cluster-service-bind living-on-the-cloud --service living-on-the-cloud-db --namespace default
```

This command will create a new service credential for the PostgreSQL database and bind it to the kubernetes cluster. Kubernetes will then store that binding as a secret. [Kubernetes secrets](https://kubernetes.io/docs/concepts/configuration/secret/) are a secure way of storing sensitive information, like database credentials, in your Kubernetes cluster. 

To view all the secrets a kubernetes cluster is storing you can run the following command:

```
kubectl get secrets --namespace=default
```

To get more detailed information on a secret you can run the following: 

```
kubectl get secret binding-living-on-the-cloud-db -o yaml
```

The output should look something like this. The real important element though is the base64 encoded string. 

```
apiVersion: v1
data:
  binding: <base64 encoded string>
kind: Secret
metadata:
  annotations:
    created-by-cluster-service-bind: "true"
    service-instance-id: 'crn:v1:bluemix:public:databases-for-postgresql:us-south:a/****::'
    service-key-id: 6fc0c416-f598-48f4-8d30-918730a910f8
  creationTimestamp: "2019-08-06T22:28:25Z"
  name: binding-living-on-the-cloud-db
  namespace: default
  resourceVersion: "745159"
  selfLink: /api/v1/namespaces/default/secrets/binding-living-on-the-cloud-db
  uid: 81a54292-b899-11e9-82a3-ce58a894cd59
```

The binding field is what we are really interested in, as it contains all the connection information we will be needing later. We can view the contents of the field by decoding it:

```
echo <base64 string> | base64 --decode
```

It might be a bit difficult to read in the terminal, but the return is a JSON object with connection string, user, password, and other data necessary to connect to the PostgreSQL database. This information can also be viewed from the **Service credentials** page we were on just a little earlier as well.

In the next section we will update the Kubernetes deployment and our Spring Boot application to access a Kubernetes secret. 

## Updating the Kubernetes Deployment and Spring Boot Application

Kubernetes uses YAML files to define how an application should be deployed and managed in a cluster. When we deployed the **storm-tracker** application in the previous article a YAML file was automatically generated. We will need to update this YAML file so that our application can interact with the PostgreSQL database.

**Note:** At this point, if you are not already, you will want to be in the root directory of your **storm-tracker** application. 

### Updating the Deployment YAML

Let's bring down the the YAML file for **storm-tracker**. From the root of your project run the following command: 

```
kubectl get deployments storm-tracker --namespace=default -o yaml > deployment.yaml
```

Open **deployment.yaml** in a text editor, it should look something like this: 

```yaml
apiVersion: extensions/v1beta1
kind: Deployment
metadata:
  annotations:
    deployment.kubernetes.io/revision: "12"
    kubectl.kubernetes.io/last-applied-configuration: |
      {"apiVersion":"extensions/v1beta1","kind":"Deployment","metadata":{"annotations":{},"labels":{"run":"storm-tracker"},"name":"storm-tracker","namespace":"default","selfLink":"/apis/extensions/v1beta1/namespaces/default/deployments/storm-tracker"},"spec":{"progressDeadlineSeconds":600,"replicas":1,"revisionHistoryLimit":10,"selector":{"matchLabels":{"run":"storm-tracker"}},"strategy":{"rollingUpdate":{"maxSurge":"25%","maxUnavailable":"25%"},"type":"RollingUpdate"},"template":{"metadata":{"creationTimestamp":null,"labels":{"run":"storm-tracker"}},"spec":{"containers":[{"args":["--spring.application.json=$(BINDING)"],"env":[{"name":"BINDING","valueFrom":{"secretKeyRef":{"key":"binding","name":"binding-living-on-the-cloud"}}}],"image":"us.icr.io/openj9-demo/storm-tracker:0.0.2-SNAPSHOT","imagePullPolicy":"Always","name":"storm-tracker","resources":{},"terminationMessagePath":"/dev/termination-log","terminationMessagePolicy":"File"}],"dnsPolicy":"ClusterFirst","restartPolicy":"Always","schedulerName":"default-scheduler","securityContext":{},"terminationGracePeriodSeconds":30}}}}
  creationTimestamp: "2019-08-01T22:48:32Z"
  generation: 25
  labels:
    run: storm-tracker
  name: storm-tracker
  namespace: default
  resourceVersion: "1132314"
  selfLink: /apis/extensions/v1beta1/namespaces/default/deployments/storm-tracker
  uid: 7cb6f3c5-b4ae-11e9-9a9f-461265fcff59
spec:
  progressDeadlineSeconds: 600
  replicas: 1
  revisionHistoryLimit: 10
  selector:
    matchLabels:
      run: storm-tracker
  strategy:
    rollingUpdate:
      maxSurge: 25%
      maxUnavailable: 25%
    type: RollingUpdate
  template:
    metadata:
      creationTimestamp: null
      labels:
        run: storm-tracker
    spec:
      containers:
        image: us.icr.io/openj9-demo/storm-tracker:0.0.1-SNAPSHOT
        imagePullPolicy: Always
        name: storm-tracker
        resources: {}
        terminationMessagePath: /dev/termination-log
        terminationMessagePolicy: File
      dnsPolicy: ClusterFirst
      restartPolicy: Always
      schedulerName: default-scheduler
      securityContext: {}
      terminationGracePeriodSeconds: 30
status:
  availableReplicas: 1
  conditions:
  - lastTransitionTime: "2019-08-09T16:10:37Z"
    lastUpdateTime: "2019-08-09T16:10:37Z"
    message: Deployment has minimum availability.
    reason: MinimumReplicasAvailable
    status: "True"
    type: Available
  - lastTransitionTime: "2019-08-09T16:10:32Z"
    lastUpdateTime: "2019-08-09T16:10:37Z"
    message: ReplicaSet "storm-tracker-5c99cd9c5f" has successfully progressed.
    reason: NewReplicaSetAvailable
    status: "True"
    type: Progressing
  observedGeneration: 25
  readyReplicas: 1
  replicas: 1
  updatedReplicas: 1
```

There is a lot of instance specific information we will want to remove from this **deployment.yaml**. We want is a template for telling Kubernetes how to deploy the **storm-tracker**, not a snapshot of what **storm-tracker** looks like right now, so that way we can reuse this **deployment.yaml** in the future. 

The slimmed down **deployment.yaml** should look like this:

```yaml
apiVersion: extensions/v1beta1
kind: Deployment
metadata:
  labels:
    run: storm-tracker
  name: storm-tracker
  namespace: default
  selfLink: /apis/extensions/v1beta1/namespaces/default/deployments/storm-tracker
spec:
  progressDeadlineSeconds: 600
  replicas: 1
  revisionHistoryLimit: 10
  selector:
    matchLabels:
      run: storm-tracker
  strategy:
    rollingUpdate:
      maxSurge: 25%
      maxUnavailable: 25%
    type: RollingUpdate
  template:
    metadata:
      creationTimestamp: null
      labels:
        run: storm-tracker
    spec:
      containers:
      - image: us.icr.io/openj9-demo/storm-tracker:0.0.2-SNAPSHOT
        imagePullPolicy: Always
        name: storm-tracker
        resources: {}
        terminationMessagePath: /dev/termination-log
        terminationMessagePolicy: File
        args: ["--spring.application.json=$(BINDING)"]
        env:
        - name: BINDING
          valueFrom:
            secretKeyRef:
              name: binding-living-on-the-cloud
              key: binding
      dnsPolicy: ClusterFirst
      restartPolicy: Always
      schedulerName: default-scheduler
      securityContext: {}
      terminationGracePeriodSeconds: 30
```

The two important areas to focus on are first:

```
      - image: us.icr.io/openj9-demo/storm-tracker:0.0.2-SNAPSHOT
```

The image tag has been update to be **0.0.2-SNAPSHOT**, so when we upload a new docker image later we can be confident we are using the correct docker image. Also by changing the tag version with each deployment we can more easily track the code the was being run by the Kubernetes at a specific point in time. 

The second section we want to focus on is starts just a few lines down from **image**:

```
        args: ["--spring.application.json=$(BINDING)"]
        env:
        - name: BINDING
          valueFrom:
            secretKeyRef:
              name: binding-living-on-the-cloud-db
              key: binding
```

**Note:** In YAML whitespace has meaning, the above section should have the proper formatting already. So be sure to copy everything. 

Let's step through the YAML above to get a better understanding of what is happening: 

* `args: ["--spring.application.json=$(BINDING)"]` will append the quoted element to the execution command when starting the docker container.
* `--spring.application.json=` when using this property, Spring Boot will automatically parse the JSON object and store it as a map.
* `$(BINDING)` this is a reference to an envrionment variable **BINDING** that will be replaced with tits value when the Kubernetes cluster reads the deployment file. 
* `name: BINDING` this creates the environmnet variable for the above.
* `secretKeyRef:` this tells kubernetes the value is going from a stored secret
* `name: binding-living-on-the-cloud-db` this references the stored secret to pull values from
* `key: binding` this is the specific field within the stored secret to load. If you viewed the contents of `binding-living-on-the-cloud-db` earlier you will see `binding` was a base64 encoded string. Kubernetes will automatically decode it. In this case the econded data is a JSON object.

### Updating the application.properties

Next we will need to update the `application.properties` file of our Spring-Boot application, here is what the completed file should look like, again we will step through what is happening below:

```
spring.datasource.url=jdbc:postgresql://${connection.postgres.hosts[0].hostname}:${connection.postgres.hosts[0].port}${connection.postgres.path}?sslmode=${connection.postgres.query_options.sslmode}
spring.datasource.username=${connection.postgres.authentication.username}
spring.datasource.password=${connection.postgres.authentication.password}
spring.datasource.driver-class-name=org.postgresql.Driver
spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.PostgreSQL95Dialect
```

There are several things to understand here. The first is that Spring Boot has an [order of precedence](https://docs.spring.io/spring-boot/docs/current/reference/html/boot-features-external-config.html) when it comes to properties resolution. Follow the link for the full list, but for here command line arguments and `spring.application.json` are resolved before properties in a properties file. Because of that behavior, we can reference values supplied through those methods in our `applications.properties` file. 

As mentioned above, Spring Boot will store JSON passed into `spring.application.json` as a map. In the previous section we also looked at the decoded JSON contents of the binding field. In **application.properties** we are referencing elements within from the JSON to fill out all the needed properties for constructing the datasource bean.

Spring Boot is able to construct a number of beans and drive other behavior purely from passed in properties. Here we are constructing a datasource, but the here are a lot of other beans that can be contructued or behavior defined though with Spring properties. [This document](https://docs.spring.io/spring-boot/docs/current/reference/html/common-application-properties.html) describes some of the most common ones. 

### Updating the Dockerfile

The final step we will need to update the Dockerfile from the last article. 

The first step will be to copy over the **root.crt** from `~/.postgresql`/`%APPDATA%\postgresql` into the root of the project directory.

In the Dockerfile you will want to add the following two lines to it:

```
RUN mkdir /root/.postgresql/

COPY root.crt /root/.postgresql/
```

The completed Dockerfile should look like this:

```
FROM adoptopenjdk/openjdk8-openj9:alpine-slim

COPY target/storm-tracker.jar /

RUN mkdir /root/.postgresql/

COPY root.crt /root/.postgresql/

ENTRYPOINT ["java", "-jar", "storm-tracker.jar" ]
```

This copies the crt into the docker image, which will be used to encrypt the communication between our application and the database. 

**Note:** The docker image we are creating is using Linux so even if you are on Windows you will still use `/root/.postgresql/` as the path in the Dockerfile. 

With our deployment file, code, and dockerfile all updated, lets build it all and send a new docker image to our repository by running the following:

```
mvn package docker:build -Ddocker.username=iamapikey -Ddocker.password=<your api-key> docker:push
```

Finally we need to update docker with the new deployment information from. We can send the **deployment.yaml** we updated earlier by executing this:

```
kubectl apply -f deployment.yaml
```

You should get a response back that looks something like this:

```
deployment.extensions/storm-tracker configured
```

Wait about 30 seconds while the application starts up and connects to the database. After waiting run a **curl** command, or go by browser to the your application: curl **http://\<node-ip\>:\<node-port\>/api/v1/storms**. Instructions on how to look up your node-ip and node-port can be found in the [previous article](https://developer.ibm.com/tutorials/living-on-the-cloud-1/#deploy-to-kubernetes). You should get a JSON return that looks like this:

```json
[  
   {  
      "id":10,
      "startDate":"10-10-2018",
      "endDate":"10-13-2018",
      "startLocation":"Denver, Colorado",
      "endLocation":"Kansas City, Missouri",
      "type":"Thunderstorm",
      "intensity":2
   },
   {  
      "id":11,
      "startDate":"01-15-2019",
      "endDate":"01-17-2019",
      "startLocation":"Atlantic Ocean",
      "endLocation":"New York City, New York",
      "type":"Blizzard",
      "intensity":4
   }
]
```

## Summary

Provisioning and setting up services like a database traditionally can take weeks if not longer. Cloud platforms allow organizations to setup a database and connect an application to it in an afternoon. This greatly increases the agility of organizations and like mentioned in the introduction, allows them, and their developers, to focus on their core business goals. 

One problem you might have noticed in this exercise is the need to interact with Kubernetes a lot manually. While it's not too big an issue at the small scale of this exercise, and is useful for getting more familiar with Kubernetes and kubectl, it's not something that will scale in the real world applications. In the next article in the series we will look at how to setup pipelines to automate the process of building, testing, and deploying our applications. 

The code used in this tutorial can be found in my [GitHub repository](https://github.com/wkorando/living-on-the-cloud) under the **2-connecting-to-a-database** branch.