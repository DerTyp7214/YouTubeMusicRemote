package de.dertyp7214.youtubemusicremote.viewmodels

import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModel

class SearchViewModel : ViewModel() {
    private val searchOpen = MutableLiveData(false)
    private val channelId = MutableLiveData<String?>(null)
    private val query = MutableLiveData<String?>(null)

    fun setSearchOpen(open: Boolean) {
        searchOpen.value = open
    }

    fun getSearchOpen(): Boolean? {
        return searchOpen.value
    }

    fun observerSearchOpen(owner: LifecycleOwner, observer: Observer<Boolean>) {
        searchOpen.observe(owner, observer)
    }

    fun setChannelId(channelId: String?) {
        this.channelId.value = channelId
    }

    fun getChannelId(): String? {
        return channelId.value
    }

    fun observeChannelId(owner: LifecycleOwner, observer: Observer<String?>) {
        channelId.observe(owner, observer)
    }

    fun setQuery(query: String?) {
        this.query.value = query
    }

    fun getQuery(): String? {
        return query.value
    }

    fun observeQuery(owner: LifecycleOwner, observer: Observer<String?>) {
        query.observe(owner, observer)
    }
}