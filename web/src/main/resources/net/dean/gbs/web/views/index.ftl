<!DOCTYPE html>
<html>
<head>
    <title>Gradle Bootstrap</title>
    <link rel="stylesheet" href="css/style.css">
    <link rel="stylesheet" href="css/chosen.min.css"/>
    <link href="http://fonts.googleapis.com/css?family=Lato|Slabo+27px|Ubuntu+Mono" rel="stylesheet" type="text/css">
    <script src="js/jquery-2.1.3.min.js"></script>
    <script src="js/chosen.jquery.min.js"></script>
    <script src="js/script.js"></script>
</head>
<body>
<div id="content-wrapper">
    <header>
        <h1 id="title">Gradle Bootstrap</h1>
        <h3 id="slogan">Gradle projects in 30 seconds</h3>
    </header>
    <div id="body">
        <a href="https://github.com/thatJavaNerd/gradle-bootstrap">
            <img style="position: absolute; top: 0; right: 0; border: 0;"
                 src="https://camo.githubusercontent.com/e7bbb0521b397edbd5fe43e7f760759336b5e05f/68747470733a2f2f73332e616d617a6f6e6177732e636f6d2f6769746875622f726962626f6e732f666f726b6d655f72696768745f677265656e5f3030373230302e706e67"
                 alt="Fork me on GitHub"
                 data-canonical-src="https://s3.amazonaws.com/github/ribbons/forkme_right_green_007200.png">
        </a>
        <div id="sections-wrapper">
            <h2>The Basics</h2>
            <section id="section-core">
                <div class="property" data-name="group">
                    <label for="group">Package</label>
                    <input class="code" type="text" id="group">
                </div>

                <div class="property" data-name="name">
                    <label for="name">Name</label>
                    <input class="code" type="text" id="name">
                </div>

                <div class="property" data-name="version">
                    <label for="version">Version</label>
                    <input class="code" type="text" id="version">
                </div>

                <div class="property" data-name="license">
                    <label for="license">License</label>
                    <select id="license"></select>
                </div>

                <div class="property" data-name="language">
                    <label for="language">Languages</label>
                    <select id="language" multiple></select>
                </div>
            </section>

            <h2>Frameworks</h2>
            <section id="section-frameworks">
                <div class="property" data-name="testing">
                    <label for="testing">Testing</label>
                    <select id="testing"></select>
                </div>

                <div class="property" data-name="logging">
                    <label for="logging">Logging</label>
                    <select id="logging"></select>
                </div>
            </section>
        </div>
        <a id="submit" class="btn" href="javascript:void(0)">Create</a>
    </div>
</div>
<footer>
    <p>Copyright &copy; Matthew Dean 2015</p>
    <p>Source code is available at <a href="https://github.com/thatJavaNerd/gradle-bootstrap">thatJavaNerd/gradle-bootstrap</a></p>
</footer>
</body>
</html>
