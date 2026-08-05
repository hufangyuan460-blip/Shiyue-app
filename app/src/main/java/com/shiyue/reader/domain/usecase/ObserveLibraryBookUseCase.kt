package com.shiyue.reader.domain.usecase

import com.shiyue.reader.core.model.LibraryBook
import com.shiyue.reader.domain.repository.BookRepository
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow

class ObserveLibraryBookUseCase @Inject constructor(private val repository: BookRepository) {
    operator fun invoke(id: String): Flow<LibraryBook?> = repository.observeLibraryBook(id)
}
