package net.dean.gbs.api.io

import java.nio.file.Path
import java.io.Closeable
import java.nio.charset.StandardCharsets
import java.util.HashMap
import java.io.Writer
import java.util.ArrayList
import java.nio.file.Files
import kotlin.platform.platformStatic

/**
 * This class encapsulates an OutputStream to assist with the generation of generic, bracket based code. The Path
 * provided in the constructor is the base path for which to create other files. If this path was
 * "/home/thatJavaNerd/project" and we called `generator.init("src/MyClass.java")`, we would be writing to a file named
 * "/home/thatJavaNerd/project/src/MyClass.java". When we are done writing to the file, call `generator.close()`. This
 * will check if all blocks have been closed and finally close the OutputStream.
 *
 * This class is not tuned to any specific language, and is only limited to bracket-based languages such as Java and
 * Groovy (sorry Python). Grammar must be provided by the user and therefore there is is no guarantee that code produced
 * by this class will be syntactically valid.
 */
public class CodeGenerator(private val root: Path) : Closeable {
    class object {
        private val newLine = System.lineSeparator()
    }
    private val charset = StandardCharsets.UTF_8
    private var path: Path? = null
        set(value) {
            if (value != null)
                _history.add(value)
            $path = value
        }
    private var writer: Writer? = null
    private var writing: Boolean = false
    private var lastLineType: LineType? = null
    private val indent: Indent = Indent.spaces(4)

    // Only allow private write access
    public val history: List<Path>
        get() = _history
    private val _history: MutableList<Path> = ArrayList()

    public fun init(fileName: String) {
        checkWriting(false)
        this.path = relativePath(root, fileName)
        this.writer = Files.newBufferedWriter(path, charset)
        this.writing = true
    }

    /**
     * Asserts that there is or is not a file currently open for writing. If [expected] is true and we do not have a
     * file open, then an IllegalStateException is thrown. If [expected] is false and we have a file open, then an
     * IllegalStateException is thrown.
     */
    private fun checkWriting(expected: Boolean) {
        if (this.writing && !expected)
            throw IllegalStateException("File already open (${this.path})")
        if (!this.writing && expected)
            throw IllegalStateException("No file open")
    }

    /**
     * Opens a code block with the given name and increases the indentation by one. For example, `openBlock("foo")` will
     * result in `foo {`
     */
    public fun openBlock(blockName: String) {
        write("$blockName {", LineType.OPEN_BLOCK)
        indent.inc()
    }

    /**
     * Closes a code block. The only character on this line will be a closing curly bracket: '}'. Decreases the indent
     */
    public fun closeBlock() {
        indent.dec()
        write("}", LineType.END_BLOCK)
    }

    /**
     * Writes a line of code to the writer. Automatically places the indentation before and a new line after.
     */
    public fun statement(code: String) {
        write(code, LineType.STATEMENT)
    }

    /**
     * Writes the given raw text. [lineType] indicates what type of line this is.
     */
    private fun write(text: String, lineType: LineType) {
        checkWriting(true)

        // Add empty lines around the generated code to make it look like something a human would write
        // Add an empty line between two blocks
        if (lastLineType == LineType.END_BLOCK && lineType != LineType.END_BLOCK)
            newLine()
        // Separate statements from blocks with a new line
        if (lastLineType == LineType.STATEMENT && lineType == LineType.OPEN_BLOCK)
            newLine()

        writer!!.append("${indent.value()}$text$newLine")
        lastLineType = lineType
    }

    public fun newLine() {
        checkWriting(true)
        writer!!.append(newLine)
    }

    override fun close() {
        // Finish off the file like good programmers, with an extra line at the end
        newLine()
        // We don't need to assert that we have a file open because newLine() does that for us

        if (indent.current != 0)
            throw IllegalStateException("Did not close all blocks (${indent.current} remain open)")
        indent.reset()
        path = null
        writer!!.close()
        writing = false
        lastLineType = null
    }
}

/**
 * Basic types of statements in code
 */
private enum class LineType {
    /** Basic line. Declaration, method call, etc. */
    STATEMENT
    /** Indicates the start of a new code block */
    OPEN_BLOCK
    /** Indicates the end of a code block */
    END_BLOCK
}

/**
 * Represents the indentation of a file, in either tabs or spaces.
 */
public data class Indent private(public val spaces: Boolean, public val spaceCount: Int) {
    class object {
        public platformStatic fun spaces(count: Int): Indent = Indent(true, count)
        public platformStatic fun tabs(): Indent = Indent(false, -1)
    }

    private val indents: MutableMap<Int, String> = HashMap()
    private val baseIndent: String

    /**
     * Current indentation. Can be changed using inc() or dec()
     */
    public var current: Int = 0
        private set(value) {
            if (value < 0) {
                throw IllegalArgumentException("Cannot have a negative indent")
            }
            if (!indents.containsKey(value)) {
                val sb = StringBuilder()
                var temp = 0
                while (temp < value) {
                    sb.append(baseIndent)
                    temp++
                }

                indents.put(value, sb.toString())
            }
            $current = value
        }

    /** Increases the indent by one */
    public fun inc() {
        current++
    }

    /** Decreases the indent by one */
    public fun dec() {
        current--
    }

    /** Sets the indentation to 0 */
    public fun reset() {
        current = 0
    }

    /** Gets the current indentation as a String */
    public fun value(): String {
        return indents.get(current)!!
    }

    {
        // Re-initialize currentIndent because the first initialization writes to the backing field directly and does
        // not go through our custom setter, which we want
        current = 0

        // Figure out our base indent
        baseIndent = if (spaces) {
            StringBuilder {
                var temp = 0
                while (temp < spaceCount) {
                    append(' ')
                    temp++
                }
            }.toString()
        } else {
            "\t"
        }
    }
}

