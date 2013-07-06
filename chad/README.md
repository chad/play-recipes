# Interact with a database using Slick

As we've mentioned previously, Play! is a full stack Web framework. This means that everything you need to build a dataabase-backed Web application is included out of the box.  From the database to the views, you don't need to add anything to a Play! application to get started. This makes life convenient for developers who are happy to go with the defaults.

However, sometimes you will want to use different components than the ones included. Perhaps you need a custom templating engine to support views created in a previous project. Or you might want to change the way you access data.

Fortunately, while Play! ships with a set of sensible defaults, it's easy to replace the built-in components with the alternative of your choice. One popular choice for accessing a database is the Slick library. In this recipe, we'll see how to install Slick into an application, configure a mapping between our tables and Scala types, and do basic CRUD operations from a Web application.

## Installation
First, given a fresh new Play! project, we'll add Slick to our project's sbt configuration file, Build.scala. In a default Play! application, the `jdbc` and `anorm` libraries are listed in the sbt file's `appDependencies` list.  Since we're replacing the database access layer for the application, we'll remove those lines and leave our `appDependencies` declaration looking like this:

```scala
  val appDependencies = Seq(
    "com.typesafe.play" %% "play-slick" % "0.3.2"
    "mysql" % "mysql-connector-java" % "5.1.18",
  )
```

Here we have configured the `play-slick` plugin as a dependency, which provides various Play! framework integration hooks in addition to the Slick library itself.  Since we're planning to use MySQL for this example, we have also included the MySQL connector.  You can see that the configuration worked by starting your application's Play! console and typing `dependencies`:

```
[my-slick-app] $ dependencies
[info] Updating {file:/Users/chad/src/recipe/my-slick-app/}my-slick-app...
[info] Done updating.
[info] :: delivering :: my-slick-app#my-slick-app_2.10;1.0-SNAPSHOT :: 1.0-SNAPSHOT :: integration :: Mon Jun 24 09:11:03 CEST 2013
[info]  delivering ivy file to /Users/chad/src/recipe/my-slick-app/target/scala-2.10/ivy-1.0-SNAPSHOT.xml

Here are the resolved dependencies of your application:

+--------------------------------------------------------+--------------------------------------------------------+----------------------------------------+
| Module                                                 | Required by                                            | Note                                   |
+--------------------------------------------------------+--------------------------------------------------------+----------------------------------------+
| com.typesafe.play:play-slick_2.10:0.3.2                | my-slick-app:my-slick-app_2.10:1.0-SNAPSHOT                | As play-slick_2.10-0.3.2.jar           |
+--------------------------------------------------------+--------------------------------------------------------+----------------------------------------+
```

## Configuration

Now that we have the Slick library installed, we will map our code to an underlying database.  The first step is to actually create a database and point our application to it. As described in (FIXME: cref to anorm/jdbc recipe), our `application.conf` file should have a section that looks something like this:

