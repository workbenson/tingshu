package com.github.eprendre.tingshu.db

import androidx.room.*
import com.github.eprendre.tingshu.utils.Book
import io.reactivex.Completable
import io.reactivex.Flowable
import io.reactivex.Single

@Dao
interface BookDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertBooks(vararg books: Book): Completable

    @Update
    fun updateBooks(vararg books: Book): Completable

    @Query("SELECT * FROM my_books WHERE book_url = :bookUrl LIMIT 1")
    fun findByBookUrl(bookUrl: String): Single<Book>

    @Delete
    fun deleteBooks(vararg books: Book): Single<Int>

    @Query("SELECT * FROM my_books ORDER BY " +
            "CASE WHEN :parameter = 0 THEN id END ASC," +
            "CASE WHEN :parameter = 1 THEN id END DESC")
    fun loadAllBooks(parameter: Int = 1): Flowable<List<Book>>
}