package com.shiyue.reader.domain.usecase

import com.shiyue.reader.core.model.Book
import com.shiyue.reader.domain.repository.BookRepository
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow

class ObserveBooksUseCase @Inject constructor(
    private val bookRepository: BookRepository,
) {
    operator fun invoke(): Flow<List<Book>> = bookRepository.observeBooks()
}