```
db.default.driver=com.mysql.jdbc.Driver
db.default.url="jdbc:mysql://localhost/scala_speakers?characterEncoding=UTF8"
db.default.user="root"
db.default.password=""
db.default.logStatements=true
```
Here we tell Play! that we have a MySQL database called `scala_speakers` accessible on our local host using the `root` username with no password (clearly not a good strategy for production access control, but it's common for a development environment).

We will also create some tables in the database.  But, rather than go into our mysql console and issue DDL commands directly, we will do this in Scala!  We will use Slick's API to specify a class which represents a new database table. In this example, we'll build a set of tables which represent conference speakers  and the talks they present at conferences. We will start with the table representing speakers.

We'll create a new directory, `app/models`, if it doesn't already exist and then create the file `app/models/Models.scala` with the following:
```scala
package models

import play.api.db.slick.DB
import play.api.db.slick.Config.driver.simple._

case class Speaker(name: String, bio: String, id: Option[Long] = None)

class Speakers extends Table[Speaker]("SPEAKERS") {
  def id =   column[Long]("id", O.PrimaryKey, O.AutoInc)
  def name = column[String]("name", O.NotNull)
  def bio = column[String]("bio", O.NotNull)
  def * = name ~ bio ~ id.? <> (Speaker.apply _, Speaker.unapply _)
  def autoInc = * returning id
}
```
Here we find a class called "Speakers", whose job is to represeent the "SPEAKERS" table in our database. We have declared it to be a `Table` of `Speaker` objects, and have explicitly mapped it to the "SPEAKERS" table in its declaration.  Inside of the class definition, we set up mappings for the database columns, using the `column` method from Slick's `Table` class. With the `column` method, we can specify the Scala type of the column, its name in the database, and a number of options. For example, our `id` field is represented in Scala as a `Long` and is both the primary key and set to auto-increment in the database.

The `*` method is special. It is required by the `AbstractTable` class from which our `Speakers` class ultimately inherits, so our code won't compile unless we define it. Without getting into too many of the nitty gritty details, its purpose is to tell Slick how to compose and decompose `Speaker` objects from their corresponding table rows.  Here we say that a `Speaker` is composed of a `name`, a `bio`, and an optional `id`, and that Slick should use the `apply` and `unapply` methods from the `Speaker` case class to compose and decompose instances, respectively.  (FIXME: Footnote - In case you're not familiar with the `_` syntax, this is Scala's way of converting the methods from the `Speaker` class (`apply` and `unapply` respectively) to function objects. This is handy when a method or function expects a function object as input but all you have available is a method. If this isn't clear to you yet, don't worry about it.)

The next special method definition is `autoInc`. As we'll see shortly, it provides a mechanism by which we can do a database insert and retrieve the auto-incremented primary key value. The name `autoInc` isn't special.  It's the `returning` call which makes the magic happen.  We'll see how it's used in the next section.

Now that we've defined the Scala code representing our model, how do we create a database table to actually store the data?  The play-slick plugin make it easy!  It will automatically create evolutions files (FIXME: cref to evolutions recipe) for us based on our models as we change them.  We just have to tell Slick which objects we want it to pay attention to.  To do that we'll add the following line to our `application.conf`:
```
slick.default="models.*"
```
This tells the play-slick plugin to watch for changes to anything in the `models` package and to generate new DDL for us as appropriate.  Now if we were to start our application and access it via a Web browser, we would be confronted with a page telling us that we need to run our evolutions. A quick look in `conf/evolutions/default` shows that the plugin indeed created a SQL file for us!
```
# --- Created by Slick DDL
# To stop Slick DDL generation, remove this comment and start using Evolutions

# --- !Ups

create table `SPEAKERS` (`name` VARCHAR(254) NOT NULL,`bio` VARCHAR(254) NOT NULL,`id` BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY);

# --- !Downs

drop table `SPEAKERS`;
```
Now, by clicking the provided button on our Play! app's error screen, we can apply this evolution to our database. Simple!

## Inserting Database Records

Now that we have our structure defined in both our relational database and Scala, let's create some records to play with.

First, as usual, we'll start in the REPL.

Since our code depends on an implicit Play! Application object, we first instantiate that object:

```scala
scala> new play.core.StaticApplication(new java.io.File("."))
[info] play - database [default] connected at jdbc:mysql://localhost/scala_speakers?characterEncoding=UTF8
[info] play - Application started (Prod)
res0: play.core.StaticApplication = play.core.StaticApplication@bdaf1cd
```
Next we'll import that object into scope as well as some other items we need to access the database:
```scala
import play.api.Play.current
import play.api.db.slick.DB
import models._

```
Now we're ready to insert some data!
```
scala> DB.withSession { implicit session =>
     |   val nilanjan = Speaker("Nilanjan Raychaudhuri", "Developer")
     |   speakers = new Speakers
     |   speakers.autoInc.insert(nilanjan)
     | }
res5: Long = 1
```
We have used the `play.api.db.slick.DB` object's `withSession` method to implicitly provide a databavse connection to all of the code in the supplied block.  Inside of the code block, we first create an object to represent the `Speaker` record in our database. We then use our `Speakers` class to insert that object into the database.  Notice how we used `autoInc` so that when we call `insert` it returns the freshly inserted row's primary key value, which is returned as from the block.  Since this is our first record in the table, its value is 1.

Inserting another row provides the expected result:
```scala
scala> DB.withSession { implicit session =>
     |   val chad = Speaker("Chad Fowler", "CTO")
     |   speakers = new Speakers
     |   speakers.autoInc.insert(chad)
     | }
res6: Long = 2
```
Now we have developed some confidence in the REPL, let's build some helper methods which we will use over and over again.  First, inside our `Speakers`, we'll wrap the insert functionality in a method to avoid redundant code elsewhere:
```scala
  def insert(speaker: Speaker) = {
    DB.withSession { implicit session =>
      autoInc.insert(speaker)
    }
  }
```

As we did in the console, we also need to import the current Play! application into context, so at the top of the `Models.scala` file, we'll add this line:
```scala
import play.api.Play.current
```

Now if we were to restart our Play! console, we could insert a new record using the following:
```scala
scala> val dave = models.Speaker("Dave", "Publisher")
dave: models.Speaker = Speaker(dave,Publisher,None)
scala> (new models.Speakers).insert(dave)
res1: Long = 3
```

## Querying
We know how to get data into the database. Now let's build code that queries the database.  As a simple first query, we'll select all records from the `SPEAKERS` table. To do that, we'll add a convenience method to the `Speakers` class's companion object:
```scala
object Speakers
  def findAll: List[Speaker] = DB.withSession {implicit session =>
    Query(new Speakers).list
  }
}
```
We can now invoke this method like this:
```scala
scala> models.Speakers.findAll
res10: List[models.Speaker] = List(Speaker(Dave,Publisher,Some(3)), Speaker(Nilanjan Raychauduri,Developer,Some(1)), Speaker(chad,awesome,Some(2)))
```
## Displaying in a Web view

Now that we can create and query speakers from the database, let's try to implement a full but small application feature.  We are going to add "talks" to our sample database. The index page of our little application will show a full list of talks with title, abstract and speaker name. We should then be able to click the speaker's name to see a detail page for that speaker.

Since we haven't yet added our talks table we'll do that now.

The procedure is very similar to how we created the `Speakers` and `Speaker` classes.  We can also add these to `app/models/Models.scala`.
```scala
case class Talk(description: String, speakerId: Long, id: Option[Long] = None)

class Talks extends Table[Talk]("TALKS") {
  def id = column[Long]("id", O.PrimaryKey, O.AutoInc)
  def description = column[String]("description", O.NotNull)
  def speakerId = column[Long]("speakerId")

  def speaker = foreignKey("talks_speaker_fk", speakerId, speakers)(_.id)

  def * = description ~  speakerId ~ id.? <> (Talk.apply _, Talk.unapply _)

  def autoInc = * returning id

  def insert(t: Talk) = {
    DB.withSession { implicit session =>
      talks.autoInc.insert(t)
    }
  }


  def findTalks(s: Speaker): List[Talk] = {
    DB.withSession { implicit session =>

      val q = for(t <- talks if t.speakerId === s.id.get) yield t
      q.list()
    }

  }
}
```
Most of this is familiar from our implementation of `Speakers`, with two small exceptions. The first is the definition of the `speaker` method. The `foreignKey` method constructs a query method for us given the constraints of a foreign key relationship in our database. The function given at the end of the call specifies the target column of the foreign table to use in generating a query for the related record.  Concretely, this definition defines a foreign key relationship called "talks_speaker_fk" on the `TALKS` table's `speakerId` column referencing the `SPEAKERS` table (which we have captured the definition of in our `speakers` object) and defines a method for querying `Speaker` objects by using the `SPEAKERS` table's `id` field.

The second interesting addition is the `findTalks` method. This method shows how Slick allows you to use Scala `for` comprehensions to generate database queries.  Given an instance of `Speaker`, the `findTalks` method issues a SQL `SELECT` on the `TALKS` table for all rows whose `speakerId` is the same as the given `Speaker`. We then execute the query and extract those rows as a `List` of `Talk` instances.
