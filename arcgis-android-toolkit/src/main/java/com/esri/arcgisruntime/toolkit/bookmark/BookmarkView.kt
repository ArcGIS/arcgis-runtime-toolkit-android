/*
 * Copyright 2019 Esri
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.esri.arcgisruntime.toolkit.bookmark

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.databinding.DataBindingUtil
import androidx.databinding.ViewDataBinding
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.esri.arcgisruntime.mapping.Bookmark
import com.esri.arcgisruntime.mapping.BookmarkList
import com.esri.arcgisruntime.toolkit.BR
import com.esri.arcgisruntime.toolkit.R

/**
 * The BookmarkView will display a list of bookmarks in a [RecyclerView] and allows the user to
 * select a bookmark and perform some action.
 *
 * @since 100.7.0
 */
class BookmarkView : FrameLayout {

    private val bookmarksAdapter by lazy { BookmarkAdapter() }

    var bookmarks: BookmarkList? = null
        set(value) {
            field = value
            bookmarksAdapter.submitList(value)
        }

    var onItemClickListener: OnItemClickListener<Bookmark>? = null

    interface OnItemClickListener<Bookmark> {
        fun onItemClick(item: Bookmark)
    }

    /**
     * Constructor used when instantiating this View directly to attach it to another view programmatically.
     *
     * @since 100.7.0
     */
    constructor(context: Context) : super(context) {
        init(context)
    }

    /**
     * Constructor used when defining this view in an XML layout.
     *
     * @since 100.7.0
     */
    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs) {
        init(context)
    }

    /**
     * Initializes this BookmarkView by inflating the layout and setting the [RecyclerView] adapter.
     *
     * @since 100.7.0
     */
    private fun init(context: Context) {
        inflate(context, R.layout.layout_bookmarkview, this)
        val bookmarkRecyclerView = findViewById<RecyclerView>(R.id.bookmarkRecyclerView)
        bookmarkRecyclerView.layoutManager = LinearLayoutManager(context)
        bookmarkRecyclerView.adapter = bookmarksAdapter
    }

    /**
     * Implements the adapter to be set on the [RecyclerView].
     *
     * @since 100.7.0
     */
    private inner class BookmarkAdapter : ListAdapter<Bookmark, ViewHolder>(DiffCallback()) {

        private val onItemClickListener = object : OnItemClickListener<Bookmark> {
            override fun onItemClick(item: Bookmark) {
                this@BookmarkView.onItemClickListener?.onItemClick(item)
            }
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val inflater = LayoutInflater.from(parent.context)
            val binding = DataBindingUtil.inflate<ViewDataBinding>(
                inflater,
                R.layout.item_bookmark_row,
                parent,
                false
            )
            return ViewHolder(binding)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) =
            holder.bind(getItem(position), onItemClickListener)

    }

    /**
     * Callback for calculating the diff between two non-null items in a list.
     *
     * @since 100.7.0
     */
    private class DiffCallback : DiffUtil.ItemCallback<Bookmark>() {

        override fun areItemsTheSame(oldItem: Bookmark, newItem: Bookmark): Boolean {
            return oldItem == newItem
        }

        override fun areContentsTheSame(oldItem: Bookmark, newItem: Bookmark): Boolean {
            return oldItem.name == newItem.name && oldItem.viewpoint.toJson() == newItem.viewpoint.toJson()
        }
    }

    /**
     * The BookmarkAdapter ViewHolder.
     *
     * @since 100.7.0
     */
    private class ViewHolder(private val binding: ViewDataBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(
            bookmark: Bookmark,
            onItemClickListener: OnItemClickListener<Bookmark>
        ) {
            binding.setVariable(BR.bookmarkItem, bookmark)
            itemView.setOnClickListener { onItemClickListener.onItemClick(bookmark) }
            binding.executePendingBindings()
        }
    }
}
