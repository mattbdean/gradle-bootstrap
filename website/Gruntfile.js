module.exports = function(grunt) {
    grunt.initConfig({
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
                    'build/index.ftl': ['src/index.ftl']
                }
            }
        },
        copy: {
            main: {
                files: [
                    {expand: true, cwd: 'lib', src: ['css/*'], dest: 'build/'},
                    {expand: true, cwd: 'lib', src: ['js/*'], dest: 'build/'}
                ]
            }
        }
    });

    grunt.loadNpmTasks('grunt-contrib-uglify');
    grunt.loadNpmTasks('grunt-contrib-cssmin');
    grunt.loadNpmTasks('grunt-processhtml');
    grunt.loadNpmTasks('grunt-contrib-copy');

    var tasks = ['uglify', 'cssmin', 'processhtml', 'copy'];
    grunt.registerTask('default', tasks);
    grunt.registerTask('build', tasks);
};
