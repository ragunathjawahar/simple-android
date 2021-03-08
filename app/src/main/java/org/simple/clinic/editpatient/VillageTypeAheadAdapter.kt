package org.simple.clinic.editpatient

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Filter
import android.widget.TextView
import org.simple.clinic.R

class VillageTypeAheadAdapter(
    context: Context,
    colonyOrVillagesList: List<String>,
) : ArrayAdapter<String>(context, 0, 0, colonyOrVillagesList) {

  override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
    var view = convertView
    if (view == null) {
      view = LayoutInflater.from(context).inflate(
          R.layout.village_typeahead_list_item, parent, false
      )
    }

    val villageTypeAheadListItemTextView = view!!.findViewById<TextView>(R.id.villageTypeAheadItemTextView)

    val colonyOrVillageListItem = getItem(position)

    if (colonyOrVillageListItem != null) {
      villageTypeAheadListItemTextView.text = colonyOrVillageListItem
    }

    return view
  }


  override fun getFilter(): Filter {
    return villageTypeAheadFilter
  }

  private var villageTypeAheadFilter: Filter = object : Filter() {
    override fun performFiltering(constraint: CharSequence?): FilterResults {
      val filterResults = FilterResults()
      val colonyOrVillagesListSuggestions: MutableList<String> = mutableListOf()

      if (constraint.isNullOrEmpty()) {
        colonyOrVillagesListSuggestions.addAll(colonyOrVillagesList)
      } else {
        val searchQuery = constraint.toString().trim()
        colonyOrVillagesListSuggestions.addAll(colonyOrVillagesList.filter { it.contains(searchQuery) })
      }

      filterResults.values = colonyOrVillagesListSuggestions
      filterResults.count = colonyOrVillagesListSuggestions.size

      return filterResults
    }

    override fun publishResults(constraint: CharSequence?, results: FilterResults?) {
      clear()
      if (results != null)
        addAll(results.values as List<String>)
      notifyDataSetChanged()
    }
  }
}
