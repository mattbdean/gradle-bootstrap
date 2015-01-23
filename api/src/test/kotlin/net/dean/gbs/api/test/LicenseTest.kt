package net.dean.gbs.api.test

import org.junit.Test as test
import net.dean.gbs.api.models.License
import kotlin.test.assertTrue
import java.nio.file.Files
import net.dean.gbs.api.io.licensePath

public class LicenseTest {
    public test fun testAllLicensesExist() {
        License.values().forEach {
            val licensePath = licensePath(it)
            assertTrue(licensePath.isFile(), "${licensePath.getAbsolutePath()} was not a file")
        }
    }
}
