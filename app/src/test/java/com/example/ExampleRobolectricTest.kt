package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.util.FormatUtils
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    @Test
    fun `app name matches Internet Vault`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("Internet Vault", appName)
    }

    @Test
    fun `format utils formats duration and speed accurately`() {
        assertEquals("01:01:05", FormatUtils.formatDuration(3665))
        assertEquals("0 KB/s", FormatUtils.formatSpeed(0L))
    }
}
