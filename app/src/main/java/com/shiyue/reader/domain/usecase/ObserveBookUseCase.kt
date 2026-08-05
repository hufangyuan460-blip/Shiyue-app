package com.shiyue.reader.domain.usecase

import com.shiyue.reader.core.model.Book
import com.shiyue.reader.domain.repository.BookRepository
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow

class ObserveBookUseCase @Inject constructor(
    private val repository: BookRepository,
) {
    operator fun invoke(bookId: String): Flow<Book?> = repository.observeBook(bookId)
}
