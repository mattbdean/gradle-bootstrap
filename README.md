Gradle Bootstrap [![Travis-CI](http://img.shields.io/travis/thatJavaNerd/gradle-bootstrap.svg?style=flat)](https://travis-ci.org/thatJavaNerd/gradle-bootstrap)
================

Gradle Bootstrap is a simple Kotlin webapp that lets you create Gradle project skeletons in 30 seconds. Choose from several languages, license, testing, and logging frameworks, and Git integration.

## Project Structure

`:api` - Underlying API the website uses to create the Gradle projects. Nothing network-y, just plain code.

`:website` - Website managed by Grunt: HTML, CSS, JavaScript, the works. Uses the `:service` API.

`:service` - [Dropwizard](http://www.dropwizard.io/) app that uses the previous two modules to create a fully functioning service.

## Running
`gradle :website:build` builds the website. Set `JS_DEBUG=true` in the shell to disable minifying JavaScript for debugging purposes. On Linux: `JS_DEBUG=true gradle website:build`

`gradle :service:stage` packages the entire project into a runnable Jar at `service/build/libs/service-<version>-fat.jar`

`gradlew :service:run` builds and runs the website. See [`http://localhost:2001`](http://localhost:2001) once it's up and running.

## API Reference

All API methods produce JSON unless otherwise specified.

#### `POST /api/v1/project`

**Consumes**: `application/x-www-form-urlencoded`

**Form Parameters**:

1. `name`: Project name (ex: "JRAW")
2. `group`: Group/package: (ex: "net.dean.jraw")
3. `language`: A comma-separated list of languages
4. `version` (Optional) Self explanatory
5. `testing`: (Optional) Testing framework.
6. `logging`: (Optional) Logging framework.
7. `license`: (Optional) Self explanatory
8. `git_init`: (Optional) If true, will initialize a .git directory in the base of the project
9. `git_url`: (Optional) If true, will initialize a .git directory and set `origin` to the given URL

For acceptable parameters for `logging`, `testing`, `language`, and `license`, see `/api/v1/project/options`

#### [`GET  /api/v1/project/options`](https://gradle-bootstrap.herokuapp.com/api/v1/project/options)

Gets list of available options for `logging`, `testing`, `language`, and `license` in `/api/v1/project`. Also shows the default value for each.

#### `GET  /api/v1/project/{id}`

Retrieves information about a project with a given ID

#### `GET  /api/v1/project/{id}/download`

**Produces**: `application/zip`

Downloads a project

