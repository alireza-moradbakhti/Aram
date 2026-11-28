package com.aram.app.session

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aram.core.domain.usecase.RestoreTransaction
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.launch

@HiltViewModel
class TransactionUndoViewModel @Inject constructor(
    private val restoreTransaction: RestoreTransaction,
) : ViewModel() {

    fun restore(id: String) {
        viewModelScope.launch {
            restoreTransaction(id)
        }
    }
}