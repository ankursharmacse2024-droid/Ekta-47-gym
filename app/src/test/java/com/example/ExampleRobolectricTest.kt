package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("EKTA 47 GYM", appName)
    val dbId = context.getString(R.string.firestore_database_id)
    assert(dbId.isNotEmpty())
  }

  @Test
  fun `staff registration store saves and consumes pending registration`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val registration = com.example.auth.PendingStaffRegistration(
      staffName = "Rohan Sharma",
      role = com.example.model.StaffAccessRole.STAFF,
      facilityName = "EKTA 47 GYM & FITNESS CENTRE",
      phone = "+91 98765 43210"
    )
    com.example.auth.StaffRegistrationStore.savePendingRegistration(context, registration)
    val consumed = com.example.auth.StaffRegistrationStore.consumePendingRegistration(context)
    org.junit.Assert.assertNotNull(consumed)
    assertEquals("Rohan Sharma", consumed?.staffName)
    assertEquals(com.example.model.StaffAccessRole.STAFF, consumed?.role)
    assertEquals("EKTA 47 GYM & FITNESS CENTRE", consumed?.facilityName)
    assertEquals("+91 98765 43210", consumed?.phone)

    // Verify it was consumed (one-time handshake)
    val consumedAgain = com.example.auth.StaffRegistrationStore.consumePendingRegistration(context)
    org.junit.Assert.assertNull(consumedAgain)
  }
}
