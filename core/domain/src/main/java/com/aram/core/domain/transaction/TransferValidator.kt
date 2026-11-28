package com.aram.core.domain.transaction

import com.aram.core.model.Account

/**
 * Pure transfer rules. Callers resolve both accounts first; a null lookup means
 * the id does not exist in the current ledger. Cross-currency transfers are
 * rejected rather than converted — the MVP has no FX rate.
 */
object TransferValidator {

    fun validate(
        draft: TransferDraft,
        source: Account?,
        destination: Account?,
    ): TransferValidation {
        val problems = mutableMapOf<TransactionField, TransferProblem>()

        if (draft.amountMinor <= 0L) {
            problems[TransactionField.AMOUNT] = TransferProblem.AmountNotPositive
        }
        if (draft.fromAccountId == draft.toAccountId) {
            problems[TransactionField.TO_ACCOUNT] = TransferProblem.SameAccount
        }

        when {
            source == null ->
                problems[TransactionField.ACCOUNT] = TransferProblem.AccountNotFound
            source.archived ->
                problems[TransactionField.ACCOUNT] = TransferProblem.AccountArchived
        }
        when {
            destination == null ->
                problems[TransactionField.TO_ACCOUNT] = TransferProblem.AccountNotFound
            destination.archived ->
                problems[TransactionField.TO_ACCOUNT] = TransferProblem.AccountArchived
        }

        if (source != null && destination != null) {
            if (source.currencyCode != destination.currencyCode) {
                problems[TransactionField.CURRENCY] = TransferProblem.CurrencyMismatch(
                    sourceCurrency = source.currencyCode,
                    destinationCurrency = destination.currencyCode,
                )
            } else if (draft.currencyCode != source.currencyCode) {
                problems[TransactionField.CURRENCY] = TransferProblem.CurrencyMismatch(
                    sourceCurrency = source.currencyCode,
                    destinationCurrency = draft.currencyCode,
                )
            }
        }

        return if (problems.isEmpty()) {
            TransferValidation.Valid
        } else {
            TransferValidation.Invalid(problems)
        }
    }
}