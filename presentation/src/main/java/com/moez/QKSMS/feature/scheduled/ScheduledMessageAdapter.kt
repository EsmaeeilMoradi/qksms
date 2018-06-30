package com.moez.QKSMS.feature.scheduled

import android.net.Uri
import android.telephony.PhoneNumberUtils
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import com.moez.QKSMS.R
import com.moez.QKSMS.common.base.QkRealmAdapter
import com.moez.QKSMS.common.base.QkViewHolder
import com.moez.QKSMS.common.util.DateFormatter
import com.moez.QKSMS.model.Contact
import com.moez.QKSMS.model.PhoneNumber
import com.moez.QKSMS.model.Recipient
import com.moez.QKSMS.model.ScheduledMessage
import com.moez.QKSMS.repository.ContactRepository
import io.reactivex.subjects.PublishSubject
import io.reactivex.subjects.Subject
import io.realm.RealmList
import kotlinx.android.synthetic.main.scheduled_message_list_item.view.*
import javax.inject.Inject

class ScheduledMessageAdapter @Inject constructor(
        private val contactRepo: ContactRepository,
        private val dateFormatter: DateFormatter
) : QkRealmAdapter<ScheduledMessage>() {

    private val contacts by lazy { contactRepo.getContacts() }
    private val contactMap: HashMap<String, Contact> = hashMapOf()
    private val imagesViewPool = RecyclerView.RecycledViewPool()

    val clicks: Subject<Long> = PublishSubject.create()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): QkViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.scheduled_message_list_item, parent, false)

        view.attachments.adapter = ScheduledMessageAttachmentAdapter()
        view.attachments.setRecycledViewPool(imagesViewPool)

        return QkViewHolder(view)
    }

    override fun onBindViewHolder(holder: QkViewHolder, position: Int) {
        val message = getItem(position)!!
        val view = holder.itemView

        view.setOnClickListener { clicks.onNext(message.id) }

        message.recipients.forEach { address ->
            if (!contactMap.containsKey(address)) {
                contactMap[address] = contacts
                        .firstOrNull { it.numbers.any { number -> PhoneNumberUtils.compare(address, number.address) } }
                        ?: Contact(numbers = RealmList(PhoneNumber(address)))
            }
        }

        // GroupAvatarView only accepts recipients, so map the phone numbers to recipients
        view.avatars.contacts = message.recipients.map { address -> Recipient(address = address) }

        view.recipients.text = message.recipients.joinToString(",") { address ->
            contactMap[address]?.name?.takeIf { it.isNotBlank() } ?: address
        }

        view.date.text = dateFormatter.getScheduledTimestamp(message.date)
        view.body.text = message.body

        val adapter = view.attachments.adapter as ScheduledMessageAttachmentAdapter
        adapter.data = message.attachments.map(Uri::parse)
        view.attachments.isVisible = message.attachments.isNotEmpty()
    }

}