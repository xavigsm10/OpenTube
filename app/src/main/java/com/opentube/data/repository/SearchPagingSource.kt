package com.opentube.data.repository

import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.opentube.data.extractor.NewPipeHelper
import com.opentube.data.models.Video

class SearchPagingSource(
    private val newPipeHelper: NewPipeHelper,
    private val query: String
) : PagingSource<String, Video>() {

    override suspend fun load(params: LoadParams<String>): LoadResult<String, Video> {
        return try {
            val pageUrl = params.key
            
            // Call NewPipeHelper directly
            val result = newPipeHelper.searchVideosPaged(query, pageUrl)

            result.fold(
                onSuccess = { pagedResult ->
                    LoadResult.Page(
                        data = pagedResult.items,
                        prevKey = null, 
                        nextKey = pagedResult.nextPageUrl
                    )
                },
                onFailure = { exception ->
                    LoadResult.Error(exception)
                }
            )
        } catch (exception: Exception) {
            LoadResult.Error(exception)
        }
    }

    override fun getRefreshKey(state: PagingState<String, Video>): String? {
        return null // Always start from the beginning on refresh
    }
}
