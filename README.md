gradle-bootstrap [![Travis-CI](http://img.shields.io/travis/thatJavaNerd/gradle-bootstrap.svg?style=flat)](https://travis-ci.org/thatJavaNerd/gradle-bootstrap)
================

Gradle Bootstrap is a simple Kotlin webapp that lets you create Gradle project skeletons in 30 seconds. Choose from several languages, license, testing, and logging frameworks, and Git integration.

## Project Structure

`:api` - Underlying API the website uses to create the Gradle projects. Nothing network-y, just plain code.

`:website` - Website managed by Grunt: HTML, CSS, JavaScript, the works.

`:service` - [Dropwizard](http://www.dropwizard.io/) app that uses the previous two modules to create a fully functioning service.

## Running
`gradle :website:build` builds the website. Set variable `JS_DEBUG=true` in the shell to disable minifying JavaScript for debugging purposes. On Linux: `JS_DEBUG=true gradle website:build`

`gradle :service:stage` packages the entire project into a runnable Jar at `service/libs/service-<version>-fat.jar`

`gradlew :service:run` builds and runs the website. See [`http://localhost:2001`](http://localhost:2001) once it's up and running.

## Branches

`master`: Stable branch. Pushed to Heroku when updated.

`develop`: Unstable branch. All work should be pushed to this branch before merging into master.
