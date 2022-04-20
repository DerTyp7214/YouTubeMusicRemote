package de.dertyp7214.youtubemusicremote.viewmodels

import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModel

class YouTubeViewModel : ViewModel() {
    private val searchOpen = MutableLiveData(false)
    private val channelId = MutableLiveData<String?>(null)

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
}