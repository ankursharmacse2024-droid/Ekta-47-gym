package com.example.auth

import android.content.Context
import com.example.model.StaffAccessRole

data class PendingStaffRegistration(
    val staffName: String,
    val role: StaffAccessRole,
    val facilityName: String,
    val phone: String
)

object StaffRegistrationStore {
    private const val PREFS_NAME = "ekta_staff_auth_prefs"
    private const val KEY_PENDING_NAME = "pending_staff_name"
    private const val KEY_PENDING_ROLE = "pending_staff_role"
    private const val KEY_PENDING_FACILITY = "pending_facility_name"
    private const val KEY_PENDING_PHONE = "pending_staff_phone"
    private const val KEY_SELECTED_LOGIN_ROLE = "selected_login_role"

    fun savePendingRegistration(context: Context, registration: PendingStaffRegistration) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_PENDING_NAME, registration.staffName)
            .putString(KEY_PENDING_ROLE, registration.role.name)
            .putString(KEY_PENDING_FACILITY, registration.facilityName)
            .putString(KEY_PENDING_PHONE, registration.phone)
            .apply()
    }

    fun consumePendingRegistration(context: Context): PendingStaffRegistration? {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val name = prefs.getString(KEY_PENDING_NAME, null) ?: return null
        val roleStr = prefs.getString(KEY_PENDING_ROLE, StaffAccessRole.ADMIN.name)
        val role = runCatching { StaffAccessRole.valueOf(roleStr!!) }.getOrDefault(StaffAccessRole.ADMIN)
        val facility = prefs.getString(KEY_PENDING_FACILITY, "EKTA 47 GYM & FITNESS CENTRE") ?: "EKTA 47 GYM & FITNESS CENTRE"
        val phone = prefs.getString(KEY_PENDING_PHONE, "") ?: ""

        prefs.edit()
            .remove(KEY_PENDING_NAME)
            .remove(KEY_PENDING_ROLE)
            .remove(KEY_PENDING_FACILITY)
            .remove(KEY_PENDING_PHONE)
            .apply()

        return PendingStaffRegistration(
            staffName = name,
            role = role,
            facilityName = facility,
            phone = phone
        )
    }

    fun saveSelectedLoginRole(context: Context, role: StaffAccessRole) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_SELECTED_LOGIN_ROLE, role.name)
            .apply()
    }

    fun getSelectedLoginRole(context: Context): StaffAccessRole {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val roleStr = prefs.getString(KEY_SELECTED_LOGIN_ROLE, StaffAccessRole.ADMIN.name)
        return runCatching { StaffAccessRole.valueOf(roleStr!!) }.getOrDefault(StaffAccessRole.ADMIN)
    }
}
