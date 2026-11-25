package com.odysee.app.core.data.wallet

import com.odysee.app.core.network.dto.AccountDto

/**
 * Choose which SDK account should be default after wallet sync.
 * Prefer funded; else a non-default account (usually the one sync_apply added);
 * else index 1; else the sole account.
 */
internal fun pickDefaultAccount(accounts: List<AccountDto>): AccountDto? {
    if (accounts.isEmpty()) return null
    return accounts.firstOrNull { it.satoshiBalance() > 0L }
        ?: accounts.firstOrNull { it.isDefault != true }
        ?: accounts.getOrNull(1)
        ?: accounts.first()
}
