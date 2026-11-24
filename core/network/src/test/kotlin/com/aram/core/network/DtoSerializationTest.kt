package com.aram.core.network

import com.aram.core.network.dto.AuthResponseDto
import com.aram.core.network.dto.ResultEnvelope
import com.aram.core.network.dto.SaveTransactionRequest
import com.aram.core.network.dto.SyncPullDto
import com.aram.core.network.dto.SyncPushRequest
import com.aram.core.network.dto.SyncPushResultDto
import com.aram.core.network.dto.SyncTransactionItem
import com.aram.core.network.dto.TransactionDto
import com.aram.core.network.dto.TransactionListDto
import com.aram.core.network.dto.ValidationErrorEnvelope
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DtoSerializationTest {

    @Test
    fun `result envelope parses camelCase and ignores unknown fields`() {
        val envelope = AramJson.decodeFromString<ResultEnvelope<AuthResponseDto>>(
            """
            {
              "isSuccess": true,
              "message": "",
              "errors": [],
              "data": {
                "token": "jwt-token",
                "expiresAtUtc": "2026-11-22T10:15:30Z",
                "profile": {
                  "id": "u1",
                  "displayName": "Ava Bennett",
                  "firstName": "Ava",
                  "email": "demo@aram.app",
                  "avatarInitials": "AB",
                  "homeCurrency": "IRR",
                  "unexpectedField": true
                }
              }
            }
            """.trimIndent(),
        )
        assertTrue(envelope.isSuccess)
        val data = requireNotNull(envelope.data)
        assertEquals("jwt-token", data.token)
        assertEquals("2026-11-22T10:15:30Z", data.expiresAtUtc)
        assertEquals("Ava Bennett", data.profile.displayName)
        assertEquals("system", data.profile.appearance)
    }

    @Test
    fun `validation envelope keeps per-field messages`() {
        val envelope = AramJson.decodeFromString<ValidationErrorEnvelope>(
            """
            {
              "statusCode": 400,
              "message": "Validation failed.",
              "errors": { "email": ["Email is required.", "Email is invalid."] }
            }
            """.trimIndent(),
        )
        assertEquals(400, envelope.statusCode)
        assertEquals("Email is required.", envelope.errors?.get("email")?.first())
    }

    @Test
    fun `transaction parses irr amounts and entered currency`() {
        val tx = AramJson.decodeFromString<TransactionDto>(
            """
            {
              "id": "t1",
              "kind": "expense",
              "amountIrr": 10420000.0,
              "category": {
                "id": "c1", "slug": "groceries", "name": "Groceries",
                "iconKey": "cart", "tone": "positive", "kind": "expense"
              },
              "account": { "id": "a1", "name": "Everyday", "institution": "Bank", "kind": "bank" },
              "note": "Weekly shop",
              "occurredAt": "2026-11-20T08:05:00Z",
              "enteredCurrency": "USD",
              "enteredAmount": 10.0,
              "fxRate": 1042000.0
            }
            """.trimIndent(),
        )
        assertEquals("groceries", tx.category.slug)
        assertEquals(10_420_000.0, tx.amountIrr, 0.001)
        assertEquals(10.0, requireNotNull(tx.enteredAmount), 0.001)
        assertEquals("Everyday", tx.account?.name)
        assertEquals("2026-11-20T08:05:00Z", tx.occurredAt)
    }

    @Test
    fun `transaction list parses day groups and paging`() {
        val list = AramJson.decodeFromString<TransactionListDto>(
            """
            {
              "year": 2026, "month": 11, "page": 1, "pageSize": 50,
              "totalCount": 1, "totalPages": 1,
              "totalSpentIrr": 10420000.0, "totalIncomeIrr": 0.0, "spentDeltaPercent": -4.5,
              "groups": [{
                "date": "2026-11-20", "label": "Yesterday", "totalIrr": 10420000.0,
                "items": [{
                  "id": "t1", "kind": "expense", "amountIrr": 10420000.0,
                  "category": {
                    "id": "c1", "slug": "groceries", "name": "Groceries",
                    "iconKey": "cart", "tone": "positive", "kind": "expense"
                  },
                  "occurredAt": "2026-11-20T08:05:00Z"
                }]
              }]
            }
            """.trimIndent(),
        )
        assertEquals(1, list.groups.single().items.size)
        assertEquals("Yesterday", list.groups.single().label)
        assertEquals("IRR", list.groups.single().items.single().enteredCurrency)
        assertNull(list.groups.single().items.single().account)
    }

    @Test
    fun `sync pull parses transactions budgets goals and settings`() {
        val pull = AramJson.decodeFromString<SyncPullDto>(
            """
            {
              "serverTime": "2026-11-21T12:00:00Z",
              "lastSyncedAt": "2026-11-21T12:00:00Z",
              "transactions": [{
                "id": "t1", "kind": "income", "amountIrr": 45000000.0,
                "category": {
                  "id": "c2", "slug": "salary", "name": "Salary",
                  "iconKey": "wallet", "tone": "positive", "kind": "income"
                },
                "occurredAt": "2026-11-01T09:00:00Z"
              }],
              "budgets": [{
                "id": "b1", "categoryId": "c1", "categorySlug": "groceries",
                "categoryName": "Groceries", "iconKey": "cart", "monthlyLimitIrr": 12000000.0
              }],
              "goals": [{
                "id": "g1", "name": "Emergency fund", "targetIrr": 200000000.0,
                "savedIrr": 75000000.0, "remainingIrr": 125000000.0, "percent": 37.5
              }],
              "settings": {
                "displayName": "Ava Bennett", "email": "demo@aram.app", "avatarInitials": "AB",
                "homeCurrency": "IRR", "ratesUpdatedAt": "2026-11-21T00:00:00Z"
              }
            }
            """.trimIndent(),
        )
        assertEquals(1, pull.transactions.size)
        assertEquals(12_000_000.0, pull.budgets.single().monthlyLimitIrr, 0.001)
        assertEquals(37.5, pull.goals.single().percent, 0.001)
        assertEquals("IRR", pull.settings?.homeCurrency)
    }

    @Test
    fun `sync push parses applied and skipped ids`() {
        val push = AramJson.decodeFromString<SyncPushResultDto>(
            """
            {
              "applied": ["t1"], "skipped": ["t2"],
              "serverTime": "2026-11-21T12:00:00Z",
              "lastSyncedAt": "2026-11-21T12:00:00Z"
            }
            """.trimIndent(),
        )
        assertEquals(listOf("t1"), push.applied)
        assertEquals(listOf("t2"), push.skipped)
    }

    @Test
    fun `requests encode camelCase and omit nulls`() {
        val json = AramJson.encodeToString(
            SaveTransactionRequest(kind = "expense", amount = 250_000.0, note = "Lunch"),
        )
        assertTrue(json.contains("\"kind\":\"expense\""))
        assertTrue(json.contains("\"amount\":250000.0"))
        assertTrue(!json.contains("currency"))
        assertTrue(!json.contains("categorySlug"))
    }

    @Test
    fun `sync items omit defaults on the wire`() {
        val json = AramJson.encodeToString(SyncPushRequest(items = listOf(SyncTransactionItem(id = "t1"))))
        assertTrue(json.contains("\"id\":\"t1\""))
        assertTrue(!json.contains("\"kind\""))
        assertTrue(!json.contains("\"categorySlug\""))
    }
}
