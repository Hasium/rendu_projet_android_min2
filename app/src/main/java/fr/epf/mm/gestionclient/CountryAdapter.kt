package fr.epf.mm.gestionclient

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import fr.epf.mm.gestionclient.model.Country
import com.google.gson.Gson

//public class ClientViewHolder extends RecyclerView.ViewHolder{
//
//    public ClientViewHolder(View view){
//        super(view)
//    }

class ClientViewHolder(view : View) : RecyclerView.ViewHolder(view)


class CountryAdapter(val countries: List<Country>, val context: Context) : RecyclerView.Adapter<ClientViewHolder>(){

    private val likedCountries = mutableListOf<Country>()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ClientViewHolder {
        val layoutInflater = LayoutInflater.from(parent.context)
        val view = layoutInflater.inflate(R.layout.country_view, parent, false)
        return ClientViewHolder(view)
    }

    override fun getItemCount() = countries.size

    override fun onBindViewHolder(holder: ClientViewHolder, position: Int) {
        val country = countries[position]
        val view = holder.itemView
        val countryNameTextView = view.findViewById<TextView>(R.id.country_view_textview)
        countryNameTextView.text = country.name

        val imageView = view.findViewById<ImageView>(R.id.country_view_imageview)

        Glide.with(view.context)
            .load(country.flag)
            .into(imageView)

        val likeButton = view.findViewById<ImageView>(R.id.like_coutry_button)

        val gson = Gson()
        val sharedPreferences: SharedPreferences = context.getSharedPreferences("countries", Context.MODE_PRIVATE)
        val likedCountriesJson = sharedPreferences.getString("liked_countries", "[]")
        likedCountries.clear()
        likedCountries.addAll(gson.fromJson(likedCountriesJson, Array<Country>::class.java).asList())
        var isLiked = likedCountries.any { it.name == country.name }
        likeButton.setImageResource(if (isLiked) R.drawable.favorie_on else R.drawable.favorie_off)

        likeButton.setOnClickListener {
            val editor = sharedPreferences.edit()
            if (isLiked) {
                likedCountries.removeIf { it.name == country.name }
            } else {
                likedCountries.add(country)
            }
            val likedCountriesJson = gson.toJson(likedCountries)
            editor.putString("liked_countries", likedCountriesJson)
            editor.apply()

            isLiked = !isLiked
            likeButton.setImageResource(if (isLiked) R.drawable.favorie_on else R.drawable.favorie_off)
        }

        val cardVIew = view.findViewById<CardView>(R.id.country_view_cardview)
        cardVIew.click {
            with(it.context){
                val intent = Intent(this, DetailsCountryActivity::class.java)
                intent.putExtra(DetailsCountryActivity.COUNTRY_ID_EXTRA, country)
                startActivity(intent)
            }
        }
    }
}


