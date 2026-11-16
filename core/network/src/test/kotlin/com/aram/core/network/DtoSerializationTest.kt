package com.aram.core.network

import com.aram.core.network.dto.AccountDto
import com.aram.core.network.dto.AuthResponseDto
import com.aram.core.network.dto.BudgetDto
import com.aram.core.network.dto.CategoryDto
import com.aram.core.network.dto.DataEnvelope
import com.aram.core.network.dto.ErrorEnvelope
import com.aram.core.network.dto.GoalContributionDto
import com.aram.core.network.dto.GoalDto
import com.aram.core.network.dto.PatchMeRequest
import com.aram.core.network.dto.PushEnvelope
import com.aram.core.network.dto.SyncEnvelope
import com.aram.core.network.dto.TransactionDto
import com.aram.core.network.dto.UserDto
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DtoSerializationTest {

    @Test
    fun `user fixture parses snake case fields`() {
        val envelope = AramJson.decodeFromString<DataEnvelope<UserDto>>(
            """
            {
              "data": {
                "id": "8f14e45f-ea0f-4b0c-9d9a-1c6f4c9a8b11",
                "email": "demo@aram.app",
                "display_name": "Demo",
                "default_currency": "USD",
                "calendar_system": "GREGORIAN",
                "created_at": 1763500000000,
                "updated_at": 1763500000001,
                "unexpected_field": true
              },
              "meta": { "server_time": 1763500000123 }
            }
            """.trimIndent(),
        )
        assertEquals("Demo", envelope.data.displayName)
        assertEquals("USD", envelope.data.defaultCurrency)
        assertEquals(1763500000123, envelope.meta?.serverTime)
        assertNull(envelope.meta?.nextCursor)
    }

    @Test
    fun `account fixture parses and keeps tombstones`() {
        val account = AramJson.decodeFromString<AccountDto>(
            """
            {
              "id": "acc-1", "name": "Cash", "type": "CASH",
              "currency_code": "USD", "opening_balance_minor": 125000,
              "color_argb": -16711936, "icon_key": "wallet", "sort_order": 1,
              "archived": false, "created_at": 1, "updated_at": 2, "deleted_at": 3
            }
            """.trimIndent(),
        )
        assertEquals("CASH", account.type)
        assertEquals(125000L, account.openingBalanceMinor)
        assertEquals(3L, account.deletedAt)
    }

    @Test
    fun `transaction fixture parses`() {
        val tx = AramJson.decodeFromString<TransactionDto>(
            """
            {
              "id": "t-1", "account_id": "acc-1", "category_id": null,
              "type": "TRANSFER", "amount_minor": 2500, "currency_code": "USD",
              "occurred_at": 1763500000000, "note": "rent",
              "transfer_group_id": "g-1", "recurring_rule_id": null
            }
            """.trimIndent(),
        )
        assertEquals("TRANSFER", tx.type)
        assertEquals(2500L, tx.amountMinor)
        assertEquals("g-1", tx.transferGroupId)
        assertNull(tx.categoryId)
    }

    @Test
    fun `budget goal and contribution fixtures parse`() {
        val budget = AramJson.decodeFromString<BudgetDto>(
            """{"id":"b1","category_id":"c1","period_start_epoch_day":20690,"limit_minor":50000,"currency_code":"USD"}""",
        )
        assertEquals(20690L, budget.periodStartEpochDay)

        val goal = AramJson.decodeFromString<GoalDto>(
            """{"id":"g1","name":"Trip","target_minor":100000,"currency_code":"USD","deadline_epoch_day":21000,"status":"ACTIVE"}""",
        )
        assertEquals("ACTIVE", goal.status)

        val contribution = AramJson.decodeFromString<GoalContributionDto>(
            """{"id":"gc1","goal_id":"g1","amount_minor":1000,"occurred_at":1763500000000}""",
        )
        assertEquals("g1", contribution.goalId)
    }

    @Test
    fun `category fixture parses`() {
        val category = AramJson.decodeFromString<CategoryDto>(
            """{"id":"c1","name":"Food","kind":"EXPENSE","parent_id":"p1","color_argb":1,"icon_key":"food"}""",
        )
        assertEquals("EXPENSE", category.kind)
        assertEquals("p1", category.parentId)
    }

    @Test
    fun `error envelope exposes fields and the canonical row`() {
        val envelope = AramJson.decodeFromString<ErrorEnvelope>(
            """
            {
              "error": {
                "code": "conflict",
                "message": "stale row",
                "fields": { "amount_minor": "must be positive" },
                "request_id": "req_123",
                "details": { "current": { "id": "t-1", "updated_at": 5 } }
              }
            }
            """.trimIndent(),
        )
        assertEquals("conflict", envelope.error.code)
        assertEquals("must be positive", envelope.error.fields?.get("amount_minor"))
        assertEquals("req_123", envelope.error.requestId)
        assertNotNull(envelope.error.details?.get("current"))
    }

    @Test
    fun `auth response parses tokens`() {
        val auth = AramJson.decodeFromString<DataEnvelope<AuthResponseDto>>(
            """
            {
              "data": {
                "user": { "id": "u1", "email": "demo@aram.app" },
                "tokens": {
                  "access_token": "a", "access_expires_at": 1,
                  "refresh_token": "r", "refresh_expires_at": 2
                }
              }
            }
            """.trimIndent(),
        )
        assertEquals("a", auth.data.tokens.accessToken)
        assertEquals(2L, auth.data.tokens.refreshExpiresAt)
    }

    @Test
    fun `sync envelope parses every collection`() {
        val sync = AramJson.decodeFromString<SyncEnvelope>(
            """
            {
              "data": {
                "changes": {
                  "accounts": [ { "id": "a1", "name": "Cash", "type": "CASH", "currency_code": "USD", "opening_balance_minor": 0 } ],
                  "categories": [],
                  "transactions": [],
                  "budgets": [],
                  "goals": [ { "id": "g1", "name": "Trip", "target_minor": 1, "currency_code": "USD", "status": "ACTIVE" } ],
                  "goal_contributions": []
                }
              },
              "meta": { "next_since": 42, "next_cursor": "c", "has_more": false }
            }
            """.trimIndent(),
        )
        assertEquals(1, sync.data.changes.accounts.size)
        assertEquals(1, sync.data.changes.goals.size)
        assertEquals(42L, sync.meta?.nextSince)
    }

    @Test
    fun `push envelope parses results`() {
        val push = AramJson.decodeFromString<PushEnvelope>(
            """
            {
              "data": {
                "results": [
                  { "idempotency_key": "k1", "status": "applied", "row": { "id": "t1" } },
                  { "idempotency_key": "k2", "status": "conflict", "row": { "id": "t2" } },
                  { "idempotency_key": "k3", "status": "rejected", "error": { "code": "validation_failed" } }
                ]
              },
              "meta": { "server_time": 7 }
            }
            """.trimIndent(),
        )
        assertEquals(3, push.data.results.size)
        assertEquals("conflict", push.data.results[1].status)
        assertTrue(push.data.results[2].error != null)
    }

    @Test
    fun `requests encode snake case and omit nulls`() {
        val json = AramJson.encodeToString(PatchMeRequest(displayName = "Ava"))
        assertTrue(json.contains("\"display_name\":\"Ava\""))
        assertTrue(!json.contains("default_currency"))
    }
}
