package net.dean.gbs.api.test

import net.dean.gbs.api.io.licensePath
import net.dean.gbs.api.models.License
import org.junit.Assert.assertTrue
import kotlin.collections.forEach
import org.junit.Test as test

public class LicenseTest {
    public @test fun testAllLicensesExist() {
        License.values().forEach {
            val licensePath = licensePath(it)
            assertTrue("${licensePath.absolutePath} was not a file", licensePath.isFile)
        }
    }
}
