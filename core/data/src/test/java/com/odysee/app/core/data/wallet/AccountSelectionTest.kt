package com.odysee.app.core.data.wallet

import com.odysee.app.core.network.dto.AccountDto
import kotlinx.serialization.json.JsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class AccountSelectionTest {

    @Test
    fun empty_returnsNull() {
        assertNull(pickDefaultAccount(emptyList()))
    }

    @Test
    fun prefersFundedAccount() {
        val accounts = listOf(
            AccountDto(id = "empty-default", satoshis = JsonPrimitive(0), isDefault = true),
            AccountDto(id = "funded", satoshis = JsonPrimitive(1000), isDefault = false),
        )
        assertEquals("funded", pickDefaultAccount(accounts)?.id)
    }

    @Test
    fun prefersNonDefaultWhenAllUnfunded() {
        val accounts = listOf(
            AccountDto(id = "anon-default", satoshis = JsonPrimitive(0), isDefault = true),
            AccountDto(id = "synced", satoshis = JsonPrimitive(0), isDefault = false),
        )
        assertEquals("synced", pickDefaultAccount(accounts)?.id)
    }

    @Test
    fun singleAccount() {
        val accounts = listOf(
            AccountDto(id = "only", satoshis = JsonPrimitive(0), isDefault = true),
        )
        assertEquals("only", pickDefaultAccount(accounts)?.id)
    }
}
