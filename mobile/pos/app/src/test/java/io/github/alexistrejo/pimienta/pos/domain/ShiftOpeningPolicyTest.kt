package io.github.alexistrejo.pimienta.pos.domain

import io.github.alexistrejo.pimienta.pos.allowedShiftAssignees
import io.github.alexistrejo.pimienta.pos.data.local.entity.LocalUserEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

// Unit tests for shift opening permission rules and assignee filtering.
class ShiftOpeningPolicyTest {

    private val mgr1 = LocalUserEntity(id = "mgr-1", displayName = "Manager 1", role = "MANAGER", pinHash = "hash", active = true)
    private val mgr2 = LocalUserEntity(id = "mgr-2", displayName = "Manager 2", role = "MANAGER", pinHash = "hash", active = true)
    private val superAdmin1 = LocalUserEntity(id = "super-1", displayName = "Super Admin 1", role = "SUPERADMIN", pinHash = "hash", active = true)
    private val superAdmin2 = LocalUserEntity(id = "super-2", displayName = "Super Admin 2", role = "SUPERADMIN", pinHash = "hash", active = true)
    private val cashier1 = LocalUserEntity(id = "cashier-1", displayName = "Cashier 1", role = "CASHIER", pinHash = "hash", active = true)
    private val cashier2 = LocalUserEntity(id = "cashier-2", displayName = "Cashier 2", role = "CASHIER", pinHash = "hash", active = true)
    private val inactiveCashier = LocalUserEntity(id = "cashier-3", displayName = "Inactive Cashier", role = "CASHIER", pinHash = "hash", active = false)

    private val allUsers = listOf(mgr1, mgr2, superAdmin1, superAdmin2, cashier1, cashier2, inactiveCashier)

    @Test
    fun managerCanOnlyAssignShiftToSelfAndCashiers() {
        val allowed = allowedShiftAssignees(authorizer = mgr1, users = allUsers)
        val allowedIds = allowed.map { it.id }

        // Manager 1 can select himself and active cashiers
        assertTrue(allowedIds.contains("mgr-1"))
        assertTrue(allowedIds.contains("cashier-1"))
        assertTrue(allowedIds.contains("cashier-2"))

        // Manager 1 cannot select other managers or superadmins or inactive users
        assertFalse(allowedIds.contains("mgr-2"))
        assertFalse(allowedIds.contains("super-1"))
        assertFalse(allowedIds.contains("super-2"))
        assertFalse(allowedIds.contains("cashier-3"))
    }

    @Test
    fun superAdminCanAssignShiftToAnyoneActive() {
        val allowed = allowedShiftAssignees(authorizer = superAdmin1, users = allUsers)
        val allowedIds = allowed.map { it.id }

        // Superadmin can select all active users including other superadmins, managers, and cashiers
        assertTrue(allowedIds.contains("super-1"))
        assertTrue(allowedIds.contains("super-2"))
        assertTrue(allowedIds.contains("mgr-1"))
        assertTrue(allowedIds.contains("mgr-2"))
        assertTrue(allowedIds.contains("cashier-1"))
        assertTrue(allowedIds.contains("cashier-2"))

        // Inactive user must still be excluded
        assertFalse(allowedIds.contains("cashier-3"))
    }

    @Test
    fun roleMatchingIsCaseInsensitive() {
        val mgrLower = LocalUserEntity(id = "mgr-lower", displayName = "Manager Lower", role = "manager", pinHash = "hash", active = true)
        val cashierLower = LocalUserEntity(id = "cashier-lower", displayName = "Cashier Lower", role = "cashier", pinHash = "hash", active = true)
        val users = listOf(mgrLower, cashierLower)

        val allowed = allowedShiftAssignees(authorizer = mgrLower, users = users)
        assertEquals(2, allowed.size)
    }
}
