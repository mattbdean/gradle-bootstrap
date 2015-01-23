module.exports = function(grunt) {
    var config = {
        pkg: grunt.file.readJSON('package.json'),
        uglify: {
            options: {
                banner: '/*! <%= pkg.name %> <%= grunt.template.today("yyyy-mm-dd") %> */\n'
            },
            build: {
                src: 'src/js/*.js',
                dest: 'build/js/script.min.js'
            }
        },
        cssmin: {
            files: {
                expand: true,
                cwd: 'src/css',
                src: ['*.css'],
                dest: 'build/css',
                ext: '.min.css'
            }
        },
        processhtml: {
            options: {},
            dist: {
                files: {
                    'build/index.html': ['src/index.html']
                }
            }
        },
        copy: {
            main: {
                files: [
                    {expand: true, cwd: 'lib', src: ['css/*'], dest: 'build/'},
                    {expand: true, cwd: 'lib', src: ['js/*'], dest: 'build/'},
                    {expand: true, cwd: 'src/other', src: ['./*'], dest: 'build/', filter: 'isFile'}
                ]
            }
        },
        clean: ['build']
    };

    var tasks = ['clean', 'cssmin', 'processhtml', 'copy'];

    // --js-debug=true will stop JS from being minified
    var debug = grunt.option('js-debug');
    console.log("JavaScript minification: " + (debug ? "DISABLED" : "ENABLED"));
    if (!debug) {
        // Uglify it not debugging
        tasks.push('uglify');
    } else {
        // Debugging the script, don't minify, just "cp src/script.js build/script.min.js"
        config.copy.main.files.push({
            expand: true,
            cwd: 'src',
            src: ['js/script.js'],
            dest: 'build/',
            rename: function(dest, src) {
                return dest + src.replace('.js', '.min.js');
            }
        })
    }

    grunt.initConfig(config);

    grunt.loadNpmTasks('grunt-contrib-uglify');
    grunt.loadNpmTasks('grunt-contrib-cssmin');
    grunt.loadNpmTasks('grunt-processhtml');
    grunt.loadNpmTasks('grunt-contrib-copy');
    grunt.loadNpmTasks('grunt-contrib-clean');

    // For CLI convenience
    grunt.registerTask('default', tasks);
    // For the Gradle task
    grunt.registerTask('build', tasks);
};
