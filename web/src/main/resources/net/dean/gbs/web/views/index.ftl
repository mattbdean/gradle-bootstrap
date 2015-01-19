<!DOCTYPE html>
<html>
<head>
    <title>Gradle Bootstrap</title>
    <link rel="stylesheet" href="/css/style.css">
    <script src="/js/jquery-2.1.3.min.js"></script>
    <script src="/js/script.js"></script>
</head>
<body>
<div id="header">
    <h1>Gradle Bootstrap</h1>	
</div>
<div id="body">
    <div id="sections-wrapper">
        <section id="section-core">
            <h2>Core</h2>
            <div class="property">
                <label for="group">Package</label>
                <input type="text" id="group">
            </div>

            <div class="property">
                <label for="name">Name</label>
                <input type="text" id="name">
            </div>

            <div class="property">
                <label for="version">Version</label>
                <input type="text" id="version">
            </div>

            <div class="property">
                <label for="license">License</label>
                <select id="license"></select>
            </div>

            <div class="property">
                <label for="language">Languages</label>
                <select id="language" multiple></select>
            </div>
        </section>

        <h2>Frameworks</h2>
        <section id="section-frameworks">
            <div class="property">
                <label for="testing">Testing</label>
                <select id="testing"></select>
            </div>

            <div class="property">
                <label for="logging">Logging</label>
                <select id="logging"></select>
            </div>
        </section>
    </div>
    <a id="submit" href="javascript:void(0)">Submit</a>
</div>
</body>
</html>
