package org.tokend.contoredemptions.features.pos.view

import android.content.Context
import android.text.TextUtils
import android.util.AttributeSet
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.TextView
import androidx.appcompat.widget.AppCompatSpinner
import org.jetbrains.anko.layoutInflater
import org.tokend.contoredemptions.R
import org.tokend.contoredemptions.features.assets.data.model.Asset
import org.tokend.contoredemptions.view.Picker

class AssetPickerSpinner @JvmOverloads constructor(
        context: Context,
        attrs: AttributeSet? = null,
        defStyleAttr: Int = 0
) : AppCompatSpinner(context, attrs, defStyleAttr), Picker<Asset> {
    private class Adapter(
            context: Context
    ) : ArrayAdapter<Asset>(context, android.R.layout.simple_dropdown_item_1line) {
        private val textLeftPadding: Int by lazy {
            context.resources.getDimensionPixelSize(R.dimen.half_standard_padding)
        }
        private val textRightPadding: Int by lazy {
            context.resources.getDimensionPixelSize(R.dimen.standard_padding)
        }

        override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
            return getItemView(position, convertView, parent)
        }

        override fun getDropDownView(position: Int, convertView: View?, parent: ViewGroup): View {
            return getItemView(position, convertView, parent)
        }

        private fun getItemView(position: Int, convertView: View?, parent: ViewGroup): View {

            val view: TextView = (convertView
                    ?: context.layoutInflater.inflate(android.R.layout.simple_dropdown_item_1line,
                            parent, false)) as TextView

            val item = getItem(position)!!

            view.setPadding(textLeftPadding, view.paddingTop, textRightPadding, view.bottom)
            view.setSingleLine(false)
            view.maxLines = 2
            view.ellipsize = TextUtils.TruncateAt.END
            view.text = item.name ?: item.code

            return view
        }
    }

    override var selectedItemIndex: Int
        get() = selectedItemPosition
        set(value) {
            setSelection(value)
        }
    override var selectedItem: Asset?
        get() = items.getOrNull(selectedItemIndex)
        set(value) {
            selectedItemIndex = items.indexOf(value).takeIf { it >= 0 } ?: 0
        }

    private var items = listOf<Asset>()
    private var itemsAdapter = Adapter(context)
    private var suspendEvent = false
    private var itemSelectionListener: ((Asset) -> Unit)? = null

    override fun setItems(items: List<Asset>, selectedIndex: Int) {
        val selected = items.getOrNull(selectedIndex)
        this.items = items
        initItems(selected)
    }

    override fun onItemSelected(listener: ((Asset) -> Unit)?) {
        this.itemSelectionListener = listener
    }

    init {
        adapter = itemsAdapter
        onItemSelectedListener = object : OnItemSelectedListener {
            override fun onNothingSelected(parent: AdapterView<*>?) {}

            override fun onItemSelected(
                    parent: AdapterView<*>?, view: View?,
                    position: Int, id: Long
            ) {
                if (!suspendEvent) {
                    itemSelectionListener?.invoke(items[position])
                }
            }
        }
    }

    private fun initItems(selected: Asset? = null) {
        val indexToSelect = items.indexOfFirst { it.name == selected?.name }
                .let { index ->
                    if (index < 0) {
                        suspendEvent = false
                        0
                    } else {
                        suspendEvent = true
                        index
                    }
                }

        post {
            itemsAdapter.clear()
            itemsAdapter.addAll(items)

            selectedItemIndex = indexToSelect

            suspendEvent = false
        }
    }
}