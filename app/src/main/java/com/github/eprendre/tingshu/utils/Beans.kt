package com.github.eprendre.tingshu.utils

import android.os.Parcel
import android.os.Parcelable
import androidx.annotation.DrawableRes
import androidx.annotation.IdRes
import androidx.annotation.Keep
import androidx.recyclerview.widget.DiffUtil
import androidx.room.*

@Keep
data class Episode(val title: String, val url: String) {
    companion object {
        val diffCallback = object : DiffUtil.ItemCallback<Episode>() {
            override fun areItemsTheSame(oldItem: Episode, newItem: Episode): Boolean {
                return oldItem.url == newItem.url
            }

            override fun areContentsTheSame(oldItem: Episode, newItem: Episode): Boolean {
                return oldItem.url == newItem.url && oldItem.title == newItem.title
            }

        }
    }
}

@Keep
@Entity(tableName = "my_books", indices = [Index(value = ["book_url"], unique = true)])
data class Book(
    @ColumnInfo(name = "cover_url")
    var coverUrl: String,
    @ColumnInfo(name = "book_url")
    val bookUrl: String,
    val title: String,
    var author: String,
    var artist: String
) {
    @PrimaryKey(autoGenerate = true)
    var id: Int? = null
    var intro: String = ""
    var currentEpisodeUrl: String? = null
    var currentEpisodeName: String? = null
    var currentEpisodePosition: Long = 0
    var skipBeginning: Long = 0
    var skipEnd: Long = 0
    var isFree: Boolean = true
    @Ignore var status: String = ""

    companion object {
        val diffCallback = object : DiffUtil.ItemCallback<Book>() {
            override fun areItemsTheSame(oldItem: Book, newItem: Book): Boolean {
                return oldItem == newItem
            }

            override fun areContentsTheSame(oldItem: Book, newItem: Book): Boolean {
                return oldItem.bookUrl == newItem.bookUrl &&
                        oldItem.currentEpisodeUrl == newItem.currentEpisodeUrl &&
                        oldItem.currentEpisodePosition == newItem.currentEpisodePosition
            }

        }
    }
}

@Keep
data class CategoryTab(
    val title: String,
    val url: String
) : Parcelable {
    constructor(source: Parcel) : this(
        source.readString(),
        source.readString()
    )

    override fun describeContents() = 0

    override fun writeToParcel(dest: Parcel, flags: Int) = with(dest) {
        writeString(title)
        writeString(url)
    }

    companion object {
        @JvmField
        val CREATOR: Parcelable.Creator<CategoryTab> = object : Parcelable.Creator<CategoryTab> {
            override fun createFromParcel(source: Parcel): CategoryTab = CategoryTab(source)
            override fun newArray(size: Int): Array<CategoryTab?> = arrayOfNulls(size)
        }
    }
}

@Keep
data class CategoryMenu(
    val title: String,
    @DrawableRes val icon: Int,
    @IdRes val id: Int,
    val tabs: List<CategoryTab>
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) {
            return true
        }
        if (other !is CategoryMenu) {
            return false
        }

        return tabs == other.tabs
    }

    override fun hashCode(): Int {
        var result = title.hashCode()
        result = 31 * result + icon
        result = 31 * result + id
        result = 31 * result + tabs.hashCode()
        return result
    }
}

@Keep
data class Category(
    val list: List<Book>,
    val currentPage: Int,
    val totalPage: Int,
    val currentUrl: String,
    val nextUrl: String
)