package fr.epf.mm.gestionclient

import android.util.Log
import android.view.View
import fr.epf.mm.gestionclient.model.Country
import fr.epf.mm.gestionclient.model.Gender

//fun Country.getImage() = if(this.gender == Gender.MAN) R.drawable.man else R.drawable.woman

//val Country.nomComplet : String
    //get() = "${firstName} ${lastName.mettreEnMajuscule()}"

fun String.mettreEnMajuscule() = this.uppercase()

fun View.click( action : (View) -> Unit){
    Log.d("CLICK","click !")
    this.setOnClickListener(action)
}